package com.example.utils

import android.content.Context
import android.util.Base64
import com.example.data.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object BackupUtils {

    // AES encryption functions
    fun encrypt(data: String, password: String): ByteArray {
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        val iv = ByteArray(16)
        SecureRandom().nextBytes(iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        return result
    }

    fun decrypt(encryptedWithIv: ByteArray, password: String): String {
        if (encryptedWithIv.size < 16) throw IllegalArgumentException("Invalid encrypted backup file.")
        val iv = ByteArray(16)
        System.arraycopy(encryptedWithIv, 0, iv, 0, 16)
        val encrypted = ByteArray(encryptedWithIv.size - 16)
        System.arraycopy(encryptedWithIv, 16, encrypted, 0, encrypted.size)
        
        val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
        val decryptedBytes = cipher.doFinal(encrypted)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    // Export all DB tables to an encrypted file
    suspend fun exportBackup(context: Context, password: String): File {
        val database = AppDatabase.getDatabase(context)
        val dao = database.healthWalletDao()

        val rootObject = JSONObject()

        // 1. Patient Profiles
        val profiles = dao.getAllProfiles().first()
        val profilesArray = JSONArray()
        profiles.forEach { profile ->
            val pObj = JSONObject().apply {
                put("id", profile.id)
                put("name", profile.name)
                put("bloodGroup", profile.bloodGroup)
                put("age", profile.age)
                put("gender", profile.gender)
                put("allergies", profile.allergies)
                put("chronicConditions", profile.chronicConditions)
                put("language", profile.language)
                put("location", profile.location)
                put("pin", profile.pin)
                put("photoUri", profile.photoUri ?: "")
            }
            profilesArray.put(pObj)
        }
        rootObject.put("profiles", profilesArray)

        // 2. Medical Documents
        val documents = dao.getAllDocuments().first()
        val docsArray = JSONArray()
        documents.forEach { doc ->
            val dObj = JSONObject().apply {
                put("id", doc.id)
                put("type", doc.type)
                put("date", doc.date)
                put("providerName", doc.providerName)
                put("uri", doc.uri)
                put("imageBlob", doc.imageBlob?.let { Base64.encodeToString(it, Base64.DEFAULT) } ?: "")
                put("analysis", doc.analysis ?: "")
                put("profileId", doc.profileId)
            }
            docsArray.put(dObj)
        }
        rootObject.put("documents", docsArray)

        // 3. Medications
        val medications = dao.getAllMedications().first()
        val medsArray = JSONArray()
        medications.forEach { med ->
            val mObj = JSONObject().apply {
                put("id", med.id)
                put("name", med.name)
                put("dose", med.dose)
                put("frequency", med.frequency)
                put("doctorName", med.doctorName)
                put("status", med.status)
                put("startDate", med.startDate)
                put("endDate", med.endDate)
                put("reminderTime", med.reminderTime)
                put("profileId", med.profileId)
                put("frequencyType", med.frequencyType)
                put("frequencyDaysOfWeek", med.frequencyDaysOfWeek)
                put("notificationEnabled", med.notificationEnabled)
            }
            medsArray.put(mObj)
        }
        rootObject.put("medications", medsArray)

        // 4. Medication Logs
        val logs = dao.getAllMedicationLogs().first()
        val logsArray = JSONArray()
        logs.forEach { log ->
            val lObj = JSONObject().apply {
                put("id", log.id)
                put("medicationId", log.medicationId)
                put("medicationName", log.medicationName)
                put("logDate", log.logDate)
                put("logTime", log.logTime)
                put("status", log.status)
                put("notes", log.notes)
                put("profileId", log.profileId)
            }
            logsArray.put(lObj)
        }
        rootObject.put("medication_logs", logsArray)

        // 5. Doctor Visits
        val visits = dao.getAllVisits().first()
        val visitsArray = JSONArray()
        visits.forEach { visit ->
            val vObj = JSONObject().apply {
                put("id", visit.id)
                put("doctorName", visit.doctorName)
                put("speciality", visit.speciality)
                put("hospital", visit.hospital)
                put("date", visit.date)
                put("reason", visit.reason)
                put("notes", visit.notes)
                put("linkedDocumentIds", visit.linkedDocumentIds)
                put("profileId", visit.profileId)
            }
            visitsArray.put(vObj)
        }
        rootObject.put("visits", visitsArray)

        // 6. Vital Signs
        val bpSigns = dao.getVitalSignsByType("Blood Pressure").first()
        val sugarSigns = dao.getVitalSignsByType("Blood Sugar").first()
        val cholSigns = dao.getVitalSignsByType("Cholesterol").first()
        val allVitals = bpSigns + sugarSigns + cholSigns
        val vitalsArray = JSONArray()
        allVitals.forEach { vs ->
            val vsObj = JSONObject().apply {
                put("id", vs.id)
                put("date", vs.date)
                put("type", vs.type)
                put("value1", vs.value1)
                put("value2", vs.value2)
                put("profileId", vs.profileId)
            }
            vitalsArray.put(vsObj)
        }
        rootObject.put("vital_signs", vitalsArray)

        // 7. Daily Vitals
        val dailyVits = dao.getAllDailyVitals().first()
        val dailyVitsArray = JSONArray()
        dailyVits.forEach { dv ->
            val dvObj = JSONObject().apply {
                put("id", dv.id)
                put("date", dv.date)
                put("systolic", dv.systolic)
                put("diastolic", dv.diastolic)
                put("heartRate", dv.heartRate)
                put("profileId", dv.profileId)
            }
            dailyVitsArray.put(dvObj)
        }
        rootObject.put("daily_vitals", dailyVitsArray)

        // 8. Health Expenses
        val expenses = dao.getAllExpenses().first()
        val expArray = JSONArray()
        expenses.forEach { exp ->
            val expObj = JSONObject().apply {
                put("id", exp.id)
                put("description", exp.description)
                put("amount", exp.amount)
                put("date", exp.date)
                put("category", exp.category)
                put("profileName", exp.profileName)
                put("profileId", exp.profileId)
            }
            expArray.put(expObj)
        }
        rootObject.put("health_expenses", expArray)

        // 9. Symptom Logs
        val symptoms = dao.getAllSymptomLogs().first()
        val symArray = JSONArray()
        symptoms.forEach { sym ->
            val symObj = JSONObject().apply {
                put("id", sym.id)
                put("name", sym.name)
                put("severity", sym.severity)
                put("date", sym.date)
                put("status", sym.status)
                put("notes", sym.notes)
                put("profileId", sym.profileId)
            }
            symArray.put(symObj)
        }
        rootObject.put("symptom_logs", symArray)

        // 10. Saved Pharmacies
        val pharmacies = dao.getAllSavedPharmacies().first()
        val pharmArray = JSONArray()
        pharmacies.forEach { ph ->
            val phObj = JSONObject().apply {
                put("id", ph.id)
                put("name", ph.name)
                put("type", ph.type)
                put("lat", ph.lat ?: 0.0)
                put("lon", ph.lon ?: 0.0)
                put("tagsJson", ph.tagsJson ?: "")
                put("town", ph.town)
                put("timestamp", ph.timestamp)
            }
            pharmArray.put(phObj)
        }
        rootObject.put("saved_pharmacies", pharmArray)

        val jsonString = rootObject.toString()
        val encryptedBytes = encrypt(jsonString, password)

        val cachePath = File(context.cacheDir, "backups")
        cachePath.mkdirs()
        val backupFile = File(cachePath, "arogya_wallet_backup.enc")
        val stream = java.io.FileOutputStream(backupFile)
        stream.write(encryptedBytes)
        stream.close()

        return backupFile
    }

    // Import encrypted file back to database
    suspend fun importBackup(context: Context, backupBytes: ByteArray, password: String) {
        val jsonString = decrypt(backupBytes, password)
        val rootObject = JSONObject(jsonString)

        val database = AppDatabase.getDatabase(context)
        val dao = database.healthWalletDao()

        // Core requirement: Clear existing db data to avoid conflicts and ensure exact restored state
        database.clearAllTables()

        // 1. Patient Profiles
        if (rootObject.has("profiles")) {
            val arr = rootObject.getJSONArray("profiles")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val profile = PatientProfile(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    bloodGroup = obj.getString("bloodGroup"),
                    age = obj.getString("age"),
                    gender = obj.getString("gender"),
                    allergies = obj.getString("allergies"),
                    chronicConditions = obj.getString("chronicConditions"),
                    language = obj.optString("language", "English"),
                    location = obj.optString("location", ""),
                    pin = obj.optString("pin", ""),
                    photoUri = obj.optString("photoUri", "").takeIf { it.isNotEmpty() }
                )
                dao.saveProfile(profile)
            }
        }

        // 2. Medical Documents
        if (rootObject.has("documents")) {
            val arr = rootObject.getJSONArray("documents")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val doc = MedicalDocument(
                    id = obj.optInt("id", 0),
                    type = obj.getString("type"),
                    date = obj.getString("date"),
                    providerName = obj.getString("providerName"),
                    uri = obj.getString("uri"),
                    imageBlob = obj.optString("imageBlob", "").takeIf { it.isNotEmpty() }?.let { Base64.decode(it, Base64.DEFAULT) },
                    analysis = obj.optString("analysis", "").takeIf { it.isNotEmpty() },
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertDocument(doc)
            }
        }

        // 3. Medications
        if (rootObject.has("medications")) {
            val arr = rootObject.getJSONArray("medications")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val med = Medication(
                    id = obj.optInt("id", 0),
                    name = obj.getString("name"),
                    dose = obj.getString("dose"),
                    frequency = obj.getString("frequency"),
                    doctorName = obj.getString("doctorName"),
                    status = obj.getString("status"),
                    startDate = obj.optString("startDate", ""),
                    endDate = obj.optString("endDate", ""),
                    reminderTime = obj.optString("reminderTime", "09:00 AM"),
                    profileId = obj.optString("profileId", "1"),
                    frequencyType = obj.optString("frequencyType", "Daily"),
                    frequencyDaysOfWeek = obj.optString("frequencyDaysOfWeek", ""),
                    notificationEnabled = obj.optBoolean("notificationEnabled", true)
                )
                dao.insertMedication(med)
            }
        }

        // 4. Medication Logs
        if (rootObject.has("medication_logs")) {
            val arr = rootObject.getJSONArray("medication_logs")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val log = MedicationLog(
                    id = obj.optInt("id", 0),
                    medicationId = obj.getInt("medicationId"),
                    medicationName = obj.getString("medicationName"),
                    logDate = obj.getString("logDate"),
                    logTime = obj.getString("logTime"),
                    status = obj.getString("status"),
                    notes = obj.optString("notes", ""),
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertMedicationLog(log)
            }
        }

        // 5. Doctor Visits
        if (rootObject.has("visits")) {
            val arr = rootObject.getJSONArray("visits")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val visit = DoctorVisit(
                    id = obj.optInt("id", 0),
                    doctorName = obj.getString("doctorName"),
                    speciality = obj.getString("speciality"),
                    hospital = obj.getString("hospital"),
                    date = obj.getString("date"),
                    reason = obj.getString("reason"),
                    notes = obj.getString("notes"),
                    linkedDocumentIds = obj.optString("linkedDocumentIds", ""),
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertVisit(visit)
            }
        }

        // 6. Vital Signs
        if (rootObject.has("vital_signs")) {
            val arr = rootObject.getJSONArray("vital_signs")
            val vSignsList = mutableListOf<VitalSign>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val vs = VitalSign(
                    id = obj.optInt("id", 0),
                    date = obj.getString("date"),
                    type = obj.getString("type"),
                    value1 = obj.getInt("value1"),
                    value2 = obj.optInt("value2", 0),
                    profileId = obj.optString("profileId", "1")
                )
                vSignsList.add(vs)
            }
            if (vSignsList.isNotEmpty()) {
                dao.insertVitalSigns(vSignsList)
            }
        }

        // 7. Daily Vitals
        if (rootObject.has("daily_vitals")) {
            val arr = rootObject.getJSONArray("daily_vitals")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dv = DailyVitals(
                    id = obj.optInt("id", 0),
                    date = obj.getString("date"),
                    systolic = obj.getInt("systolic"),
                    diastolic = obj.getInt("diastolic"),
                    heartRate = obj.getInt("heartRate"),
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertDailyVitals(dv)
            }
        }

        // 8. Health Expenses
        if (rootObject.has("health_expenses")) {
            val arr = rootObject.getJSONArray("health_expenses")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val exp = HealthExpense(
                    id = obj.optInt("id", 0),
                    description = obj.getString("description"),
                    amount = obj.getDouble("amount"),
                    date = obj.getString("date"),
                    category = obj.getString("category"),
                    profileName = obj.optString("profileName", "Janaka (Me)"),
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertExpense(exp)
            }
        }

        // 9. Symptom Logs
        if (rootObject.has("symptom_logs")) {
            val arr = rootObject.getJSONArray("symptom_logs")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val sym = SymptomLog(
                    id = obj.optInt("id", 0),
                    name = obj.getString("name"),
                    severity = obj.getInt("severity"),
                    date = obj.getString("date"),
                    status = obj.getString("status"),
                    notes = obj.optString("notes", ""),
                    profileId = obj.optString("profileId", "1")
                )
                dao.insertSymptomLog(sym)
            }
        }

        // 10. Saved Pharmacies
        if (rootObject.has("saved_pharmacies")) {
            val arr = rootObject.getJSONArray("saved_pharmacies")
            val list = mutableListOf<SavedPharmacy>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val ph = SavedPharmacy(
                    id = obj.getLong("id"),
                    name = obj.getString("name"),
                    type = obj.getString("type"),
                    lat = obj.optDouble("lat", 0.0),
                    lon = obj.optDouble("lon", 0.0),
                    tagsJson = obj.optString("tagsJson", ""),
                    town = obj.optString("town", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                )
                list.add(ph)
            }
            if (list.isNotEmpty()) {
                dao.insertSavedPharmacies(list)
            }
        }
    }
}
