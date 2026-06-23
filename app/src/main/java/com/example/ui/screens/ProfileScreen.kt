package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.example.navigation.Screen
import com.example.ui.theme.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.testTag
import com.example.data.model.PatientProfile
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: HealthViewModel) {
    val context = LocalContext.current
    val profile by viewModel.profile.collectAsState()
    val activeMemberId by viewModel.activeMemberId.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var bloodGroup by remember { mutableStateOf(profile?.bloodGroup ?: "") }
    var age by remember { mutableStateOf(profile?.age ?: "") }
    var gender by remember { mutableStateOf(profile?.gender ?: "") }
    var allergies by remember { mutableStateOf(profile?.allergies ?: "") }
    var chronic by remember { mutableStateOf(profile?.chronicConditions ?: "") }
    var location by remember { mutableStateOf(profile?.location ?: "") }

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupPassword by remember { mutableStateOf("") }
    var isBackingUp by remember { mutableStateOf(false) }
    val lastSync by viewModel.lastSyncTime.collectAsState()
    val lastBackup by viewModel.lastBackupTime.collectAsState()
    var isSimulatingSync by remember { mutableStateOf(false) }

    var showSetFamilyPinDialog by remember { mutableStateOf(false) }
    var familyPinInput by remember { mutableStateOf("") }
    var isBypassChecked by remember { mutableStateOf(viewModel.isEmergencyBypassEnabled) }

    val createDocLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            isBackingUp = true
            viewModel.createBackup(context, uri, backupPassword) { success ->
                isBackingUp = false
                backupPassword = ""
                if (success) {
                    Toast.makeText(context, "Backup created successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Backup failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            backupPassword = ""
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val file = java.io.File(context.filesDir, "profile_pic_${activeMemberId}.jpg")
                    file.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    viewModel.updateProfilePhoto(file.absolutePath)
                    Toast.makeText(context, "Profile picture updated!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(profile, activeMemberId) {
        if (profile != null) {
            name = profile!!.name
            bloodGroup = profile!!.bloodGroup
            age = profile!!.age
            gender = profile!!.gender
            allergies = profile!!.allergies
            chronic = profile!!.chronicConditions
            location = profile!!.location
        } else {
            val fm = familyMembers.find { it.id == activeMemberId }
            name = fm?.name ?: ""
            bloodGroup = fm?.bloodGroup ?: ""
            age = fm?.age ?: ""
            gender = ""
            allergies = ""
            chronic = ""
            location = ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Profile", "පැතිකඩ", "சுயவிவரம்", color = White, scale = 0.95f)
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            // Profile Picture Card
            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val photoPath = profile?.photoUri
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .background(White)
                            .border(2.dp, PrimaryDarkTeal, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoPath.isNullOrEmpty() && java.io.File(photoPath).exists()) {
                            AsyncImage(
                                model = java.io.File(photoPath),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Default Avatar",
                                tint = PrimaryDarkTeal,
                                modifier = Modifier.size(70.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            modifier = Modifier.testTag("upload_photo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", fontSize = 13.sp)
                        }

                        if (!photoPath.isNullOrEmpty() && java.io.File(photoPath).exists()) {
                            OutlinedButton(
                                onClick = {
                                    try {
                                        val file = java.io.File(photoPath)
                                        if (file.exists()) {
                                            file.delete()
                                        }
                                        viewModel.updateProfilePhoto(null)
                                        Toast.makeText(context, "Profile picture deleted!", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                                modifier = Modifier.testTag("delete_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Delete", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("City / Town Location") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = bloodGroup,
                onValueChange = { bloodGroup = it },
                label = { Text("Blood Group") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = gender,
                onValueChange = { gender = it },
                label = { Text("Gender") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = allergies,
                onValueChange = { allergies = it },
                label = { Text("Allergies") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = chronic,
                onValueChange = { chronic = it },
                label = { Text("Chronic Conditions") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            Button(
                onClick = {
                    viewModel.saveProfile(
                        PatientProfile(
                            id = activeMemberId,
                            name = name,
                            bloodGroup = bloodGroup,
                            age = age,
                            gender = gender,
                            allergies = allergies,
                            chronicConditions = chronic,
                            location = location,
                            photoUri = profile?.photoUri
                        )
                    )
                    Toast.makeText(context, "Profile Saved Locally!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Save Profile")
            }

            Spacer(modifier = Modifier.height(32.dp))
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = GrayText)
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TrilingualText("Language", "භාෂාව", "மொழி", color = PrimaryDarkTeal, scale = 1.1f)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose your preferred language.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                    val currentLang by viewModel.currentLanguage.collectAsState()
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        LangButton("ALL", "ALL", currentLang, Modifier.weight(1f)) { viewModel.setLanguage("ALL") }
                        LangButton("EN", "EN", currentLang, Modifier.weight(1f)) { viewModel.setLanguage("EN") }
                        LangButton("SI", "සිංහල", currentLang, Modifier.weight(1f)) { viewModel.setLanguage("SI") }
                        LangButton("TA", "தமிழ்", currentLang, Modifier.weight(1f)) { viewModel.setLanguage("TA") }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TrilingualText("Cloud Sync", "වලාකුළු සමමුහුර්තකරණය", "மேகக்கணி ஒத்திசைவு", color = PrimaryDarkTeal, scale = 1.1f)
                    
                    val currentUser = viewModel.auth?.currentUser
                    if (currentUser != null) {
                        Text("Logged in as ${currentUser.email ?: "Google User"}", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp), color = PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                    } else {
                        Text("Securely backup and sync your profile using Google Sign-in and Firebase.", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 6.dp))
                    }
                    
                    if (lastSync != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .background(com.example.ui.theme.PastelGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text("Last Synced (Sandbox): $lastSync", fontSize = 12.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.SemiBold)
                        }
                    } else {
                        Text("Status: Unsynced (No google-services.json details fully attached)", fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 12.dp))
                    }
                    
                    Button(
                        onClick = { 
                            val uid = viewModel.auth?.currentUser?.uid ?: "default_user_sync_id"
                            // Check if Firebase is initialized and perform Sync
                            viewModel.syncToFirestore(uid) { success ->
                                if (success) {
                                    Toast.makeText(context, "Cloud sync successful", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Standard Firebase Sync failed: Add google-services.json to proceed, or use Cloud Sandbox Sync below.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal.copy(alpha = 0.8f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Trigger Google & Firebase Sync")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = { 
                            isSimulatingSync = true
                            viewModel.performSimulatedSync {
                                isSimulatingSync = false
                                Toast.makeText(context, "Cloud Sandbox Sync completed successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSimulatingSync
                    ) {
                        if (isSimulatingSync) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryDarkTeal, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing to Cloud...")
                        } else {
                            Text("Run Cloud Sandbox Sync")
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TrilingualText("Backup Data", "දත්ත උපස්ථය", "தரவு காப்பு", color = PrimaryDarkTeal, scale = 1.1f)
                    
                    if (lastBackup != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .background(com.example.ui.theme.IconGreen, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text("Last Saved Backup: $lastBackup", fontSize = 12.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .padding(end = 6.dp)
                                    .size(8.dp)
                                    .background(com.example.ui.theme.IconOrange, androidx.compose.foundation.shape.CircleShape)
                            )
                            Text("Status: Never Backed Up", fontSize = 12.sp, color = IconOrange, fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Text("Export all records as a password-protected ZIP file.", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    Button(
                        onClick = { showBackupDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBackingUp
                    ) {
                        Text(if (isBackingUp) "Processing..." else "Create ZIP Backup")
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val createJsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
                        if (uri != null) {
                            isBackingUp = true
                            viewModel.exportAsJson(context, uri) { success ->
                                isBackingUp = false
                                if (success) {
                                    Toast.makeText(context, "JSON Export successful", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "JSON Export failed", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }

                    Text("Or export raw data as JSON.", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    OutlinedButton(
                        onClick = { 
                            val sdf = java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault())
                            val fileName = "HealthWallet_Data_${sdf.format(java.util.Date())}.json"
                            createJsonLauncher.launch(fileName) 
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBackingUp
                    ) {
                        Text("Export as JSON")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TrilingualText("App Security", "යෙදුම් ආරක්ෂාව", "பயன்பாட்டு பாதுகாப்பு", color = PrimaryDarkTeal, scale = 1.1f)
                    Text("Secure your medical records with an app passcode.", fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    val hasPin = viewModel.appPin != null
                    
                    Button(
                        onClick = { 
                            if (hasPin) {
                                viewModel.setAppPin("")
                                Toast.makeText(context, "Passcode Disabled", Toast.LENGTH_SHORT).show()
                            } else {
                                navController.navigate(Screen.Passcode.route)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasPin) androidx.compose.ui.graphics.Color.Red else PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (hasPin) "Disable Passcode" else "Set Passcode")
                    }

                    val familyPin = viewModel.familyPin
                    val hasFamilyPin = familyPin != null
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = PrimaryDarkTeal.copy(alpha = 0.2f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TrilingualText("🛡️ Dual Action & Family Access", "ද්විත්ව ක්‍රියාකාරී පවුලේ ප්‍රවේශය", "இரட்டை செயல்பாடு மற்றும் குடும்ப அணுகல்", color = PrimaryDarkTeal, scale = 1.0f)
                    Text("Allow alternative family members to unlock the wallet to present critical historical medical data to doctors during an admission or health emergency.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                familyPinInput = familyPin ?: ""
                                showSetFamilyPinDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (hasFamilyPin) "Edit Family PIN" else "Set Family PIN", fontSize = 12.sp)
                        }
                        
                        if (hasFamilyPin) {
                            Button(
                                onClick = { 
                                    viewModel.setFamilyPin("")
                                    Toast.makeText(context, "Family Passcode Disabled", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.ui.graphics.Color.Red),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Disable Family PIN", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Passwordless Emergency Bypass", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Permit instant access to clinical summaries/Med Pass inside the app without any lock code in an active medical emergency.", fontSize = 11.sp, color = GrayText)
                        }
                        Switch(
                            checked = isBypassChecked,
                            onCheckedChange = { checked ->
                                isBypassChecked = checked
                                viewModel.setEmergencyBypassEnabled(checked)
                                Toast.makeText(context, if (checked) "Emergency Bypass Enabled!" else "Emergency Bypass Disabled.", Toast.LENGTH_SHORT).show()
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryDarkTeal, checkedTrackColor = BackgroundLightTeal)
                        )
                    }
                }
            }

            if (showSetFamilyPinDialog) {
                AlertDialog(
                    onDismissRequest = { showSetFamilyPinDialog = false },
                    title = {
                        TrilingualText(
                            english = "Configure Secondary Family PIN",
                            sinhala = "පවුලේ අය සඳහා මුරපදය",
                            tamil = "குடும்பத்திற்கான இரகசிய குறியீடு",
                            color = PrimaryDarkTeal,
                            scale = 1.0f
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Choose a secure 4-digit PIN for secondary access. You can share this PIN with alternative family members or caregivers, so they can access and disclose your medical profiles in hospitals.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = familyPinInput,
                                onValueChange = { input -> 
                                    if (input.all { it.isDigit() } && input.length <= 4) {
                                        familyPinInput = input 
                                    }
                                },
                                label = { Text("Secondary Family PIN") },
                                placeholder = { Text("4 Digits") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (familyPinInput.length < 4) {
                                    Toast.makeText(context, "Secondary PIN must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                viewModel.setFamilyPin(familyPinInput)
                                showSetFamilyPinDialog = false
                                Toast.makeText(context, "Family PIN saved successfully!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                        ) {
                            Text("Save PIN")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSetFamilyPinDialog = false }) {
                            Text("Cancel", color = PrimaryDarkTeal)
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal),
                modifier = Modifier.fillMaxWidth().testTag("premium_settings_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Premium",
                            tint = IconOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TrilingualText("Premium Family Plan", "ප්‍රිමියම් පවුලේ සැලැස්ම", "பிரீமியம் குடும்பத் திட்டம்", color = PrimaryDarkTeal, scale = 1.1f)
                    }
                    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isPremiumUser) "Status: Active Premium Subscriber" else "Status: Standard Edition (Limit 3 profiles)",
                        fontSize = 12.sp,
                        color = if (isPremiumUser) IconGreen else GrayText,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text("Unlock unlimited profiles, smart insights, and advanced backups.", fontSize = 14.sp, modifier = Modifier.padding(bottom = 12.dp))
                    Button(
                        onClick = { navController.navigate(Screen.PremiumFamily.route) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth().testTag("view_premium_benefits")
                    ) {
                        Text("View Benefits & Support Plan")
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        if (showBackupDialog) {
            AlertDialog(
                onDismissRequest = { 
                    showBackupDialog = false 
                    backupPassword = ""
                },
                title = { Text("Backup Password") },
                text = {
                    Column {
                        Text("Enter a password to encrypt your backup file. You will need this password to restore your data.")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = backupPassword,
                            onValueChange = { backupPassword = it },
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showBackupDialog = false
                        val sdf = java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault())
                        val fileName = "HealthWallet_Backup_${sdf.format(java.util.Date())}.zip"
                        createDocLauncher.launch(fileName)
                    }) {
                        Text("Continue", color = PrimaryDarkTeal)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { 
                        showBackupDialog = false 
                        backupPassword = ""
                    }) {
                        Text("Cancel", color = GrayText)
                    }
                }
            )
        }
    }
}

@Composable
fun LangButton(text: String, langCode: String, currentLang: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val selected = langCode == currentLang
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryDarkTeal else BackgroundLightTeal,
            contentColor = if (selected) White else PrimaryDarkTeal
        ),
        shape = MaterialTheme.shapes.small,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(text, fontSize = 12.sp, maxLines = 1)
    }
}
