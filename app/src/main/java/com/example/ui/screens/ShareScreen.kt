package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.PatientProfile
import com.example.data.model.Medication
import com.example.data.model.DailyVitals
import com.example.data.model.MedicalDocument
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import com.example.ui.theme.PrimaryDarkTeal
import com.example.ui.theme.White
import com.example.ui.theme.BackgroundLightTeal
import com.example.ui.theme.PastelGreen
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.AlertLightRed
import com.example.ui.theme.WarningLightAmber
import com.example.ui.theme.IconOrange
import com.example.ui.theme.Black

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareScreen(navController: NavController, viewModel: HealthViewModel) {
    val context = LocalContext.current
    var shareDocs by remember { mutableStateOf(true) }
    var shareMeds by remember { mutableStateOf(true) }
    var shareProfile by remember { mutableStateOf(true) }
    var shareVitals by remember { mutableStateOf(true) }
    var showQr by remember { mutableStateOf(false) }
    var isGeneratingPdf by remember { mutableStateOf(false) }

    // Active records collected from ViewModel
    val profile by viewModel.profile.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val bpVitalSigns by viewModel.bpVitalSigns.collectAsState()
    val sugarVitalSigns by viewModel.sugarVitalSigns.collectAsState()
    val cholesterolVitalSigns by viewModel.cholesterolVitalSigns.collectAsState()

    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current

    // Dynamically compile the Medical Pass text based on check box selections
    val qrText = remember(shareProfile, shareDocs, shareMeds, shareVitals, profile, medications, bpVitalSigns, sugarVitalSigns, cholesterolVitalSigns, documents) {
        generateMedicalPassText(
            profile = profile,
            medications = medications,
            vitalsBp = bpVitalSigns,
            vitalsSugar = sugarVitalSigns,
            vitalsChol = cholesterolVitalSigns,
            documents = documents,
            includeProfile = shareProfile,
            includeMedications = shareMeds,
            includeVitals = shareVitals,
            includeDocuments = shareDocs
        )
    }

    // Live bitmap generated from the compiled text
    val qrBitmap = remember(qrText) {
        generateQrCodeBitmap(qrText, 512)
    }

    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportPassword by remember { mutableStateOf("") }
    var importPassword by remember { mutableStateOf("") }
    var selectedBackupBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isBackingUp by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var showAllergyEditDialog by remember { mutableStateOf(false) }
    var allergyInputText by remember { mutableStateOf("") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val bytes = inputStream.readBytes()
                    selectedBackupBytes = bytes
                    importPassword = ""
                    showImportDialog = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error reading selected backup file.", Toast.LENGTH_LONG).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Share Records", "බෙදාගන්න", "பகிர்", color = White, scale = 0.95f)
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TrilingualText(
                        "Clinical PDF Export & Doctor Sharing",
                        "වෛද්‍ය PDF අපනයනය සහ වෛද්‍යවරයා සමඟ බෙදාගැනීම",
                        "மருத்துவ PDF ஏற்றுமதி மற்றும் மருத்துவருடன் பகிர்தல்",
                        color = PrimaryDarkTeal,
                        scale = 1.1f
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Compile, format, and generate a secure clinical-grade medical snapshot PDF of your local health portfolio. Perfect for emailing, printing, or presenting directly to your consulting physicians.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ------------------ PROMINENT HIGH-CONTRAST ALLERGY ALERT SECTION ------------------
            val currentProfile = profile
            if (currentProfile != null) {
                val hasAllergies = currentProfile.allergies.isNotBlank() && !currentProfile.allergies.equals("None", ignoreCase = true)
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasAllergies) AlertLightRed else WarningLightAmber
                    ),
                    border = BorderStroke(
                        width = 2.dp, 
                        color = if (hasAllergies) ErrorRed else IconOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = if (hasAllergies) Icons.Filled.Warning else Icons.Filled.Info,
                                contentDescription = if (hasAllergies) "Allergy Alert" else "Allergy Status",
                                tint = if (hasAllergies) ErrorRed else IconOrange,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                TrilingualText(
                                    english = if (hasAllergies) "⚠️ CRITICAL ALLERGY ALERT" else "⚠️ NO ACTIVE ALLERGIES",
                                    sinhala = if (hasAllergies) "අසාත්මිකතා අනතුරු ඇඟවීම" else "අසාත්මිකතා වාර්තා වී නැත",
                                    tamil = if (hasAllergies) "தீவிர ஒவ்வாமை எச்சரிக்கை" else "ஒவ்வாமை எதுவும் இல்லை",
                                    color = if (hasAllergies) ErrorRed else IconOrange,
                                    scale = 1.05f
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    allergyInputText = currentProfile.allergies
                                    showAllergyEditDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Edit Allergies",
                                    tint = if (hasAllergies) ErrorRed else IconOrange
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = if (hasAllergies) {
                                currentProfile.allergies
                            } else {
                                "No allergies reported. If you have any food, drug, or environmental allergies, click edit above to add them immediately."
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (hasAllergies) ErrorRed else Black,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                        
                        if (hasAllergies) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Medical Personnel Care: Do NOT administer contraindicated compounds. Scan or download the full Medical Pass below for details.",
                                fontSize = 11.sp,
                                color = ErrorRed.copy(alpha = 0.85f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (showAllergyEditDialog && currentProfile != null) {
                AlertDialog(
                    onDismissRequest = { showAllergyEditDialog = false },
                    title = {
                        TrilingualText(
                            english = "Manage Allergies & Triggers",
                            sinhala = "අසාත්මිකතා කළමනාකරණය",
                            tamil = "ஒவ்வாமை மேலாண்மை",
                            color = PrimaryDarkTeal,
                            scale = 1.0f
                        )
                    },
                    text = {
                        Column {
                            Text(
                                "Specify any hypersensitivities, pharmacological allergies (e.g., Penicillin, Sulfa drugs), food allergies, or severe animal/environmental triggers. This will immediately display on your high-contrast clinical dashboard and Medical Pass QR.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = allergyInputText,
                                onValueChange = { allergyInputText = it },
                                label = { Text("Allergic Subscriptions / Triggers") },
                                placeholder = { Text("e.g. Penicillin, Pollen, Asthmatic Triggers") },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val updatedProfile = currentProfile.copy(
                                    allergies = allergyInputText.trim()
                                )
                                viewModel.saveProfile(updatedProfile)
                                showAllergyEditDialog = false
                                Toast.makeText(context, "Allergic triggers successfully updated!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                        ) {
                            Text("Save Changes")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAllergyEditDialog = false }) {
                            Text("Cancel", color = PrimaryDarkTeal)
                        }
                    }
                )
            }

            Text("Select sections to include in the PDF report / QR snapshot:", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Checkbox(checked = shareProfile, onCheckedChange = { shareProfile = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            TrilingualText("Patient Demographic & Clinical Profile", "රෝගියාගේ තොරතුරු සහ පැතිකඩ", "நோயாளி சுயவிவரம்", scale = 0.85f)
                            Text("Name, blood group, age, allergies, chronic illnesses", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Checkbox(checked = shareDocs, onCheckedChange = { shareDocs = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            TrilingualText("Lab Reports, Documents & Visit Logs", "රසායනාගාර වාර්තා සහ උපදේශන සටහන්", "ஆய்வக அறிக்கைகள் மற்றும் ஆலோசனை", scale = 0.85f)
                            Text("Summary of uploaded clinical documents & recent consultations", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Checkbox(checked = shareMeds, onCheckedChange = { shareMeds = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            TrilingualText("Active Prescriptions & Medications", "සක්‍රීය බෙහෙත් වට්ටෝරු සහ ඖෂධ", "மருந்துச்சீட்டுகள் மற்றும் மருந்துகள்", scale = 0.85f)
                            Text("Current active medications block, dosages, frequency, status", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Divider(modifier = Modifier.padding(horizontal = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)

                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Checkbox(checked = shareVitals, onCheckedChange = { shareVitals = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            TrilingualText("Health Vitals & Clinical Trends", "වැදගත් සෞඛ්‍ය ලකුණු සහ ප්‍රවණතා", "முக்கிய ஆரோக்கிய அளவீடுகள்", scale = 0.85f)
                            Text("Systolic/diastolic BP, blood sugar tracking, cholesterol trends", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PDF Export button
            Button(
                onClick = {
                    if (!shareProfile && !shareDocs && !shareMeds && !shareVitals) {
                        Toast.makeText(context, "Please select at least one section to export.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGeneratingPdf = true
                    viewModel.generatePdfReport(
                        context = context,
                        includeProfile = shareProfile,
                        includeDocuments = shareDocs,
                        includeMedications = shareMeds,
                        includeVitals = shareVitals
                    ) { success, file ->
                        isGeneratingPdf = false
                        if (success && file != null) {
                            try {
                                val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Patient Clinical Snapshot PDF"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to share PDF: File system permission error", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Failed to generate Clinical PDF report.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isGeneratingPdf
            ) {
                if (isGeneratingPdf) {
                    CircularProgressIndicator(color = White, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Compiling Clinical PDF...")
                } else {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = White)
                    Spacer(modifier = Modifier.width(8.dp))
                    TrilingualText("Export Medical Snapshot (PDF)", "වෛද්‍ය වාර්තාව PDF ලෙස අපනයනය", "மருத்துவ சாதனங்கள் PDF ஏற்றுமதி", color = White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // QR Code button
            OutlinedButton(
                onClick = { showQr = !showQr },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDarkTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Filled.QrCode, contentDescription = null, tint = PrimaryDarkTeal)
                Spacer(modifier = Modifier.width(8.dp))
                TrilingualText(
                    if (showQr) "Hide Medical QR Code" else "Generate Secure QR Token",
                    if (showQr) "QR කේතය සඟවන්න" else "ආරක්ෂිත QR කේතය සාදන්න",
                    if (showQr) "QR குறியீட்டை மறை" else "பாதுகாப்பான QR ஐ உருவாக்கு",
                    color = PrimaryDarkTeal
                )
            }

            if (showQr) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                    border = BorderStroke(1.dp, PrimaryDarkTeal.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TrilingualText(
                            "One-Time Secure QR Code",
                            "ආරක්ෂිත වෛද්‍ය QR කේතය",
                            "பாதுகாப்பான மருத்துவ QR குறியீடு",
                            color = PrimaryDarkTeal,
                            scale = 1.0f
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Instant, completely offline clinic-grade medical pass. Doctors can scan it with any device scanner to load files instantly.",
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Render the real generated QR Code!
                        Box(
                            modifier = Modifier
                                .size(220.dp)
                                .background(White, RoundedCornerShape(12.dp))
                                .border(2.dp, PrimaryDarkTeal.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Medical Pass QR Code",
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = PrimaryDarkTeal)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Share / Copy Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (qrBitmap != null) {
                                        shareQrCodeImage(context, qrBitmap)
                                    } else {
                                        Toast.makeText(context, "QR code not ready yet.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share QR Image", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    val annotatedString = androidx.compose.ui.text.AnnotatedString(qrText)
                                    clipboardManager.setText(annotatedString)
                                    Toast.makeText(context, "Clinical records copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDarkTeal),
                                border = BorderStroke(1.dp, PrimaryDarkTeal),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy Text Pass", fontSize = 12.sp)
                            }
                        }

                        // Clinical Snapshot Plain-Text Preview Accordion
                        Spacer(modifier = Modifier.height(20.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.QrCode, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Encodes Data Preview:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryDarkTeal)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = qrText,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, PrimaryDarkTeal.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = PrimaryDarkTeal,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrilingualText(
                            "Secure Local Backup & Portability",
                            "ආරක්ෂිත වෛද්‍ය උපස්ථ කිරීම",
                            "பாதுகாப்பான உள்ளூர் காப்புப்பிரதி",
                            color = PrimaryDarkTeal,
                            scale = 1.0f
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Export all clinical profiles, medical documents (including attached lab results/images), active prescriptions, logs, consulting doctor visits, expenses, and symptom trends as an encrypted file. Share or restore this file to safely sync your entire medical profile across device migrations.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                exportPassword = ""
                                showExportDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Backup", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                filePickerLauncher.launch("*/*")
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDarkTeal),
                            border = BorderStroke(1.dp, PrimaryDarkTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import Backup", fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                TrilingualText(
                    "Set Security Password",
                    "මුරපදයක් ඇතුළත් කරන්න",
                    "கடவுச்சொல்லை அமைக்கவும்",
                    color = PrimaryDarkTeal,
                    scale = 1.0f
                )
            },
            text = {
                Column {
                    Text(
                        "Set a custom security password to encrypt your patient clinical data. Make sure to remember this password as it is required to decrypt and restore your medical dossier during migration.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = exportPassword,
                        onValueChange = { exportPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (exportPassword.isBlank()) {
                            Toast.makeText(context, "Password cannot be empty.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showExportDialog = false
                        isBackingUp = true
                        viewModel.exportEncryptedBackup(context, exportPassword) { success, file ->
                            isBackingUp = false
                            if (success && file != null) {
                                try {
                                    val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/octet-stream"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Share or Save Encrypted Medical Dossier"))
                                    Toast.makeText(context, "Backup exported successfully!", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Error sharing backup file.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Toast.makeText(context, "Failed to export medical data.", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                ) {
                    Text("Encrypt & Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                TrilingualText(
                    "Decrypt & Restore Backup",
                    "උපස්ථය නැවත සකසන්න",
                    "காப்புப்பிரதியை மீட்டமைக்கவும்",
                    color = PrimaryDarkTeal,
                    scale = 1.0f
                )
            },
            text = {
                Column {
                    Text(
                        "Enter the medical backup file password. Warning: Importing a backup will overwrite the existing clinical records, profiles, and logs on this current device with the backup data.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = importPassword,
                        onValueChange = { importPassword = it },
                        label = { Text("Backup Password") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val bytes = selectedBackupBytes
                        if (bytes == null) {
                            Toast.makeText(context, "No backup data loaded.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (importPassword.isBlank()) {
                            Toast.makeText(context, "Decryption password cannot be blank.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showImportDialog = false
                        isRestoring = true
                        viewModel.importEncryptedBackup(context, bytes, importPassword) { success, errorMsg ->
                            isRestoring = false
                            if (success) {
                                Toast.makeText(context, "Dossier restored successfully! Database reloaded.", Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Restore failed: ${errorMsg ?: "Invalid decryption key"}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                ) {
                    Text("Decrypt & Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        )
    }

    if (isBackingUp || isRestoring) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = PrimaryDarkTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(if (isBackingUp) "Backing Up Databases..." else "Restoring Databases...", fontSize = 16.sp)
                }
            }
        )
    }
}

// Helper: Dynanically compile the Medical Pass payload text
fun generateMedicalPassText(
    profile: PatientProfile?,
    medications: List<Medication>,
    vitalsBp: List<com.example.data.model.VitalSign>,
    vitalsSugar: List<com.example.data.model.VitalSign>,
    vitalsChol: List<com.example.data.model.VitalSign>,
    documents: List<MedicalDocument>,
    includeProfile: Boolean,
    includeMedications: Boolean,
    includeVitals: Boolean,
    includeDocuments: Boolean
): String {
    val sb = java.lang.StringBuilder()
    sb.append("🚨 AROGYA DIGITAL MEDICAL PASS 🚨\n")
    sb.append("===============================\n\n")

    if (includeProfile && profile != null) {
        sb.append("👤 PATIENT CLINICAL DATA\n")
        sb.append("• Name: ${profile.name.ifEmpty { "Patient Name" }}\n")
        sb.append("• Age / Gender: ${profile.age.ifEmpty { "N/A" }} / ${profile.gender.ifEmpty { "N/A" }}\n")
        sb.append("• Blood Group: ${profile.bloodGroup.ifEmpty { "N/A" }}\n")
        sb.append("• Allergies: ${profile.allergies.ifEmpty { "None Reported" }}\n")
        sb.append("• Chronic Illnesses: ${profile.chronicConditions.ifEmpty { "None Reported" }}\n")
        sb.append("\n")
    }

    if (includeMedications && medications.isNotEmpty()) {
        val activeMeds = medications.filter { it.status == "Active" }
        if (activeMeds.isNotEmpty()) {
            sb.append("💊 ACTIVE PRESCRIBED MEDICATIONS\n")
            activeMeds.forEach { med ->
                val freqDisplay = if (med.frequencyType == "Weekly") {
                    "Weekly on (${med.frequencyDaysOfWeek})"
                } else {
                    "Daily"
                }
                sb.append("• ${med.name} (${med.dose}) - $freqDisplay at ${med.reminderTime}\n")
            }
            sb.append("\n")
        }
    }

    if (includeVitals) {
        val hasBp = vitalsBp.isNotEmpty()
        val hasSugar = vitalsSugar.isNotEmpty()
        val hasChol = vitalsChol.isNotEmpty()

        if (hasBp || hasSugar || hasChol) {
            sb.append("📈 LATEST RECORDED HEALTH VITALS\n")
            if (hasBp) {
                val latestBp = vitalsBp.sortedByDescending { it.date }.first()
                sb.append("• Blood Pressure: ${latestBp.value1}/${latestBp.value2} mmHg (${latestBp.date})\n")
            }
            if (hasSugar) {
                val latestSugar = vitalsSugar.sortedByDescending { it.date }.first()
                sb.append("• Blood Sugar: ${latestSugar.value1} mg/dL (${latestSugar.date})\n")
            }
            if (hasChol) {
                val latestChol = vitalsChol.sortedByDescending { it.date }.first()
                sb.append("• Cholesterol: ${latestChol.value1} mg/dL (${latestChol.date})\n")
            }
            sb.append("\n")
        }
    }

    if (includeDocuments && documents.isNotEmpty()) {
        sb.append("📋 MEDICAL DOCUMENTS & LAB RESULTS\n")
        documents.take(3).forEach { doc ->
            sb.append("• [${doc.type}] ${doc.providerName} (${doc.date})\n")
        }
        if (documents.size > 3) {
            sb.append("• ...and ${documents.size - 3} more records\n")
        }
        sb.append("\n")
    }

    sb.append("===============================\n")
    sb.append("Scan secure offline local clinical snapshot.")
    return sb.toString()
}

// Helper: Generate real scannable QR Code bit matrix using ZXing
fun generateQrCodeBitmap(content: String, size: Int = 512): Bitmap? {
    if (content.isEmpty()) return null
    return try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper: Share generated QR image file using FileProvider
fun shareQrCodeImage(context: android.content.Context, bitmap: Bitmap) {
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        cachePath.mkdirs()
        val imageFile = java.io.File(cachePath, "medical_pass_qr.png")
        val stream = java.io.FileOutputStream(imageFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val contentUri = FileProvider.getUriForFile(context, "com.example.fileprovider", imageFile)

        if (contentUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "image/png"
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Medical Pass QR Code"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Sharing QR failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
