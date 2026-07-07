package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HealthRepository
import com.example.data.model.DoctorVisit
import com.example.data.model.MedicalDocument
import com.example.data.model.Medication
import com.example.data.model.PatientProfile
import com.example.data.model.MedicationLog
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.EncryptionMethod
import net.lingala.zip4j.model.enums.AesKeyStrength
import android.content.Context
import android.net.Uri
import android.graphics.Paint
import android.graphics.Typeface
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.gemini.*
import com.example.utils.toBase64
import android.graphics.Bitmap
import com.example.BuildConfig

import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.worker.MedicationNotificationWorker
import com.example.receiver.MedicationNotificationScheduler
import com.example.receiver.AppointmentNotificationScheduler
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

sealed interface ClinicalOverviewState {
    object Idle : ClinicalOverviewState
    object Loading : ClinicalOverviewState
    data class Success(
        val overviewText: String,
        val keyObservations: List<String>,
        val riskIndicators: List<ClinicalRiskIndicator>,
        val recommendations: List<String>
    ) : ClinicalOverviewState
    data class Error(val message: String) : ClinicalOverviewState
}

data class ClinicalRiskIndicator(
    val title: String,
    val severity: String,
    val description: String
)

class HealthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HealthRepository

    private val _clinicalOverviewState = kotlinx.coroutines.flow.MutableStateFlow<ClinicalOverviewState>(ClinicalOverviewState.Idle)
    val clinicalOverviewState: StateFlow<ClinicalOverviewState> = _clinicalOverviewState

    private val _ocrResult = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val ocrResult: StateFlow<String?> = _ocrResult

    private val _extractedVitals = mutableListOf<com.example.data.model.VitalSign>()
    val extractedVitals: List<com.example.data.model.VitalSign> get() = _extractedVitals

    private val _extractedDate = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val extractedDate: StateFlow<String?> = _extractedDate

    private val _extractedProvider = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val extractedProvider: StateFlow<String?> = _extractedProvider

    private fun enhanceImageContrast(bitmap: Bitmap): Bitmap {
        val cm = android.graphics.ColorMatrix()
        val contrast = 1.3f
        val brightness = 20f
        cm.set(floatArrayOf(
            contrast, 0f, 0f, 0f, brightness,
            0f, contrast, 0f, 0f, brightness,
            0f, 0f, contrast, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))
        val ret = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(ret)
        val paint = android.graphics.Paint()
        paint.colorFilter = android.graphics.ColorMatrixColorFilter(cm)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return ret
    }

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            var extractedRawText = ""
            val enhancedBitmap = enhanceImageContrast(bitmap)
            
            try {
                // First use ML Kit OCR to extract text
                val image = InputImage.fromBitmap(enhancedBitmap, 0)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val mlKitResult = recognizer.process(image).await()
                extractedRawText = mlKitResult.text

                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val promptText = if (extractedRawText.isNotBlank()) {
                    "Please analyze this medical document. We have extracted some raw text, but please also rely on the image visually to correct any OCR mistakes, read handwriting, and understand the layout.\n\n" +
                    "--- EXTRACTED RAW TEXT ---\n" +
                    "$extractedRawText\n\n" +
                    "-------------------------------\n\n"
                } else {
                    "Please analyze this medical document very carefully directly from the image. Extract key information such as the patient's name, document type (e.g. Lab Report, Prescription), provider/doctor name/hospital, and any important findings or medications listed. Provide a detailed, readable summary.\n\n"
                }
                
                val finalPrompt = promptText +
                        "Additionally, meticulously search the record for any of the following numerical vital parameters if present:\n" +
                        "- Blood Sugar (Glucose, Fasting or Random, in mg/dL or mmol/L - translate/convert to mg/dL if needed, typically 70-300)\n" +
                        "- Blood Pressure (systolic/diastolic, e.g. 120/80 mmHg)\n" +
                        "- Cholesterol level (Total cholesterol in mg/dL, typically 100-350 mg/dL)\n\n" +
                        "To enable automatic updates to the dashboard, append a strict JSON block at the very end of your response inside a ```json ... ``` codeblock with the exact schema format:\n" +
                        "{\n" +
                        "  \"patientName\": \"NAME\",\n" +
                        "  \"providerName\": \"PROVIDER\",\n" +
                        "  \"documentDate\": \"YYYY-MM-DD\",\n" +
                        "  \"vitals\": [\n" +
                        "     {\"type\": \"Blood Pressure\", \"value1\": 120, \"value2\": 80},\n" +
                        "     {\"type\": \"Blood Sugar\", \"value1\": 105, \"value2\": 0},\n" +
                        "     {\"type\": \"Cholesterol\", \"value1\": 190, \"value2\": 0}\n" +
                        "  ]\n" +
                        "}\n" +
                        "Ensure you output the JSON inside a markdown backticks block. If none of these vitals are found, leave the \"vitals\" array empty."

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(text = finalPrompt),
                                Part(inlineData = InlineData(mimeType = "image/jpeg", data = enhancedBitmap.toBase64()))
                            )
                        )
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    var cleanText = responseText
                    var jsonBlock: String? = null
                    
                    val jsonStartToken = "```json"
                    val jsonEndToken = "```"
                    val startIndex = responseText.indexOf(jsonStartToken)
                    if (startIndex != -1) {
                        val contentStartIndex = startIndex + jsonStartToken.length
                        val endIndex = responseText.indexOf(jsonEndToken, contentStartIndex)
                        if (endIndex != -1) {
                            jsonBlock = responseText.substring(contentStartIndex, endIndex).trim()
                            cleanText = responseText.substring(0, startIndex).trim()
                        }
                    } else {
                        val firstBrace = responseText.indexOf('{')
                        val lastBrace = responseText.lastIndexOf('}')
                        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                            jsonBlock = responseText.substring(firstBrace, lastBrace + 1).trim()
                            cleanText = responseText.substring(0, firstBrace).trim()
                        }
                    }
                    
                    var finalResult = cleanText
                    if (extractedRawText.isNotBlank()) {
                         finalResult += "\n\n--- Raw OCR Text (For Searching) ---\n" +
                             extractedRawText.take(1500) + if (extractedRawText.length > 1500) " [Truncated]" else ""
                    }
                    
                    val tempVitals = mutableListOf<com.example.data.model.VitalSign>()
                    var detectedDate: String? = null
                    var detectedProvider: String? = null
                    
                    if (!jsonBlock.isNullOrEmpty()) {
                        try {
                            val json = JSONObject(jsonBlock)
                            detectedDate = json.optString("documentDate", null)
                            detectedProvider = json.optString("providerName", null)
                            if (detectedDate == "null" || detectedDate == "YYYY-MM-DD") {
                                detectedDate = null
                            }
                            if (detectedProvider == "null" || detectedProvider == "PROVIDER") {
                                detectedProvider = null
                            }
                            
                            val vitalsArray = json.optJSONArray("vitals")
                            if (vitalsArray != null) {
                                for (i in 0 until vitalsArray.length()) {
                                    val vObj = vitalsArray.getJSONObject(i)
                                    val type = vObj.optString("type")
                                    val val1 = vObj.optInt("value1", 0)
                                    val val2 = vObj.optInt("value2", 0)
                                    if (type.isNotEmpty() && val1 > 0) {
                                        tempVitals.add(
                                            com.example.data.model.VitalSign(
                                                type = type,
                                                date = detectedDate ?: java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
                                                value1 = val1,
                                                value2 = val2
                                            )
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        _ocrResult.value = finalResult
                        _extractedVitals.clear()
                        _extractedVitals.addAll(tempVitals)
                        _extractedDate.value = detectedDate
                        _extractedProvider.value = detectedProvider
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val fallbackText = if (extractedRawText.isNotBlank()) {
                            "Gemini Analysis Failed. Raw Text Extracted:\n\n$extractedRawText"
                        } else {
                            "Could not extract information."
                        }
                        _ocrResult.value = fallbackText
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val fallbackStr = if (extractedRawText.isNotBlank()) "\n--- Raw Text Extracted ---\n$extractedRawText" else ""
                    _ocrResult.value = if (e.message?.contains("429") == true) {
                        "AI Rate Limit Exceeded (HTTP 429). Please try again later. Alternatively, manually enter details.$fallbackStr"
                    } else {
                        "Error during analysis: ${e.message}$fallbackStr"
                    }
                }
            }
        }
    }
    
    fun clearOcrResult() {
        _ocrResult.value = null
        _extractedVitals.clear()
        _extractedDate.value = null
        _extractedProvider.value = null
    }

    private val prefs = application.getSharedPreferences("health_wallet_prefs", Context.MODE_PRIVATE)

    val appPin: String?
        get() = prefs.getString("app_pin", null)?.takeIf { it.isNotEmpty() }
        
    fun setAppPin(pin: String) {
        prefs.edit().putString("app_pin", pin).apply()
    }

    val familyPin: String?
        get() = prefs.getString("family_pin", null)?.takeIf { it.isNotEmpty() }
        
    fun setFamilyPin(pin: String) {
        prefs.edit().putString("family_pin", pin).apply()
    }

    val isEmergencyBypassEnabled: Boolean
        get() = prefs.getBoolean("emergency_bypass_enabled", false)

    fun setEmergencyBypassEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("emergency_bypass_enabled", enabled).apply()
    }

    private val _currentLanguage = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("language", "ALL") ?: "ALL")
    val currentLanguage: StateFlow<String> = _currentLanguage

    private val _lastSyncTime = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("last_sync_time", null))
    val lastSyncTime: StateFlow<String?> = _lastSyncTime

    private val _lastBackupTime = kotlinx.coroutines.flow.MutableStateFlow(prefs.getString("last_backup_time", null))
    val lastBackupTime: StateFlow<String?> = _lastBackupTime

    // Family Profiles & Offline Tracker States
    private val _familyMembers = kotlinx.coroutines.flow.MutableStateFlow(listOf(
        FamilyMemberProfile("1", "Janaka (Me)", "Self", "42", "A+"),
        FamilyMemberProfile("2", "Anusha", "Wife", "38", "O+"),
        FamilyMemberProfile("3", "Malith", "Son", "12", "B+")
    ))
    val familyMembers: StateFlow<List<FamilyMemberProfile>> = _familyMembers

    private val _activeMemberId = kotlinx.coroutines.flow.MutableStateFlow("1")
    val activeMemberId: StateFlow<String> = _activeMemberId

    // Local Vaccine Logs
    private val _vaccineLogs = kotlinx.coroutines.flow.MutableStateFlow(listOf(
        VaccineLog("v1", "BCG Tuberculosis", "2015-02-10", "Completed", "Malith"),
        VaccineLog("v2", "MMR (Measles, Mumps, Rubella)", "2024-05-15", "Completed", "Malith"),
        VaccineLog("v3", "Influenza Vaccine Daily Dose", "2026-11-20", "Scheduled", "Janaka (Me)")
    ))
    val vaccineLogs: StateFlow<List<VaccineLog>> = _vaccineLogs.combine(activeMemberId) { logs, activeId ->
        val member = _familyMembers.value.find { it.id == activeId }
        val shortName = member?.name?.split(" ")?.firstOrNull() ?: ""
        logs.filter { log ->
            log.memberName.contains(shortName, ignoreCase = true) || shortName.contains(log.memberName, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Subscription status
    private val _isPremiumUser = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser

    fun selectActiveMember(id: String) {
        _activeMemberId.value = id
    }

    fun addFamilyMember(name: String, relation: String, age: String, bloodGroup: String): Boolean {
        if (!_isPremiumUser.value && _familyMembers.value.size >= 3) {
            return false // Trigger Subscription Dialog
        }
        val newId = System.currentTimeMillis().toString()
        val newMember = FamilyMemberProfile(newId, name, relation, age, bloodGroup)
        _familyMembers.value = _familyMembers.value + newMember
        return true
    }

    fun addExpense(description: String, amount: Double, category: String, profileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val newExpense = com.example.data.model.HealthExpense(
                description = description,
                amount = amount,
                date = dateStr,
                category = category,
                profileName = profileName,
                profileId = activeMemberId.value
            )
            repository.insertExpense(newExpense)
        }
    }

    fun addExpenseWithDate(description: String, amount: Double, category: String, profileName: String, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val newExpense = com.example.data.model.HealthExpense(
                description = description,
                amount = amount,
                date = date,
                category = category,
                profileName = profileName,
                profileId = activeMemberId.value
            )
            repository.insertExpense(newExpense)
        }
    }

    fun deleteExpense(expense: com.example.data.model.HealthExpense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteExpense(expense)
        }
    }

    fun addSymptomLog(name: String, severity: Int, status: String = "Active", notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            val newLog = com.example.data.model.SymptomLog(
                name = name,
                severity = severity,
                date = dateStr,
                status = status,
                notes = notes,
                profileId = activeMemberId.value
            )
            repository.insertSymptomLog(newLog)
        }
    }

    fun addSymptomLogWithDate(name: String, severity: Int, date: String, status: String = "Active", notes: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val newLog = com.example.data.model.SymptomLog(
                name = name,
                severity = severity,
                date = date,
                status = status,
                notes = notes,
                profileId = activeMemberId.value
            )
            repository.insertSymptomLog(newLog)
        }
    }

    fun deleteSymptomLog(symptomLog: com.example.data.model.SymptomLog) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteSymptomLog(symptomLog)
        }
    }

    fun addVaccineLog(name: String, date: String, status: String, memberName: String) {
        val newId = System.currentTimeMillis().toString()
        val newLog = VaccineLog(newId, name, date, status, memberName)
        _vaccineLogs.value = _vaccineLogs.value + newLog
    }

    fun togglePremiumStatus() {
        _isPremiumUser.value = !_isPremiumUser.value
    }

    fun updateLastBackupTime() {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val dateStr = sdf.format(java.util.Date())
        prefs.edit().putString("last_backup_time", dateStr).apply()
        _lastBackupTime.value = dateStr
    }

    fun performSimulatedSync(onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(1800)
            val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val dateStr = format.format(java.util.Date())
            prefs.edit().putString("last_sync_time", dateStr).apply()
            withContext(Dispatchers.Main) {
                _lastSyncTime.value = dateStr
                onComplete()
            }
        }
    }

    fun setLanguage(language: String) {
        prefs.edit().putString("language", language).apply()
        _currentLanguage.value = language
    }

    var auth: com.google.firebase.auth.FirebaseAuth? = null

    init {
        val healthWalletDao = AppDatabase.getDatabase(application).healthWalletDao()
        repository = HealthRepository(healthWalletDao)
        try {
            com.google.firebase.FirebaseApp.initializeApp(application)
            auth = com.google.firebase.auth.FirebaseAuth.getInstance()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        scheduleMedicationReminders(application)

        // Load cached overview if available
        val cachedOverview = prefs.getString("cached_clinical_overview", null)
        if (!cachedOverview.isNullOrEmpty()) {
            val parsed = parseClinicalOverview(cachedOverview)
            if (parsed != null) {
                _clinicalOverviewState.value = parsed
            }
        }
        // Generate clinical overview background task
        viewModelScope.launch(Dispatchers.IO) {
            kotlinx.coroutines.delay(2000)
            generateClinicalOverview()
        }

        // Pre-seed family member profiles if empty
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val profileList = repository.allProfiles.firstOrNull() ?: emptyList()
                if (profileList.isEmpty()) {
                    repository.saveProfile(PatientProfile(
                        id = "1",
                        name = "Janaka (Me)",
                        bloodGroup = "A+",
                        age = "42",
                        gender = "Male",
                        allergies = "Penicillin",
                        chronicConditions = "Hypertension, Type 2 Diabetes"
                    ))
                    repository.saveProfile(PatientProfile(
                        id = "2",
                        name = "Anusha",
                        bloodGroup = "O+",
                        age = "38",
                        gender = "Female",
                        allergies = "Sulfonamides",
                        chronicConditions = "Asthma"
                    ))
                    repository.saveProfile(PatientProfile(
                        id = "3",
                        name = "Malith",
                        bloodGroup = "B+",
                        age = "12",
                        gender = "Male",
                        allergies = "None",
                        chronicConditions = "None"
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            val count = repository.getVitalSignsByType("Blood Pressure").firstOrNull()?.size ?: 0
            if (count == 0) {
                val mockVitals = mutableListOf<com.example.data.model.VitalSign>()
                val cal = java.util.Calendar.getInstance()
                cal.add(java.util.Calendar.DAY_OF_YEAR, -360)
                val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                for (i in 0..360 step 15) {
                    val dateStr = format.format(cal.time)
                    mockVitals.add(
                        com.example.data.model.VitalSign(
                            type = "Blood Pressure",
                            date = dateStr,
                            value1 = 115 + (Math.random() * 20).toInt(),
                            value2 = 72 + (Math.random() * 12).toInt()
                        )
                    )
                    mockVitals.add(
                        com.example.data.model.VitalSign(
                            type = "Blood Sugar",
                            date = dateStr,
                            value1 = 85 + (Math.random() * 45).toInt(),
                            value2 = 0
                        )
                    )
                    mockVitals.add(
                        com.example.data.model.VitalSign(
                            type = "Cholesterol",
                            date = dateStr,
                            value1 = 160 + (Math.random() * 60).toInt(),
                            value2 = 0
                        )
                    )
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 15)
                }
                repository.insertVitalSigns(mockVitals)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val expenseCount = repository.allExpenses.firstOrNull()?.size ?: 0
                if (expenseCount == 0) {
                    val seedExpenses = listOf(
                        com.example.data.model.HealthExpense(
                            description = "Consultation - Dr. Fernando (Cardiology)",
                            amount = 2500.0,
                            date = "2026-06-12",
                            category = "Doctor",
                            profileName = "Janaka (Me)"
                        ),
                        com.example.data.model.HealthExpense(
                            description = "Amoxicillin & Paracetamol refills",
                            amount = 1200.0,
                            date = "2026-06-14",
                            category = "Medicine",
                            profileName = "Malith"
                        ),
                        com.example.data.model.HealthExpense(
                            description = "Blood Lipids - Asiri Laboratory",
                            amount = 3800.0,
                            date = "2026-06-15",
                            category = "Doctor",
                            profileName = "Anusha"
                        ),
                        com.example.data.model.HealthExpense(
                            description = "Ayurvedic Herbal Balms & Kashaya",
                            amount = 1500.0,
                            date = "2026-06-16",
                            category = "Medicine",
                            profileName = "Janaka (Me)"
                        ),
                        com.example.data.model.HealthExpense(
                            description = "Dr. Amara Perera Pediatric Clinic",
                            amount = 3000.0,
                            date = "2026-05-10",
                            category = "Doctor",
                            profileName = "Malith"
                        ),
                        com.example.data.model.HealthExpense(
                            description = "Chronic Metformin Refills (Osu Sala)",
                            amount = 1350.0,
                            date = "2026-05-18",
                            category = "Medicine",
                            profileName = "Janaka (Me)"
                        )
                    )
                    seedExpenses.forEach { repository.insertExpense(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val symptomCount = repository.allSymptomLogs.firstOrNull()?.size ?: 0
                if (symptomCount == 0) {
                    val seedSymptoms = listOf(
                        com.example.data.model.SymptomLog(
                            name = "Fever & Chills",
                            severity = 6,
                            date = "2026-06-17",
                            status = "Active",
                            notes = "Sudden fever spike after evening walk, took paracetamol."
                        ),
                        com.example.data.model.SymptomLog(
                            name = "Migraine Headache",
                            severity = 8,
                            date = "2026-06-16",
                            status = "Active",
                            notes = "Throbbing pain behind the right eye accompanied by severe photophobia."
                        ),
                        com.example.data.model.SymptomLog(
                            name = "Indigestion & Bloating",
                            severity = 4,
                            date = "2026-06-18",
                            status = "Resolved",
                            notes = "Felt bloating after heavy dinner, fully resolved this morning."
                        )
                    )
                    seedSymptoms.forEach { repository.insertSymptomLog(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun scheduleMedicationReminders(context: Context) {
        try {
            val workRequest = PeriodicWorkRequestBuilder<MedicationNotificationWorker>(1, TimeUnit.DAYS)
                .build()
                
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "MedicationReminderWork",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun testMedicationNotification() {
        try {
            val workRequest = androidx.work.OneTimeWorkRequestBuilder<MedicationNotificationWorker>().build()
            WorkManager.getInstance(getApplication()).enqueue(workRequest)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncToFirestore(userId: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val userDoc = db.collection("users").document(userId)
                
                val currentProfile = profile.value
                if (currentProfile != null) {
                    val pMap = mapOf(
                        "name" to currentProfile.name,
                        "bloodGroup" to currentProfile.bloodGroup,
                        "allergies" to currentProfile.allergies,
                        "chronicConditions" to currentProfile.chronicConditions
                    )
                    userDoc.set(mapOf("profile" to pMap), com.google.firebase.firestore.SetOptions.merge())
                }
                
                withContext(Dispatchers.Main) {
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    val profile: StateFlow<PatientProfile?> = repository.allProfiles.combine(activeMemberId) { profiles, activeId ->
        profiles.find { it.id == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val documents: StateFlow<List<MedicalDocument>> = repository.documents.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val medications: StateFlow<List<Medication>> = repository.medications.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val medicationLogs: StateFlow<List<MedicationLog>> = repository.medicationLogs.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val visits: StateFlow<List<DoctorVisit>> = repository.visits.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val dailyVitals: StateFlow<List<com.example.data.model.DailyVitals>> = repository.dailyVitals.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val expenses: StateFlow<List<com.example.data.model.HealthExpense>> = repository.allExpenses.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val symptomLogs: StateFlow<List<com.example.data.model.SymptomLog>> = repository.allSymptomLogs.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val medicalRecords: StateFlow<List<com.example.data.model.MedicalRecord>> = repository.medicalRecords.combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun insertDailyVitals(vitals: com.example.data.model.DailyVitals) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDailyVitals(vitals.copy(profileId = activeMemberId.value))
            generateClinicalOverview()
        }
    }
    val bpVitalSigns: StateFlow<List<com.example.data.model.VitalSign>> = repository.getVitalSignsByType("Blood Pressure").combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    val sugarVitalSigns: StateFlow<List<com.example.data.model.VitalSign>> = repository.getVitalSignsByType("Blood Sugar").combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cholesterolVitalSigns: StateFlow<List<com.example.data.model.VitalSign>> = repository.getVitalSignsByType("Cholesterol").combine(activeMemberId) { list, activeId ->
        list.filter { it.profileId == activeId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveProfile(profile: PatientProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveProfile(profile)
        }
    }

    fun updateProfilePhoto(uriStr: String?) {
        val current = profile.value
        if (current != null) {
            saveProfile(current.copy(photoUri = uriStr))
        } else {
            saveProfile(
                PatientProfile(
                    id = activeMemberId.value,
                    photoUri = uriStr
                )
            )
        }
    }

    fun insertDocument(document: MedicalDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDocument(document.copy(profileId = activeMemberId.value))
        }
    }

    fun insertDocumentWithVitals(document: MedicalDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertDocument(document.copy(profileId = activeMemberId.value))
            if (_extractedVitals.isNotEmpty()) {
                val updatedVitals = _extractedVitals.map { vital ->
                    vital.copy(date = document.date, profileId = activeMemberId.value)
                }
                repository.insertVitalSigns(updatedVitals)
                _extractedVitals.clear()
            }
            generateClinicalOverview()
        }
    }

    fun deleteDocument(document: MedicalDocument) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocument(document)
            generateClinicalOverview()
        }
    }

    fun insertMedicalRecord(record: com.example.data.model.MedicalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertMedicalRecord(record.copy(profileId = activeMemberId.value))
        }
    }

    fun deleteMedicalRecord(record: com.example.data.model.MedicalRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMedicalRecord(record)
        }
    }

    fun generateClinicalOverview() {
        viewModelScope.launch(Dispatchers.IO) {
            _clinicalOverviewState.value = ClinicalOverviewState.Loading
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                
                val currentProfile = profile.value
                val loadedDocs = documents.value
                val loadedBP = bpVitalSigns.value
                val loadedSugar = sugarVitalSigns.value
                val loadedCholesterol = cholesterolVitalSigns.value

                // Filter for Lab Reports with analyses
                val labReportsSummary = loadedDocs
                    .filter { it.type.equals("Lab Report", ignoreCase = true) && !it.analysis.isNullOrEmpty() }
                    .take(3)
                    .joinToString("\n---\n") { doc ->
                        "Lab Report Date: ${doc.date}\nProvider: ${doc.providerName}\nAnalysis summary: ${doc.analysis}"
                    }

                val bpSummary = loadedBP.takeLast(5).joinToString(", ") { "${it.value1}/${it.value2} (${it.date})" }
                val sugarSummary = loadedSugar.takeLast(5).joinToString(", ") { "${it.value1} mg/dL (${it.date})" }
                val cholesterolSummary = loadedCholesterol.takeLast(5).joinToString(", ") { "${it.value1} mg/dL (${it.date})" }

                val targetLang = when (_currentLanguage.value) {
                    "SI" -> "Sinhala"
                    "TA" -> "Tamil"
                    else -> "English"
                }

                val prompt = "You are an expert clinical consultant and medical AI. Analyze the patient's record to provide a comprehensive, clinical overview of the patient's current status for viewing by consulting doctors.\n\n" +
                        "The report and all texts MUST be written in the following target language: $targetLang.\n\n" +
                        "PATIENT PROFILE:\n" +
                        "- Name: ${currentProfile?.name ?: "Unknown"}\n" +
                        "- Age: ${currentProfile?.age ?: "Unknown"}\n" +
                        "- Gender: ${currentProfile?.gender ?: "Unknown"}\n" +
                        "- Chronic Conditions: ${currentProfile?.chronicConditions ?: "None documented"}\n" +
                        "- Allergies: ${currentProfile?.allergies ?: "None"}\n\n" +
                        "LATEST PATIENT CLINICAL VITALS AND TRENDS:\n" +
                        "- Blood Pressure history (latest 5): $bpSummary\n" +
                        "- Blood Sugar history (latest 5): $sugarSummary\n" +
                        "- Cholesterol history (latest 5): $cholesterolSummary\n\n" +
                        "LATEST LAB REPORTS AND MEDICAL DISCOVERIES:\n" +
                        "${if (labReportsSummary.isEmpty()) "No lab reports uploaded yet." else labReportsSummary}\n\n" +
                        "Based on this clinical data, please synthesize an overview of the patient's current status. You must output a JSON object containing the following keys (ensure it's valid JSON, do NOT use trailing commas, do NOT include markdown block characters in the raw JSON text string itself besides the surrounding braces):\n" +
                        "{\n" +
                        "  \"overviewText\": \"A professional 3-4 sentence clinical summary of the patient's current status.\",\n" +
                        "  \"keyObservations\": [\n" +
                        "     \"Observation 1 (e..g, blood pressure shows stable systolic range.)\",\n" +
                        "     \"Observation 2\"\n" +
                        "  ],\n" +
                        "  \"riskIndicators\": [\n" +
                        "     {\n" +
                        "       \"title\": \"Risk title (e.g., 'Slight Hyperglycemia', 'Elevated Systolic BP')\",\n" +
                        "       \"severity\": \"Low\" or \"Medium\" or \"High\",\n" +
                        "       \"description\": \"Details about the risk and what to monitor.\"\n" +
                        "     }\n" +
                        "  ],\n" +
                        "  \"recommendations\": [\n" +
                        "     \"Clinical recommendation 1\",\n" +
                        "     \"Clinical recommendation 2\"\n" +
                        "  ]\n" +
                        "}\n\n" +
                        "Ensure the entire response is ONLY the raw JSON object itself in the language: $targetLang. Do not wrap the JSON object with any markdown block in your response, just output the pure JSON beginning with { and ending with } directly."

                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(parts = listOf(Part(text = prompt)))
                    ),
                    generationConfig = GenerationConfig(
                        temperature = 0.2f
                    )
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    var jsonStr = responseText.trim()
                    if (jsonStr.startsWith("```json")) {
                        jsonStr = jsonStr.substring(7)
                    }
                    if (jsonStr.endsWith("```")) {
                        jsonStr = jsonStr.substring(0, jsonStr.length - 3)
                    }
                    jsonStr = jsonStr.trim()

                    val parsed = parseClinicalOverview(jsonStr)
                    if (parsed != null) {
                        prefs.edit().putString("cached_clinical_overview", jsonStr).apply()
                        withContext(Dispatchers.Main) {
                            _clinicalOverviewState.value = parsed
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            _clinicalOverviewState.value = ClinicalOverviewState.Error("Parsing Error: Invalid JSON structure")
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _clinicalOverviewState.value = ClinicalOverviewState.Error("Empty response from clinical AI engine")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    val errorMsg = if (e.message?.contains("429") == true) {
                        "AI Rate Limit Exceeded (HTTP 429). Please try again later."
                    } else {
                        "AI Generation Failed: ${e.message}"
                    }
                    val cachedStr = prefs.getString("cached_clinical_overview", null)
                    if (!cachedStr.isNullOrEmpty()) {
                        val parsed = parseClinicalOverview(cachedStr)
                        if (parsed != null) {
                            _clinicalOverviewState.value = parsed
                            return@withContext
                        }
                    }
                    _clinicalOverviewState.value = ClinicalOverviewState.Error(errorMsg)
                }
            }
        }
    }

    private fun parseClinicalOverview(jsonStr: String): ClinicalOverviewState.Success? {
        return try {
            val obj = JSONObject(jsonStr)
            val overviewText = obj.optString("overviewText", "No overview text available.")
            
            val keyObs = mutableListOf<String>()
            val obsArr = obj.optJSONArray("keyObservations")
            if (obsArr != null) {
                for (i in 0 until obsArr.length()) {
                    keyObs.add(obsArr.getString(i))
                }
            }
            
            val risks = mutableListOf<ClinicalRiskIndicator>()
            val risksArr = obj.optJSONArray("riskIndicators")
            if (risksArr != null) {
                for (i in 0 until risksArr.length()) {
                    val rObj = risksArr.getJSONObject(i)
                    risks.add(
                        ClinicalRiskIndicator(
                            title = rObj.optString("title", ""),
                            severity = rObj.optString("severity", "Low"),
                            description = rObj.optString("description", "")
                        )
                    )
                }
            }
            
            val recs = mutableListOf<String>()
            val recsArr = obj.optJSONArray("recommendations")
            if (recsArr != null) {
                for (i in 0 until recsArr.length()) {
                    recs.add(recsArr.getString(i))
                }
            }
            
            ClinicalOverviewState.Success(
                overviewText = overviewText,
                keyObservations = keyObs,
                riskIndicators = risks,
                recommendations = recs
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun insertVitalSign(vital: com.example.data.model.VitalSign) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertVitalSigns(listOf(vital.copy(profileId = activeMemberId.value)))
        }
    }

    fun insertMedication(medication: Medication) {
        viewModelScope.launch {
            val medWithProfileId = medication.copy(profileId = activeMemberId.value)
            val newId = repository.insertMedication(medWithProfileId)
            val savedMed = medWithProfileId.copy(id = newId.toInt())
            MedicationNotificationScheduler.scheduleNotification(getApplication(), savedMed)
        }
    }

    fun updateMedication(medication: Medication) {
        viewModelScope.launch {
            repository.updateMedication(medication)
            MedicationNotificationScheduler.scheduleNotification(getApplication(), medication)
        }
    }

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
            MedicationNotificationScheduler.cancelNotification(getApplication(), medication.id)
        }
    }

    fun insertMedicationLog(log: MedicationLog) {
        viewModelScope.launch {
            repository.insertMedicationLog(log.copy(profileId = activeMemberId.value))
        }
    }

    fun deleteMedicationLog(log: MedicationLog) {
        viewModelScope.launch {
            repository.deleteMedicationLog(log)
        }
    }

    fun insertVisit(visit: DoctorVisit) {
        viewModelScope.launch {
            val visitWithProfile = visit.copy(profileId = activeMemberId.value)
            val id = repository.insertVisit(visitWithProfile)
            val finalVisit = if (visitWithProfile.id == 0) {
                visitWithProfile.copy(id = id.toInt())
            } else {
                visitWithProfile
            }
            AppointmentNotificationScheduler.scheduleAppointmentReminders(getApplication(), finalVisit)
        }
    }

    fun deleteVisit(visit: DoctorVisit) {
        viewModelScope.launch {
            repository.deleteVisit(visit)
            AppointmentNotificationScheduler.cancelAppointmentReminders(getApplication(), visit.id)
        }
    }

    fun processQRCode(data: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = JSONObject(data)
                val type = json.optString("type")
                val itemData = json.optJSONObject("data")
                if (itemData != null) {
                    when (type) {
                        "medication" -> {
                            val med = Medication(
                                name = itemData.optString("name", "Unknown"),
                                dose = itemData.optString("dose", ""),
                                frequency = itemData.optString("frequency", ""),
                                doctorName = itemData.optString("doctorName", ""),
                                status = itemData.optString("status", "Active"),
                                profileId = activeMemberId.value
                            )
                            repository.insertMedication(med)
                        }
                        "visit" -> {
                            val visit = DoctorVisit(
                                doctorName = itemData.optString("doctorName", "Unknown"),
                                speciality = itemData.optString("speciality", ""),
                                hospital = itemData.optString("hospital", ""),
                                date = itemData.optString("date", ""),
                                reason = itemData.optString("reason", ""),
                                notes = itemData.optString("notes", ""),
                                profileId = activeMemberId.value
                            )
                            repository.insertVisit(visit)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createBackup(context: Context, uri: Uri, password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataObj = JSONObject()
                
                val p = profile.value
                if (p != null) {
                    val pObj = JSONObject()
                    pObj.put("id", p.id)
                    pObj.put("name", p.name)
                    pObj.put("bloodGroup", p.bloodGroup)
                    pObj.put("age", p.age)
                    pObj.put("gender", p.gender)
                    pObj.put("allergies", p.allergies)
                    pObj.put("chronicConditions", p.chronicConditions)
                    dataObj.put("profile", pObj)
                }

                val docArray = JSONArray()
                documents.value.forEach { doc ->
                    val obj = JSONObject()
                    obj.put("id", doc.id)
                    obj.put("type", doc.type)
                    obj.put("date", doc.date)
                    obj.put("providerName", doc.providerName)
                    obj.put("uri", doc.uri)
                    docArray.put(obj)
                }
                dataObj.put("documents", docArray)

                val medArray = JSONArray()
                medications.value.forEach { med ->
                    val obj = JSONObject()
                    obj.put("id", med.id)
                    obj.put("name", med.name)
                    obj.put("dose", med.dose)
                    obj.put("frequency", med.frequency)
                    obj.put("doctorName", med.doctorName)
                    obj.put("status", med.status)
                    obj.put("startDate", med.startDate)
                    obj.put("endDate", med.endDate)
                    medArray.put(obj)
                }
                dataObj.put("medications", medArray)

                val visitArray = JSONArray()
                visits.value.forEach { v ->
                    val obj = JSONObject()
                    obj.put("id", v.id)
                    obj.put("doctorName", v.doctorName)
                    obj.put("speciality", v.speciality)
                    obj.put("hospital", v.hospital)
                    obj.put("date", v.date)
                    obj.put("reason", v.reason)
                    obj.put("notes", v.notes)
                    visitArray.put(obj)
                }
                dataObj.put("visits", visitArray)

                val jsonString = dataObj.toString(2)
                
                val tempDir = context.cacheDir
                val tempJsonFile = File(tempDir, "backup_data.json")
                tempJsonFile.writeText(jsonString)

                val tempZipFile = File(tempDir, "temp_backup.zip")
                if (tempZipFile.exists()) tempZipFile.delete()

                val zipParameters = ZipParameters()
                if (password.isNotEmpty()) {
                    zipParameters.isEncryptFiles = true
                    zipParameters.encryptionMethod = EncryptionMethod.AES
                    zipParameters.aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }

                val zipFile = if (password.isNotEmpty()) {
                    ZipFile(tempZipFile, password.toCharArray())
                } else {
                    ZipFile(tempZipFile)
                }
                
                zipFile.addFile(tempJsonFile, zipParameters)

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    tempZipFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }

                tempJsonFile.delete()
                tempZipFile.delete()
                
                withContext(Dispatchers.Main) {
                    updateLastBackupTime()
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun exportAsJson(context: Context, uri: Uri, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dataObj = JSONObject()
                
                val docArray = JSONArray()
                documents.value.forEach { doc ->
                    val obj = JSONObject()
                    obj.put("id", doc.id)
                    obj.put("type", doc.type)
                    obj.put("date", doc.date)
                    obj.put("providerName", doc.providerName)
                    obj.put("uri", doc.uri)
                    docArray.put(obj)
                }
                dataObj.put("documents", docArray)

                val medArray = JSONArray()
                medications.value.forEach { med ->
                    val obj = JSONObject()
                    obj.put("id", med.id)
                    obj.put("name", med.name)
                    obj.put("dose", med.dose)
                    obj.put("frequency", med.frequency)
                    obj.put("doctorName", med.doctorName)
                    obj.put("status", med.status)
                    obj.put("startDate", med.startDate)
                    obj.put("endDate", med.endDate)
                    medArray.put(obj)
                }
                dataObj.put("medications", medArray)

                val jsonString = dataObj.toString(2)
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(jsonString.toByteArray())
                }
                
                withContext(Dispatchers.Main) {
                    updateLastBackupTime()
                    onResult(true)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false)
                }
            }
        }
    }

    fun generatePdfReport(
        context: Context,
        includeProfile: Boolean,
        includeDocuments: Boolean,
        includeMedications: Boolean,
        includeVitals: Boolean,
        onResult: (Boolean, File?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                
                var pageNumber = 1
                var pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                var currentPage = pdfDocument.startPage(pageInfo)
                var canvas = currentPage.canvas
                
                val titlePaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 18f
                    color = android.graphics.Color.parseColor("#005E54")
                }
                
                val subtitlePaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 12f
                    color = android.graphics.Color.DKGRAY
                }
                
                val headerPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 14f
                    color = android.graphics.Color.parseColor("#005E54")
                }
                
                val textPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    textSize = 10f
                    color = android.graphics.Color.BLACK
                }
                
                val textBoldPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 10f
                    color = android.graphics.Color.BLACK
                }
                
                val footerPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    textSize = 8f
                    color = android.graphics.Color.GRAY
                }
                
                val linePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#B2DFDB")
                    strokeWidth = 1f
                }
                
                val borderPaint = Paint().apply {
                    color = android.graphics.Color.LTGRAY
                    style = Paint.Style.STROKE
                    strokeWidth = 0.5f
                }
                
                val bgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#F5F5F5")
                }

                val redHeaderPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 10f
                    color = android.graphics.Color.parseColor("#B71C1C")
                }
                
                val redTableLinePaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFCDD2")
                    strokeWidth = 0.5f
                    style = Paint.Style.STROKE
                }
                
                val redRowBgPaint = Paint().apply {
                    color = android.graphics.Color.parseColor("#FFEBEE")
                }
                
                val redTextPaint = Paint().apply {
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textSize = 10f
                    color = android.graphics.Color.parseColor("#B71C1C")
                }
                
                var y = 40f
                val margin = 40f
                
                fun checkNewPage(requiredHeight: Float) {
                    if (y + requiredHeight > pageHeight - margin) {
                        canvas.drawText("Page $pageNumber", margin, pageHeight - 20f, footerPaint)
                        canvas.drawText("Arogya Secure Digital Health Wallet - Exported on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", pageWidth - 320f, pageHeight - 20f, footerPaint)
                        
                        pdfDocument.finishPage(currentPage)
                        pageNumber++
                        pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                        currentPage = pdfDocument.startPage(pageInfo)
                        canvas = currentPage.canvas
                        
                        y = margin
                        canvas.drawText("AROGYA MEDICAL SNAPSHOT REPORT (CONT.)", margin, y, subtitlePaint)
                        y += 15f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                        y += 25f
                    }
                }
                
                canvas.drawText("AROGYA DIGITAL HEALTH WALLET", margin, y, titlePaint)
                y += 18f
                canvas.drawText("Comprehensive Medical Snapshot & Portfolio", margin, y, subtitlePaint)
                y += 10f
                canvas.drawText("Generated at: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}", margin, y, footerPaint)
                y += 12f
                canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                y += 30f
                
                if (includeProfile) {
                    val p = profile.value
                    if (p != null) {
                        checkNewPage(150f)
                        canvas.drawText("I. PATIENT CLINICAL DATA", margin, y, headerPaint)
                        y += 12f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                        y += 20f
                        
                        canvas.drawRect(margin, y - 10f, pageWidth - margin, y + 100f, bgPaint)
                        canvas.drawRect(margin, y - 10f, pageWidth - margin, y + 100f, borderPaint)
                        
                        val col1X = margin + 15f
                        val col2X = pageWidth / 2f + 10f
                        
                        canvas.drawText("Full Name: ", col1X, y, textBoldPaint)
                        canvas.drawText(p.name.ifEmpty { "N/A" }, col1X + 80f, y, textPaint)
                        
                        canvas.drawText("Blood Group: ", col2X, y, textBoldPaint)
                        canvas.drawText(p.bloodGroup.ifEmpty { "N/A" }, col2X + 80f, y, textPaint)
                        
                        y += 20f
                        canvas.drawText("Age / Gender: ", col1X, y, textBoldPaint)
                        canvas.drawText("${p.age.ifEmpty { "N/A" }} / ${p.gender.ifEmpty { "N/A" }}", col1X + 80f, y, textPaint)
                        
                        canvas.drawText("Allergies: ", col2X, y, textBoldPaint)
                        canvas.drawText(p.allergies.ifEmpty { "None Reported" }, col2X + 80f, y, textPaint)
                        
                        y += 20f
                        canvas.drawText("Chronic Illness: ", col1X, y, textBoldPaint)
                        canvas.drawText(p.chronicConditions.ifEmpty { "None Reported" }, col1X + 85f, y, textPaint)
                        
                        canvas.drawText("Language: ", col2X, y, textBoldPaint)
                        canvas.drawText(p.language, col2X + 80f, y, textPaint)
                        
                        y += 60f
                    }
                }
                
                if (includeMedications) {
                    val medsList = medications.value
                    if (medsList.isNotEmpty()) {
                        checkNewPage(80f)
                        canvas.drawText("II. ACTIVE PRESCRIBED MEDICATIONS", margin, y, headerPaint)
                        y += 12f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                        y += 20f
                        
                        medsList.forEach { med ->
                            checkNewPage(45f)
                            canvas.drawText("• ${med.name} (${med.status})", margin + 10f, y, textBoldPaint)
                            canvas.drawText("Dosage: ${med.dose} | Frequency: ${med.frequency} | Reminder: ${med.reminderTime}", margin + 25f, y + 14f, textPaint)
                            canvas.drawText("Prescribed By: Dr. ${med.doctorName.ifEmpty { "N/A" }}", margin + 25f, y + 26f, textPaint)
                            y += 40f
                        }
                        y += 10f
                    }
                }
                
                if (includeVitals) {
                    val bp = bpVitalSigns.value
                    val sugar = sugarVitalSigns.value
                    val chol = cholesterolVitalSigns.value
                    val dv = dailyVitals.value
                    
                    if (bp.isNotEmpty() || sugar.isNotEmpty() || chol.isNotEmpty() || dv.isNotEmpty()) {
                        checkNewPage(100f)
                        canvas.drawText("III. HEALTH VITALS & CLINICAL METRICS (RED LIGHT HIGHLIGHT TABLES)", margin, y, redHeaderPaint)
                        y += 12f
                        canvas.drawLine(margin, y, pageWidth - margin, y, redTableLinePaint)
                        y += 20f
                        
                        if (bp.isNotEmpty()) {
                            checkNewPage(45f)
                            canvas.drawText("Blood Pressure Logs:", margin + 10f, y, redHeaderPaint)
                            y += 10f
                            
                            val startX = margin + 10f
                            val tableWidth = 350f
                            val rowHeight = 18f
                            
                            checkNewPage(rowHeight + 10f)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redRowBgPaint)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                            canvas.drawText("Date", startX + 10f, y + 12f, redHeaderPaint)
                            canvas.drawText("Reading (mmHg)", startX + 150f, y + 12f, redHeaderPaint)
                            y += rowHeight
                            
                            bp.take(10).forEach { item ->
                                checkNewPage(rowHeight + 5f)
                                canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                                canvas.drawText(item.date, startX + 10f, y + 12f, textPaint)
                                canvas.drawText("${item.value1}/${item.value2} mmHg", startX + 150f, y + 12f, redTextPaint)
                                y += rowHeight
                            }
                            y += 15f
                        }
                        
                        if (sugar.isNotEmpty()) {
                            checkNewPage(45f)
                            canvas.drawText("Blood Sugar Tracking:", margin + 10f, y, redHeaderPaint)
                            y += 10f
                            
                            val startX = margin + 10f
                            val tableWidth = 350f
                            val rowHeight = 18f
                            
                            checkNewPage(rowHeight + 10f)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redRowBgPaint)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                            canvas.drawText("Date", startX + 10f, y + 12f, redHeaderPaint)
                            canvas.drawText("Sugar Level (mg/dL)", startX + 150f, y + 12f, redHeaderPaint)
                            y += rowHeight
                            
                            sugar.take(10).forEach { item ->
                                checkNewPage(rowHeight + 5f)
                                canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                                canvas.drawText(item.date, startX + 10f, y + 12f, textPaint)
                                canvas.drawText("${item.value1} mg/dL", startX + 150f, y + 12f, redTextPaint)
                                y += rowHeight
                            }
                            y += 15f
                        }
                        
                        if (chol.isNotEmpty()) {
                            checkNewPage(45f)
                            canvas.drawText("Cholesterol Records:", margin + 10f, y, redHeaderPaint)
                            y += 10f
                            
                            val startX = margin + 10f
                            val tableWidth = 350f
                            val rowHeight = 18f
                            
                            checkNewPage(rowHeight + 10f)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redRowBgPaint)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                            canvas.drawText("Date", startX + 10f, y + 12f, redHeaderPaint)
                            canvas.drawText("Total Cholesterol (mg/dL)", startX + 150f, y + 12f, redHeaderPaint)
                            y += rowHeight
                            
                            chol.take(10).forEach { item ->
                                checkNewPage(rowHeight + 5f)
                                canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                                canvas.drawText(item.date, startX + 10f, y + 12f, textPaint)
                                canvas.drawText("${item.value1} mg/dL", startX + 150f, y + 12f, redTextPaint)
                                y += rowHeight
                            }
                            y += 15f
                        }
                        
                        if (dv.isNotEmpty()) {
                            checkNewPage(45f)
                            canvas.drawText("Home Daily Vitals Log Table:", margin + 10f, y, redHeaderPaint)
                            y += 10f
                            
                            val startX = margin + 10f
                            val tableWidth = 350f
                            val rowHeight = 18f
                            
                            checkNewPage(rowHeight + 10f)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redRowBgPaint)
                            canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                            canvas.drawText("Date", startX + 10f, y + 12f, redHeaderPaint)
                            canvas.drawText("Blood Pressure", startX + 120f, y + 12f, redHeaderPaint)
                            canvas.drawText("Heart Rate", startX + 240f, y + 12f, redHeaderPaint)
                            y += rowHeight
                            
                            dv.take(10).forEach { item ->
                                checkNewPage(rowHeight + 5f)
                                canvas.drawRect(startX, y, startX + tableWidth, y + rowHeight, redTableLinePaint)
                                canvas.drawText(item.date, startX + 10f, y + 12f, textPaint)
                                canvas.drawText("${item.systolic}/${item.diastolic} mmHg", startX + 120f, y + 12f, redTextPaint)
                                canvas.drawText("${item.heartRate} bpm", startX + 240f, y + 12f, redTextPaint)
                                y += rowHeight
                            }
                            y += 15f
                        }
                        y += 10f
                    }
                }
                
                if (includeDocuments) {
                    val visitList = visits.value
                    val docList = documents.value
                    
                    if (visitList.isNotEmpty() || docList.isNotEmpty()) {
                        checkNewPage(80f)
                        canvas.drawText("IV. CLINICAL ENCOUNTERS & MEDICAL REPORT SUMMARIES", margin, y, headerPaint)
                        y += 12f
                        canvas.drawLine(margin, y, pageWidth - margin, y, linePaint)
                        y += 20f
                        
                        if (visitList.isNotEmpty()) {
                            checkNewPage(30f)
                            canvas.drawText("Recent Clinical Consultations:", margin + 10f, y, textBoldPaint)
                            y += 15f
                            visitList.forEach { v ->
                                checkNewPage(65f)
                                canvas.drawText("• ${v.date} - Dr. ${v.doctorName} (${v.speciality} at ${v.hospital})", margin + 15f, y, textBoldPaint)
                                canvas.drawText("Reason: ${v.reason}", margin + 25f, y + 14f, textPaint)
                                canvas.drawText("Notes & Guidance: ${v.notes}", margin + 25f, y + 26f, textPaint)
                                y += 42f
                            }
                            y += 15f
                        }
                        
                        if (docList.isNotEmpty()) {
                            checkNewPage(30f)
                            canvas.drawText("Document Summaries & Reports Hub:", margin + 10f, y, textBoldPaint)
                            y += 15f
                            docList.forEach { doc ->
                                checkNewPage(35f)
                                canvas.drawText("• ${doc.date} - ${doc.type} by ${doc.providerName}", margin + 15f, y, textBoldPaint)
                                y += 18f
                            }
                        }
                    }
                }
                
                canvas.drawText("Page $pageNumber", margin, pageHeight - 20f, footerPaint)
                canvas.drawText("Arogya Secure Digital Health Wallet - Exported on ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", pageWidth - 320f, pageHeight - 20f, footerPaint)
                
                pdfDocument.finishPage(currentPage)
                
                val dir = File(context.cacheDir, "shared_pdfs")
                if (!dir.exists()) dir.mkdir()
                val file = File(dir, "Arogya_Clinical_Snapshot_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.pdf")
                val os = FileOutputStream(file)
                pdfDocument.writeTo(os)
                pdfDocument.close()
                os.close()
                
                withContext(Dispatchers.Main) {
                    onResult(true, file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }
    fun getSavedPharmaciesByTown(town: String, onResult: (List<com.example.data.model.SavedPharmacy>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pharmacies = repository.getSavedPharmaciesByTown(town)
            withContext(Dispatchers.Main) {
                onResult(pharmacies)
            }
        }
    }

    fun savePharmacies(pharmacies: List<com.example.data.model.SavedPharmacy>) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertSavedPharmacies(pharmacies)
        }
    }

    fun exportEncryptedBackup(context: Context, password: String, onResult: (Boolean, File?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = com.example.utils.BackupUtils.exportBackup(context, password)
                withContext(Dispatchers.Main) {
                    onResult(true, file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, null)
                }
            }
        }
    }

    fun importEncryptedBackup(context: Context, backupBytes: ByteArray, password: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                com.example.utils.BackupUtils.importBackup(context, backupBytes, password)
                // Also update local flows if needed
                withContext(Dispatchers.Main) {
                    onResult(true, null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(false, e.localizedMessage ?: "Decryption or import failed.")
                }
            }
        }
    }

    // --- Gemini Chatbot States ---
    private val _chatMessages = kotlinx.coroutines.flow.MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: kotlinx.coroutines.flow.StateFlow<List<ChatMessage>> = _chatMessages

    private val _isChatLoading = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isChatLoading: kotlinx.coroutines.flow.StateFlow<Boolean> = _isChatLoading

    private val _selectedAiModel = kotlinx.coroutines.flow.MutableStateFlow("gemini-3.5-flash")
    val selectedAiModel: kotlinx.coroutines.flow.StateFlow<String> = _selectedAiModel

    private val _selectedAiRole = kotlinx.coroutines.flow.MutableStateFlow("general")
    val selectedAiRole: kotlinx.coroutines.flow.StateFlow<String> = _selectedAiRole

    fun updateSelectedAiModel(model: String) {
        _selectedAiModel.value = model
    }

    fun updateSelectedAiRole(role: String) {
        _selectedAiRole.value = role
    }

    fun clearChatHistory() {
        _chatMessages.value = emptyList()
    }

    fun getSystemInstructionForRole(role: String): String {
        val currentProfile = profile.value
        val currentMeds = medications.value.filter { it.status == "Active" }
        val vitalsSummary = dailyVitals.value.take(5).joinToString("\n") { 
            "Date: ${it.date}, Systolic: ${it.systolic}, Diastolic: ${it.diastolic}, Heart Rate: ${it.heartRate}" 
        }
        
        val patientContext = if (currentProfile != null) {
            "Patient Profile: Name is ${currentProfile.name}, Age is ${currentProfile.age}, Blood Group is ${currentProfile.bloodGroup}.\n" +
            "Known Allergies/Intolerances: ${if (currentProfile.allergies.isNotBlank()) currentProfile.allergies else "None reported"}.\n"
        } else {
            "No active patient profile is set yet.\n"
        }
        
        val medsContext = if (currentMeds.isNotEmpty()) {
            "Active Medications stored in Arogya Wallet:\n" +
            currentMeds.joinToString("\n") { "- ${it.name} (${it.dose}, taken ${it.frequency})" }
        } else {
            "No active medications verified in the wallet yet.\n"
        }
        
        val vitalsContext = if (vitalsSummary.isNotBlank()) {
            "Recent logged vital signs:\n$vitalsSummary\n"
        } else {
            "No vital sign logs recorded yet.\n"
        }

        return when (role) {
            "analyst" -> {
                "You are the 'Arogya Medical Wallet Analyst', a secure, professional, clinical AI dashboard companion connected directly to the user's personal health repository.\n\n" +
                "CURRENT PATIENT PORTAL CONTEXT:\n" +
                "=== START CONTEXT ===\n" +
                "$patientContext\n" +
                "$medsContext\n" +
                "$vitalsContext" +
                "=== END CONTEXT ===\n\n" +
                "Your role is to help the user understand, parse, summarize, and cross-reference their stored clinical documents, medications, dosages, and vital trends.\n" +
                "If they ask for drug interactions, look up potential interactions with their active medications. Warn them clearly if they mention any allergen listed in their profile.\n" +
                "IMPORTANT: Always state a professional clinical warning: 'This analysis is based on your logged wallet data. Please confirm critical diagnostics with your medical doctor, practitioner, or cardiologist.'\n" +
                "Keep answers structured, highly readable, clear, and clinical."
            }
            "emergency" -> {
                "You are the 'Emergency Immediate Guide', an authoritative, fast-responding clinical assistant trained in first-aid procedures, triage principles, and critical trauma support.\n\n" +
                "Your main objective is to provide high-contrast, structured, immediately actionable steps to help caregivers handle urgent scenarios (e.g., asthma attacks, anaphylactic shocks, cuts, high fever, poisoning) before an ambulance arrives.\n" +
                "Use clear bold lists, numbered recipes, and emphasize critical Red flags (when to go to the hospital immediately). Keep details extremely brief, objective, and clutter-free so it is scannable in seconds."
            }
            "lankan" -> {
                "You are the 'Sri Lankan Home Care Specialist', a warm, culturally empathetic family health guide from Sri Lanka.\n\n" +
                "You have deep knowledge of traditional Sri Lankan home dietary remedies (e.g., coriander tea for cold/flu, 'Kaha' (turmeric) infusions, sago pudding for body heat, karapincha juice, belimal, ranawara), standard local foods, and the layout of Sri Lanka's healthcare system (OPD, National Hospital, MOH clinics).\n" +
                "Provide compassionate, sound advice that combines comfortable traditional wisdom with modern patient-safety guidelines, in a polite, supportive Sri Lankan tone."
            }
            else -> { // "general"
                "You are the 'Arogya General Health Assistant', a friendly, certified wellness coach and general health guide.\n\n" +
                "You provide general definitions, explanations of medical symptoms, daily wellness coaching, nutritional tips, and healthy habit challenges to help users feel motivated and take control of their active body and lifestyle.\n" +
                "Be empathetic, supportive, energetic, and highly readable."
            }
        }
    }

    fun sendChatMessage(text: String, onError: (String) -> Unit) {
        val userQuery = text.trim()
        if (userQuery.isEmpty()) return

        val userMsg = ChatMessage(sender = MessageSender.USER, text = userQuery)
        val currentMsgs = _chatMessages.value.toMutableList()
        currentMsgs.add(userMsg)
        _chatMessages.value = currentMsgs

        _isChatLoading.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val currentRole = _selectedAiRole.value
                val currentModel = _selectedAiModel.value
                val sysInstructionText = getSystemInstructionForRole(currentRole)

                // Map history to Content list for multi-turn conversational support
                val mappedContents = _chatMessages.value.map { message ->
                    Content(
                        role = if (message.sender == MessageSender.USER) "user" else "model",
                        parts = listOf(Part(text = message.text))
                    )
                }

                val request = GenerateContentRequest(
                    contents = mappedContents,
                    systemInstruction = Content(
                        parts = listOf(Part(text = sysInstructionText))
                    ),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                // Use the newly added dynamic content service method
                val response = RetrofitClient.service.generateDynamicContent(currentModel, apiKey, request)
                val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from model."

                withContext(Dispatchers.Main) {
                    val finalMsgs = _chatMessages.value.toMutableList()
                    finalMsgs.add(ChatMessage(sender = MessageSender.ASSISTANT, text = responseText))
                    _chatMessages.value = finalMsgs
                    _isChatLoading.value = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _isChatLoading.value = false
                    onError(e.localizedMessage ?: "Unknown error calling Gemini API.")
                }
            }
        }
    }
}

enum class MessageSender {
    USER, ASSISTANT, SYSTEM
}

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class FamilyMemberProfile(
    val id: String,
    val name: String,
    val relation: String,
    val age: String,
    val bloodGroup: String
)

data class HealthExpense(
    val id: String,
    val description: String,
    val amount: Double,
    val date: String,
    val category: String,
    val profileName: String
)

data class SymptomLog(
    val id: String,
    val name: String,
    val severity: Int,
    val date: String,
    val status: String
)

data class VaccineLog(
    val id: String,
    val name: String,
    val date: String,
    val status: String,
    val memberName: String
)

