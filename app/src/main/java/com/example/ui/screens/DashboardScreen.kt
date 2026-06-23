package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import com.example.R
import com.example.navigation.Screen
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import com.example.ui.theme.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.data.model.MedicationLog
import com.example.data.model.Medication
import androidx.compose.ui.platform.testTag

@Composable
fun DashboardScreen(navController: NavController, viewModel: HealthViewModel) {
    val profile by viewModel.profile.collectAsState()
    val medications by viewModel.medications.collectAsState()
    val bpVitals by viewModel.bpVitalSigns.collectAsState()
    val sugarVitals by viewModel.sugarVitalSigns.collectAsState()
    val cholesterolVitals by viewModel.cholesterolVitalSigns.collectAsState()
    val dailyVitalsList by viewModel.dailyVitals.collectAsState()
    val medicationLogs by viewModel.medicationLogs.collectAsState()
    val familyMembers by viewModel.familyMembers.collectAsState()
    val activeMemberId by viewModel.activeMemberId.collectAsState()
    val expenses by viewModel.expenses.collectAsState()
    val symptomLogs by viewModel.symptomLogs.collectAsState()
    val vaccineLogs by viewModel.vaccineLogs.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val documents by viewModel.documents.collectAsState()
    
    val context = LocalContext.current
    val lastBackupTime by viewModel.lastBackupTime.collectAsState()
    var isBackingUp by remember { mutableStateOf(false) }
    
    val createJsonLauncherForDashboard = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            isBackingUp = true
            viewModel.exportAsJson(context, uri) { success ->
                isBackingUp = false
                if (success) {
                    Toast.makeText(context, "Local backup saved successfully!!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Local backup failed to save.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    val todayStrForTop = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val activeMedsForTop = medications.filter { it.status == "Active" }
    val logsTodayForTop = medicationLogs.filter { it.logDate == todayStrForTop && it.status == "Taken" }
    val untakenMedsCount = activeMedsForTop.count { med -> logsTodayForTop.none { it.medicationId == med.id } }

    val pName = profile?.name?.ifEmpty { "Patient Name" } ?: "Patient Name"
    val firstName = if (pName == "Patient Name" || pName.isEmpty()) "Patient Name" else (pName.split(" ").firstOrNull() ?: "Patient Name")

    var selectedTab by remember { mutableStateOf(0) } // 0: Overview, 1: Clinical Trends, 2: Current Status
    var selectedTimeframe by remember { mutableStateOf("1Y") } // "3M", "6M", "1Y"
    var dashboardVitalsTab by remember { mutableStateOf(0) } // 0: BP, 1: BS, 2: Cholesterol

    // Home logger state
    var sysInput by remember { mutableStateOf("") }
    var diaInput by remember { mutableStateOf("") }
    var hrInput by remember { mutableStateOf("") }
    var bsInput by remember { mutableStateOf("") }
    var logNote by remember { mutableStateOf("") }
    var logDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }

    // Arogya Hub state variables
    var showAddProfileDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }
    var newProfileAge by remember { mutableStateOf("") }
    var newProfileRelation by remember { mutableStateOf("Wife") }
    var newProfileBloodGroup by remember { mutableStateOf("O+") }

    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var newExpenseDesc by remember { mutableStateOf("") }
    var newExpenseAmount by remember { mutableStateOf("") }
    var newExpenseCategory by remember { mutableStateOf("Doctor") }
    var newExpenseProfile by remember { mutableStateOf("Janaka (Me)") }

    var showAddSymptomDialog by remember { mutableStateOf(false) }
    var newSymptomName by remember { mutableStateOf("Fever") }
    var newSymptomSeverity by remember { mutableStateOf(5) }

    var showAddVaccineDialog by remember { mutableStateOf(false) }
    var newVaccineName by remember { mutableStateOf("") }
    var newVaccineDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var newVaccineStatus by remember { mutableStateOf("Completed") }
    var newVaccineMember by remember { mutableStateOf("Janaka (Me)") }

    var selectedAilmentIndex by remember { mutableStateOf(0) }
    var selectedDrugIndex by remember { mutableStateOf(0) }
    var showPremiumSubscriptionDialog by remember { mutableStateOf(false) }
    var priceComparisonTriggered by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LightGrayBackground)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Menu, contentDescription = "Menu", modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
                AppTitleText(scale = 1.0f)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.clickable { navController.navigate(Screen.Medications.route) }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", modifier = Modifier.size(28.dp))
                    if (untakenMedsCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 2.dp, y = (-2).dp)
                                .clip(CircleShape)
                                .background(ErrorRed)
                                .border(2.dp, LightGrayBackground, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(untakenMedsCount.toString(), color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clickable { navController.navigate(Screen.Chat.route) }
                        .padding(4.dp)
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = "Arogya AI Chatbot",
                        tint = PrimaryDarkTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = Black)
            }
        }

        // Header Section with Image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(140.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .align(Alignment.CenterStart)
                    .padding(bottom = 20.dp)
                    .zIndex(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Profile Avatar (Clickable to navigate to Profile)
                val photoPath = profile?.photoUri
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(1.5.dp, PrimaryDarkTeal, CircleShape)
                        .clickable { navController.navigate(Screen.Profile.route) },
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
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text("Good morning,", fontSize = 13.sp, color = GrayText)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(firstName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Black)
                        Spacer(modifier = Modifier.width(2.dp))
                        Text("👋", fontSize = 16.sp)
                    }
                    Text("Health summary", fontSize = 11.sp, color = GrayText)
                }
            }
            
            // Illustration
            Image(
                painter = painterResource(id = R.drawable.img_sri_lankan_family_1781683501360),
                contentDescription = "Family Illustration",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .width(190.dp)
                    .offset(y = 20.dp, x = 10.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (untakenMedsCount > 0 || activeMedsForTop.isNotEmpty()) {
            QuickMedicationTracker(
                activeMeds = activeMedsForTop,
                logsToday = logsTodayForTop,
                onToggleTaken = { med, isTaken ->
                    if (isTaken) {
                        viewModel.insertMedicationLog(
                            MedicationLog(
                                id = 0,
                                profileId = activeMemberId,
                                medicationId = med.id,
                                medicationName = med.name,
                                logDate = todayStrForTop,
                                logTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date()),
                                status = "Taken",
                                notes = ""
                            )
                        )
                    } else {
                        // Mark as missed or skipped
                        val log = medicationLogs.find { it.medicationId == med.id && it.logDate == todayStrForTop && it.status == "Taken" }
                        if (log != null) {
                            viewModel.deleteMedicationLog(log)
                        }
                    }
                },
                onAddMedication = { name, dose, schedule ->
                    viewModel.insertMedication(com.example.data.model.Medication(
                        id = 0,
                        profileId = activeMemberId,
                        name = name,
                        dose = dose,
                        frequency = schedule,
                        doctorName = "Self logged",
                        status = "Active",
                        startDate = todayStrForTop,
                        endDate = "",
                        reminderTime = "08:00 AM"
                    ))
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            // Show placeholder if empty to allow adding
            QuickMedicationTracker(
                activeMeds = activeMedsForTop,
                logsToday = logsTodayForTop,
                onToggleTaken = { _, _ -> },
                onAddMedication = { name, dose, schedule ->
                    viewModel.insertMedication(com.example.data.model.Medication(
                        id = 0,
                        profileId = activeMemberId,
                        name = name,
                        dose = dose,
                        frequency = schedule,
                        doctorName = "Self logged",
                        status = "Active",
                        startDate = todayStrForTop,
                        endDate = "",
                        reminderTime = "08:00 AM"
                    ))
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        BackupPromptCard(
            lastBackupTime = lastBackupTime,
            isBackingUp = isBackingUp,
            onExportBackup = {
                val sdf = java.text.SimpleDateFormat("yyyy_MM_dd_HHmm", java.util.Locale.getDefault())
                val fileName = "HealthWallet_Data_Backup_${sdf.format(java.util.Date())}.json"
                createJsonLauncherForDashboard.launch(fileName)
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Re-usable tab selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tab0Selected = selectedTab == 0
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = if (tab0Selected) PrimaryDarkTeal else White),
                modifier = Modifier.clickable { selectedTab = 0 },
                border = BorderStroke(1.dp, if (tab0Selected) Color.Transparent else BackgroundLightTeal)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.SpaceDashboard, contentDescription = null, tint = if (tab0Selected) White else PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    TrilingualText("Overview", "මුල් දසුන", "மேலோட்டம்", color = if (tab0Selected) White else PrimaryDarkTeal, scale = 0.85f)
                }
            }

            val tab1Selected = selectedTab == 1
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = if (tab1Selected) PrimaryDarkTeal else White),
                modifier = Modifier.clickable { selectedTab = 1 },
                border = BorderStroke(1.dp, if (tab1Selected) Color.Transparent else BackgroundLightTeal)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ShowChart, contentDescription = null, tint = if (tab1Selected) White else PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    TrilingualText("Clinical Trends", "සායනික දත්ත", "மருத்துவ போக்குகள்", color = if (tab1Selected) White else PrimaryDarkTeal, scale = 0.85f)
                }
            }

            val tab2Selected = selectedTab == 2
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = if (tab2Selected) PrimaryDarkTeal else White),
                modifier = Modifier.clickable { selectedTab = 2 },
                border = BorderStroke(1.dp, if (tab2Selected) Color.Transparent else BackgroundLightTeal)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonitorHeart, contentDescription = null, tint = if (tab2Selected) White else PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    TrilingualText("Current Status", "වත්මන් තත්ත්වය", "தற்போதைய நிலை", color = if (tab2Selected) White else PrimaryDarkTeal, scale = 0.85f)
                }
            }

            val tab3Selected = selectedTab == 3
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = if (tab3Selected) PrimaryDarkTeal else White),
                modifier = Modifier.clickable { selectedTab = 3 },
                border = BorderStroke(1.dp, if (tab3Selected) Color.Transparent else BackgroundLightTeal)
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.HealthAndSafety, contentDescription = null, tint = if (tab3Selected) White else PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    TrilingualText("Arogya Hub", "දේශීය සුවය", "ஆரோக்கிய மையம்", color = if (tab3Selected) White else PrimaryDarkTeal, scale = 0.85f)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // ================= Overview TabContent =================
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(8.dp, spotColor = PrimaryDarkTeal.copy(alpha = 0.1f), shape = RoundedCornerShape(24.dp))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SnapshotGradientStart, SnapshotGradientEnd)
                                )
                            )
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Your Medical Snapshot", color = White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            }
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = White)
                        }
                    }

                    Row(modifier = Modifier.padding(20.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            SnapshotItem(icon = Icons.Outlined.WaterDrop, iconTint = ErrorRed, title = "Blood Group", value = profile?.bloodGroup?.ifEmpty { "O+" } ?: "O+", valueColor = ErrorRed)
                            Spacer(Modifier.height(16.dp))
                            SnapshotItem(icon = Icons.Outlined.Coronavirus, iconTint = ErrorRed, title = "Allergies", value = profile?.allergies?.ifEmpty { "Penicillin" } ?: "Penicillin", valueColor = ErrorRed)
                            Spacer(Modifier.height(16.dp))
                            SnapshotItem(icon = Icons.Outlined.CalendarToday, iconTint = PrimaryDarkTeal, title = "Last Doctor Visit", value = "12 May 2024", valueColor = Black)
                            Spacer(Modifier.height(16.dp))
                            SnapshotItem(icon = Icons.Outlined.PhoneInTalk, iconTint = PrimaryDarkTeal, title = "Emergency Contact", value = "Wife • 077 123 4567", valueColor = Black)
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(start = 16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .background(LightGrayBackground, RoundedCornerShape(12.dp))
                                    .border(1.dp, BackgroundLightTeal, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = "QR Code", modifier = Modifier.size(80.dp), tint = PrimaryDarkTeal)
                            }
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = { navController.navigate(Screen.Share.route) },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Show Doctor Pass", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 0.85f },
                            modifier = Modifier.size(90.dp),
                            color = IconGreen,
                            trackColor = BackgroundLightTeal,
                            strokeWidth = 6.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                            Text("85", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Black)
                            Text("Excellent", fontSize = 10.sp, color = GrayText)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Health Score", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Outlined.Info, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText)
                        }
                        Text("You're doing great!", fontSize = 12.sp, color = GrayText)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniStatCard("5", "Reports", Icons.Outlined.Description, IconBlue, PastelBlue)
                            MiniStatCard("3", "Prescriptions", Icons.Outlined.Medication, IconGreen, PastelGreen)
                            MiniStatCard("2", "Active\nConditions", Icons.Outlined.MonitorHeart, IconOrange, PastelOrange)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Latest Clinical Vitals (From Scanned Reports) Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable { selectedTab = 1 },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Latest Clinical Vitals", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Black)
                Text("View Trends >", fontSize = 14.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.Medium)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Blood Pressure Column
                    val latestBP = bpVitals.lastOrNull()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(PastelPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.MonitorHeart, contentDescription = null, tint = IconPurple, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Blood Pressure", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (latestBP != null) "${latestBP.value1}/${latestBP.value2}" else "--/--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Black
                        )
                        Text(
                            text = "mmHg",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                        if (latestBP != null) {
                            Text(
                                text = formatLabelDate(latestBP.date),
                                fontSize = 9.sp,
                                color = PrimaryDarkTeal,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        } else {
                            Text("No Report", fontSize = 9.sp, color = GrayText)
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(80.dp).background(BackgroundLightTeal))

                    // Blood Sugar Column
                    val latestSugar = sugarVitals.lastOrNull()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(PastelPink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.WaterDrop, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Blood Sugar", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (latestSugar != null) "${latestSugar.value1}" else "---",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Black
                        )
                        Text(
                            text = "mg/dL",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                        if (latestSugar != null) {
                            Text(
                                text = formatLabelDate(latestSugar.date),
                                fontSize = 9.sp,
                                color = PrimaryDarkTeal,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        } else {
                            Text("No Report", fontSize = 9.sp, color = GrayText)
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(80.dp).background(BackgroundLightTeal))

                    // Cholesterol Column
                    val latestChol = cholesterolVitals.lastOrNull()
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(PastelOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Science, contentDescription = null, tint = IconOrange, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Cholesterol", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (latestChol != null) "${latestChol.value1}" else "---",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Black
                        )
                        Text(
                            text = "mg/dL",
                            fontSize = 10.sp,
                            color = GrayText
                        )
                        if (latestChol != null) {
                            Text(
                                text = formatLabelDate(latestChol.date),
                                fontSize = 9.sp,
                                color = PrimaryDarkTeal,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        } else {
                            Text("No Report", fontSize = 9.sp, color = GrayText)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= Clinical Trend Graphs Snapshot Switcher =================
            SectionHeader("Clinical Trend Graphs Overview", "View Full Trends >", onActionClick = { selectedTab = 1 })
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // BP Chip
                val bpSelected = dashboardVitalsTab == 0
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (bpSelected) IconPurple.copy(alpha = 0.15f) else White)
                        .border(1.dp, if (bpSelected) IconPurple else BackgroundLightTeal, RoundedCornerShape(12.dp))
                        .clickable { dashboardVitalsTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.MonitorHeart, 
                            contentDescription = null, 
                            tint = if (bpSelected) IconPurple else GrayText, 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "BP",
                            color = if (bpSelected) IconPurple else GrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Sugar Chip
                val sugarSelected = dashboardVitalsTab == 1
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (sugarSelected) ErrorRed.copy(alpha = 0.12f) else White)
                        .border(1.dp, if (sugarSelected) ErrorRed else BackgroundLightTeal, RoundedCornerShape(12.dp))
                        .clickable { dashboardVitalsTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.WaterDrop, 
                            contentDescription = null, 
                            tint = if (sugarSelected) ErrorRed else GrayText, 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Sugar",
                            color = if (sugarSelected) ErrorRed else GrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Cholesterol Chip
                val cholSelected = dashboardVitalsTab == 2
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (cholSelected) IconOrange.copy(alpha = 0.15f) else White)
                        .border(1.dp, if (cholSelected) IconOrange else BackgroundLightTeal, RoundedCornerShape(12.dp))
                        .clickable { dashboardVitalsTab = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Science, 
                            contentDescription = null, 
                            tint = if (cholSelected) IconOrange else GrayText, 
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cholesterol",
                            color = if (cholSelected) IconOrange else GrayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (dashboardVitalsTab) {
                0 -> {
                    val dbBP = filterVitalsByTimeframe(bpVitals, "1Y")
                    ClinicalTrendChart(
                        title = "Clinical Blood Pressure (mmHg)",
                        titleSi = "සායනික රුධිර පීඩනය (mmHg)",
                        titleTa = "மருத்துவ இரத்த அழுத்தம் (mmHg)",
                        vitals = dbBP,
                        primaryColor = IconPurple,
                        secondaryColor = IconGreen,
                        isDouble = true,
                        unit = "mmHg"
                    )
                }
                1 -> {
                    val dbSugar = filterVitalsByTimeframe(sugarVitals, "1Y")
                    ClinicalTrendChart(
                        title = "Blood Sugar / Fasting Glucose (mg/dL)",
                        titleSi = "රුධිර සීනි / නිරාහාර ග්ලූකෝස් (mg/dL)",
                        titleTa = "இரத்த சர்க்கரை / உண்ணாவிரத குளுக்கோஸ் (mg/dL)",
                        vitals = dbSugar,
                        primaryColor = ErrorRed,
                        unit = "mg/dL"
                    )
                }
                2 -> {
                    val dbChol = filterVitalsByTimeframe(cholesterolVitals, "1Y")
                    ClinicalTrendChart(
                        title = "Total Cholesterol (mg/dL)",
                        titleSi = "සම්පූර්ණ කොලෙස්ටරෝල් (mg/dL)",
                        titleTa = "மொத்த கொலஸ்ட்ரால் (mg/dL)",
                        vitals = dbChol,
                        primaryColor = IconOrange,
                        unit = "mg/dL"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ================= Medication Quick-Log Check-in Block =================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clickable { navController.navigate(Screen.Medications.route) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Medication Reminders", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Black)
                Text("Log Intake >", fontSize = 14.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.Medium)
            }

            val activeMeds = medications.filter { it.status == "Active" }
            val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
            val logsToday = medicationLogs.filter { it.logDate == todayStr }
            val takenToday = logsToday.count { it.status == "Taken" }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    if (activeMeds.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Screen.Medications.route) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.Medication, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("No active medications setup", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
                                Text("Tap here to schedule daily pill reminders.", fontSize = 12.sp, color = GrayText)
                            }
                        }
                    } else {
                        // Header statistics info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Today's Progress: $takenToday of ${activeMeds.size} Taken",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Black
                                )
                                val compliancePercent = (takenToday.toFloat() / activeMeds.size.toFloat() * 100).toInt()
                                Text(
                                    text = "Daily Compliance: $compliancePercent%",
                                    fontSize = 11.sp,
                                    color = if (compliancePercent == 100) IconGreen else GrayText,
                                    fontWeight = if (compliancePercent == 100) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    progress = { takenToday.toFloat() / activeMeds.size.toFloat() },
                                    modifier = Modifier.size(40.dp),
                                    color = PrimaryDarkTeal,
                                    trackColor = BackgroundLightTeal,
                                    strokeWidth = 4.dp
                                )
                                Text(
                                    "${(takenToday.toFloat() / activeMeds.size.toFloat() * 100).toInt()}%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = PrimaryDarkTeal
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(BackgroundLightTeal))
                        Spacer(modifier = Modifier.height(12.dp))

                        val untakenMeds = activeMeds.filter { med ->
                            logsToday.none { log -> log.medicationId == med.id && log.status == "Taken" }
                        }

                        if (untakenMeds.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PastelGreen, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = IconGreen, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Perfect Compliance! All of today's doses taken.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = IconGreen
                                )
                            }
                        } else {
                            Text(
                                "Remaining Today:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = GrayText,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            
                            untakenMeds.forEach { med ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(LightGrayBackground, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(med.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Black)
                                        Text("${med.dose} • ${med.reminderTime}", fontSize = 10.sp, color = GrayText)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                                            val quickLog = MedicationLog(
                                                medicationId = med.id,
                                                medicationName = med.name,
                                                logDate = todayStr,
                                                logTime = timeStr,
                                                status = "Taken",
                                                notes = "Quick logged from Dashboard overview"
                                            )
                                            viewModel.insertMedicationLog(quickLog)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(30.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(12.dp), tint = White)
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Take", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Quick Actions", "View All >")
            
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Medical Pass",
                        subtitle = "Share with doctors",
                        icon = Icons.Outlined.Badge,
                        iconTint = PrimaryDarkTeal,
                        bgColor = PastelGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Share.route) }
                    )
                    QuickActionCard(
                        title = "Medications",
                        subtitle = "Your medicines",
                        icon = Icons.Outlined.Medication,
                        iconTint = IconBlue,
                        bgColor = PastelBlue,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Medications.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Lab Reports",
                        subtitle = "Test & results",
                        icon = Icons.Outlined.Science,
                        iconTint = IconPurple,
                        bgColor = PastelPurple,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Documents.route) }
                    )
                    QuickActionCard(
                        title = "Medical Records",
                        subtitle = "Diagnosis history",
                        icon = Icons.Outlined.Description,
                        iconTint = IconOrange,
                        bgColor = PastelOrange,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.MedicalRecords.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Appointments",
                        subtitle = "Upcoming visits",
                        icon = Icons.Outlined.CalendarMonth,
                        iconTint = ErrorRed,
                        bgColor = PastelPink,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Visits.route) }
                    )
                    QuickActionCard(
                        title = "Daily Vitals",
                        subtitle = "BP & Heart Rate",
                        icon = Icons.Outlined.FavoriteBorder,
                        iconTint = ErrorRed,
                        bgColor = AlertLightRed,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.DailyVitals.route) }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickActionCard(
                        title = "Emergency",
                        subtitle = "Get help instantly",
                        icon = Icons.Outlined.Warning,
                        iconTint = ErrorRed,
                        bgColor = AlertLightRed,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.Emergency.route) }
                    )
                    QuickActionCard(
                        title = "Pharmacies",
                        subtitle = "Find 24x7 near you",
                        icon = Icons.Outlined.LocalPharmacy,
                        iconTint = PrimaryDarkTeal,
                        bgColor = BackgroundLightTeal,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate(Screen.PharmacyLocator.route) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SectionHeader("Recent Uploads", "View All >", onActionClick = { navController.navigate(Screen.Documents.route) })
            
            Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                val recentDocs = documents.sortedByDescending { it.id }.take(4)
                if (recentDocs.isEmpty()) {
                    Text("No documents uploaded yet.", fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(vertical = 12.dp))
                } else {
                    recentDocs.forEach { doc ->
                        val docIcon = when {
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("lab") -> Icons.Outlined.Science
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("prescription") -> Icons.Outlined.Assignment
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("vaccin") -> Icons.Outlined.Vaccines
                            else -> Icons.Outlined.Description
                        }
                        val docColor = when {
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("lab") -> IconGreen
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("prescription") -> IconBlue
                            java.util.Locale.getDefault().let { l -> doc.type.lowercase(l) }.contains("vaccin") -> IconPurple
                            else -> IconOrange
                        }
                        
                        ActivityItem(
                            icon = docIcon, 
                            iconBg = docColor, 
                            title = doc.type, 
                            subtitle = "Uploaded • ${doc.providerName}", 
                            time = doc.date
                        )
                    }
                }
            }

        } else if (selectedTab == 1) {
            // ================= Clinical Trends TabContent (AI-Extracted Vitals) =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelBlue.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IconBlue, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Secure AI Medical Extractor", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
                        Text("These metrics are automatically synthesized by Gemini AI directly from your scanned diagnostic lab reports.", fontSize = 11.sp, color = Black.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timeframe Selector Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Period:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDarkTeal, modifier = Modifier.padding(end = 4.dp))
                
                listOf("3M" to "3 Months", "6M" to "6 Months", "1Y" to "Past Year").forEach { (code, label) ->
                    val isSelected = selectedTimeframe == code
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryDarkTeal else White)
                            .border(1.dp, if (isSelected) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(12.dp))
                            .clickable { selectedTimeframe = code }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) White else PrimaryDarkTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Filter trends and calculate averages
            val filteredBP = filterVitalsByTimeframe(bpVitals, selectedTimeframe)
            val filteredSugar = filterVitalsByTimeframe(sugarVitals, selectedTimeframe)
            val filteredCholesterol = filterVitalsByTimeframe(cholesterolVitals, selectedTimeframe)

            val avgBP1 = if (filteredBP.isNotEmpty()) filteredBP.map { it.value1 }.average().toInt() else null
            val avgBP2 = if (filteredBP.isNotEmpty()) filteredBP.map { it.value2 }.average().toInt() else null
            val avgSugar = if (filteredSugar.isNotEmpty()) filteredSugar.map { it.value1 }.average().toInt() else null
            val avgChol = if (filteredCholesterol.isNotEmpty()) filteredCholesterol.map { it.value1 }.average().toInt() else null

            // Averages Summary Dashboard Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: BP Average
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Favorite, contentDescription = null, tint = IconPurple, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("BP Avg", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (avgBP1 != null && avgBP2 != null) "$avgBP1/$avgBP2" else "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Black
                        )
                        Text("mmHg", fontSize = 9.sp, color = GrayText)
                    }
                }

                // Card 2: Sugar Average
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Sugar Avg", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (avgSugar != null) "$avgSugar" else "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Black
                        )
                        Text("mg/dL", fontSize = 9.sp, color = GrayText)
                    }
                }

                // Card 3: Cholesterol Average
                Card(
                     modifier = Modifier.weight(1f),
                     colors = CardDefaults.cardColors(containerColor = White),
                     elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                     shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Speed, contentDescription = null, tint = IconOrange, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Chol. Avg", fontSize = 10.sp, color = GrayText, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (avgChol != null) "$avgChol" else "--",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Black
                        )
                        Text("mg/dL", fontSize = 9.sp, color = GrayText)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Charts rendered with filtered data
            ClinicalTrendChart(
                title = "Clinical Blood Pressure (mmHg)",
                titleSi = "සායනික රුධිර පීඩනය (mmHg)",
                titleTa = "மருத்துவ இரத்த அழுத்தம் (mmHg)",
                vitals = filteredBP,
                primaryColor = IconPurple,
                secondaryColor = IconGreen,
                isDouble = true,
                unit = "mmHg"
            )

            ClinicalTrendChart(
                title = "Blood Sugar / Fasting Glucose (mg/dL)",
                titleSi = "රුධිර සීනි / නිරාහාර ග්ලූකෝස් (mg/dL)",
                titleTa = "இரத்த சர்க்கரை / உண்ணாவிரத குளுக்கோஸ் (mg/dL)",
                vitals = filteredSugar,
                primaryColor = ErrorRed,
                unit = "mg/dL"
            )

            ClinicalTrendChart(
                title = "Total Cholesterol (mg/dL)",
                titleSi = "සම්පූර්ණ කොලෙස්ටරෝල් (mg/dL)",
                titleTa = "மொத்த கொலஸ்ட்ரால் (mg/dL)",
                vitals = filteredCholesterol,
                primaryColor = IconOrange,
                unit = "mg/dL"
            )

            // Dynamic Clinical Overview Section
            ClinicalOverviewSection(viewModel = viewModel)

            Spacer(modifier = Modifier.height(32.dp))

        } else if (selectedTab == 2) {
            // ================= Current Status TabContent (Patient's Home Devices) =================
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = PastelGreen.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhoneAndroid, contentDescription = null, tint = IconGreen, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Personal Home Device Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
                        Text("Track measurements taken with your home blood pressure monitors, glucose fingers-pricks, and fitness rings.", fontSize = 11.sp, color = Black.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Latest logged status row
            val latestLog = dailyVitalsList.lastOrNull()
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Blood Pressure card
                Card(
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PastelPurple, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Favorite, contentDescription = null, tint = IconPurple)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("BP Reading (Home Logs)", fontSize = 12.sp, color = GrayText)
                            Text(
                                text = if (latestLog != null) "${latestLog.systolic}/${latestLog.diastolic} mmHg" else "120/80 mmHg",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Black
                            )
                        }
                        val sys = latestLog?.systolic ?: 120
                        val normal = sys < 120
                        Text(
                            text = if (normal) "Normal" else "Elevated",
                            color = if (normal) IconGreen else IconOrange,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(if (normal) PastelGreen else PastelOrange, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Glucose card
                Card(
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PastelPink, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.WaterDrop, contentDescription = null, tint = ErrorRed)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Glucose Level (Home Logs)", fontSize = 12.sp, color = GrayText)
                            val sugarVal = sugarVitals.lastOrNull()?.value1 ?: 95
                            Text(
                                text = "${sugarVal} mg/dL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Black
                            )
                        }
                        val bs = sugarVitals.lastOrNull()?.value1 ?: 95
                        val bsNormal = bs < 110
                        Text(
                            text = if (bsNormal) "Normal" else "High",
                            color = if (bsNormal) IconGreen else ErrorRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(if (bsNormal) PastelGreen else PastelPink, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                // Heart rate card
                Card(
                    colors = CardDefaults.cardColors(containerColor = White),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().shadow(1.dp, RoundedCornerShape(16.dp))
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(PastelOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.DeviceThermostat, contentDescription = null, tint = IconOrange)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Pulse / Heart Rate", fontSize = 12.sp, color = GrayText)
                            Text(
                                text = if (latestLog != null) "${latestLog.heartRate} bpm" else "72 bpm",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Black
                            )
                        }
                        Text(
                            text = "Healthy",
                            color = IconGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .background(PastelGreen, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Rapid Input Logger Panel
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .shadow(4.dp, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Record Today's Home Readings", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = sysInput,
                            onValueChange = { sysInput = it },
                            label = { Text("Systolic", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = diaInput,
                            onValueChange = { diaInput = it },
                            label = { Text("Diastolic", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = bsInput,
                            onValueChange = { bsInput = it },
                            label = { Text("Sugar (mg/dL)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = hrInput,
                            onValueChange = { hrInput = it },
                            label = { Text("Pulse (bpm)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val sys = sysInput.toIntOrNull() ?: 120
                            val dia = diaInput.toIntOrNull() ?: 80
                            val hr = hrInput.toIntOrNull() ?: 72
                            val bs = bsInput.toIntOrNull() ?: 0
                            
                            viewModel.insertDailyVitals(
                                com.example.data.model.DailyVitals(
                                    date = logDate,
                                    systolic = sys,
                                    diastolic = dia,
                                    heartRate = hr
                                )
                            )
                            viewModel.insertVitalSign(
                                com.example.data.model.VitalSign(
                                    type = "Blood Pressure",
                                    value1 = sys,
                                    value2 = dia,
                                    date = logDate
                                )
                            )
                            if (bs > 0) {
                                viewModel.insertVitalSign(
                                    com.example.data.model.VitalSign(
                                        type = "Blood Sugar",
                                        value1 = bs,
                                        value2 = 0,
                                        date = logDate
                                    )
                                )
                            }
                            // Clear inputs
                            sysInput = ""
                            diaInput = ""
                            hrInput = ""
                            bsInput = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Home Measurement", fontSize = 13.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        } else if (selectedTab == 3) {
            // ================= Arogya Hub TabContent =================
            // 1. OFFLINE PRIVACY & FAMILY PROFILE MANAGER
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(PrimaryDarkTeal.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HealthAndSafety,
                            contentDescription = null,
                            tint = PrimaryDarkTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            TrilingualText("Offline Secure Mode", "දේශීය දත්ත සුරක්ෂිතතාවය", "ஆஃப்லைன் பாதுகாப்பு பயன்முறை", color = PrimaryDarkTeal, scale = 0.9f)
                            Text(
                                "0 MB mobile data consumed. Secured locally on this device.",
                                fontSize = 10.sp,
                                color = GrayText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Family Profiles",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Black
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                newProfileName = ""
                                newProfileAge = ""
                                showAddProfileDialog = true
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Member", fontSize = 12.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        familyMembers.forEach { member ->
                            val isSelected = activeMemberId == member.id
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryDarkTeal else LightGrayBackground
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else BackgroundLightTeal),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.selectActiveMember(member.id) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(if (isSelected) White else PrimaryDarkTeal.copy(alpha = 0.1f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = member.name.take(1).uppercase(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = if (isSelected) PrimaryDarkTeal else PrimaryDarkTeal
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = member.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isSelected) White else Black,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "${member.relation} • Age ${member.age}",
                                        fontSize = 10.sp,
                                        color = if (isSelected) White.copy(alpha = 0.8f) else GrayText,
                                        maxLines = 1
                                    )
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .background(if (isSelected) White.copy(alpha = 0.2f) else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(member.bloodGroup, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (isSelected) White else PrimaryDarkTeal)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. AYURVEDIC & TRADITIONAL CURE SOLUTIONS (Out-of-box angle!)
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(PastelGreen, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Spa, contentDescription = null, tint = IconGreen, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            TrilingualText("Traditional Home Remedies", "දේශීය වෛද්‍ය හා අත්බෙහෙත්", "பாரம்பரிய வீட்டு வைத்தியம்", color = PrimaryDarkTeal, scale = 1.0f)
                            Text("Integrates Ayurveda & Homeopathy alongside Allopathic", fontSize = 10.sp, color = GrayText)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // horizontal mini tabs
                    val ailments = listOf("Cough/Cold", "Gastritis", "Joint Pain", "Allergy")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ailments.forEachIndexed { index, name ->
                            val isSel = selectedAilmentIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) PrimaryDarkTeal else LightGrayBackground)
                                    .clickable { selectedAilmentIndex = index }
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSel) White else GrayText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val selectedAilmentDetails = when (selectedAilmentIndex) {
                        0 -> Triple(
                            "Warm Coriander decoction (කොත්තමල්ලි), Ginger tea with wild honey, Samahan or boiled Venivel infusions twice daily.",
                            "Bryonia 30 / Aconitum Napellus 30 (effective for dry hacking cough with sudden fever development).",
                            "Paracetamol 500mg tablet (1-2 tabs every 6 hours), steam inhalation with menthol drops."
                        )
                        1 -> Triple(
                            "Warm barley water, Gotu Kola herbal congee, fresh king coconut water, avoiding highly spiced curries.",
                            "Nux Vomica 30 (helps with burning gastritis, bloating, acid reflux, or post-meal indigestion).",
                            "Omeprazole 20mg capsule 30 minutes before breakfast, chewable antacid gel pills as symptoms arise."
                        )
                        2 -> Triple(
                            "Applying Siddharthalepa herbal balm or warm Neelyadi oil / Pinda taila massage on stiff joints.",
                            "Rhus Toxicodendron 30 (recommended for stiffness that improves with continuous light motion).",
                            "Ibuprofen 400mg tablet after food, complete physical rest, cold compress gel packs."
                        )
                        else -> Triple(
                            "Applying neem (කොහොඹ) and raw turmeric paste on rash, washing with boiled curry leaf water.",
                            "Apis Mellifica 30 (recommended for raised hives or red skin allergies with stinging heat).",
                            "Cetirizine 10mg tablet once daily at night, thin application of soothing calamine lotion."
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightGrayBackground, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row {
                            Text("🌿 ", fontSize = 14.sp)
                            Column {
                                Text("Ayurvedic Remedies (දේශීය ප්‍රතිකාර)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IconGreen)
                                Text(selectedAilmentDetails.first, fontSize = 11.sp, color = Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            Text("🧪 ", fontSize = 14.sp)
                            Column {
                                Text("Homeopathic Alternative", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IconPurple)
                                Text(selectedAilmentDetails.second, fontSize = 11.sp, color = Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row {
                            Text("⚕️ ", fontSize = 14.sp)
                            Column {
                                Text("Allopathic Standard (බටහිර වෛද්‍ය)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryDarkTeal)
                                Text(selectedAilmentDetails.third, fontSize = 11.sp, color = Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. CONSULTATION & MEDICATION EXPENSE LOGGER
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(PastelPink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Receipt, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                TrilingualText("Health Expense Logs", "සෞඛ්‍ය වියදම්", "சுகாதார செலவுகள்", color = PrimaryDarkTeal, scale = 1.0f)
                                Text("Offline tracker - absolute pricing privacy", fontSize = 10.sp, color = GrayText)
                            }
                        }
                        
                        Button(
                            onClick = { 
                                newExpenseDesc = ""
                                newExpenseAmount = ""
                                showAddExpenseDialog = true 
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier.height(28.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, tint = White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Log Costs", fontSize = 10.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val totalSpent = expenses.sumOf { it.amount.toDouble() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LightGrayBackground, RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Medical Expenditure", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GrayText)
                        Text("LKR ${String.format("%,.2f", totalSpent)}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = ErrorRed)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Last 3 logged expenses
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        expenses.takeLast(3).reversed().forEach { exp ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(
                                                when (exp.category) {
                                                    "Doctor" -> IconOrange
                                                    "Medicine" -> IconGreen
                                                    "Lab" -> IconPurple
                                                    else -> GrayText
                                                }, CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(exp.description, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Black)
                                        Text("${exp.category} • ${exp.profileName} on ${exp.date}", fontSize = 10.sp, color = GrayText)
                                    }
                                }
                                Text("LKR ${String.format("%,.2f", exp.amount.toDouble())}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { navController.navigate("expenses") },
                        colors = ButtonDefaults.buttonColors(containerColor = BackgroundLightTeal),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Assessment, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        TrilingualText(
                            english = "Manage & Analyze Expenses ➔",
                            sinhala = "වියදම් විශ්ලේෂණය සහ කළමනාකරණය ➔",
                            tamil = "செலவு மேலாண்மை மற்றும் பகுப்பாய்வு ➔",
                            color = PrimaryDarkTeal,
                            scale = 0.85f
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BackgroundLightTeal.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // MONETIZATION: pharmacy price comparative engine
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Compare, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Local Pharmacy Price Comparer", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDarkTeal)
                        Spacer(modifier = Modifier.weight(1f))
                        if (isPremiumUser) {
                            Box(
                                modifier = Modifier
                                    .background(IconGreen, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("PREMIUM UNLOCKED", fontSize = 8.sp, color = White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Compare costs of daily medicine packages across major Sri Lankan pharmacies.", fontSize = 11.sp, color = GrayText)

                    Spacer(modifier = Modifier.height(10.dp))

                    val drugOptions = listOf("Paracetamol 500mg (10 Tabs)", "Amoxicillin 250mg (15 Caps)", "Metformin 500mg (30 Tabs)", "Atorvastatin 10mg (30 Tabs)")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        drugOptions.forEachIndexed { dIndex, dName ->
                            val isD = selectedDrugIndex == dIndex
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isD) PrimaryDarkTeal else LightGrayBackground)
                                    .clickable { selectedDrugIndex = dIndex }
                                    .padding(vertical = 6.dp, horizontal = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    dName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isD) White else GrayText
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val prices = when (selectedDrugIndex) {
                        0 -> Triple(150.0, 180.0, 80.0) // Union Chemists, Healthguard, Osu Sala
                        1 -> Triple(450.0, 480.0, 320.0)
                        2 -> Triple(600.0, 650.0, 450.0)
                        else -> Triple(1200.0, 1250.0, 950.0)
                    }

                    if (isPremiumUser || selectedDrugIndex == 0) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(LightGrayBackground, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Text(drugOptions[selectedDrugIndex], fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Black)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Osu Sala (State Chemist)", fontSize = 11.sp, color = Black)
                                Text("LKR ${String.format("%.2f", prices.third)} (Cheapest! 🌟)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = IconGreen)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Union Chemists", fontSize = 11.sp, color = GrayText)
                                Text("LKR ${String.format("%.2f", prices.first)}", fontSize = 11.sp, color = Black)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Healthguard", fontSize = 11.sp, color = GrayText)
                                Text("LKR ${String.format("%.2f", prices.second)}", fontSize = 11.sp, color = Black)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PastelOrange.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Lock, contentDescription = null, tint = IconOrange, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Compare prices on Metformin, Amoxicillin and 500+ medicines with Premium Plan!", fontSize = 10.sp, color = Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(6.dp))
                                Button(
                                    onClick = { showPremiumSubscriptionDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = IconOrange),
                                    contentPadding = PaddingValues(horizontal = 12.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Text("Simulate Premium Unlock", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. DOCTOR & CLINIC LOCATOR WITH RATINGS
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(PastelOrange, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = IconOrange, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            TrilingualText("Clinic & Healers Directory", "වෛද්‍ය සායන සේවා", "மருத்துவமனை லொக்கேட்டர்", color = PrimaryDarkTeal, scale = 1.0f)
                            Text("Find open Ayurvedic, Homeopathy, & Allopathic clinics", fontSize = 10.sp, color = GrayText)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val clinics = listOf(
                        ClinicInfo("Dr. Nimal Fernando", "Cardiology Specialist", "General Hospital, Colombo", "4.8 ★ (120 reviews)"),
                        ClinicInfo("Vaidya Siriwardena", "Ayurvedic Pulse Diagnosis", "Siri Ayurveda Shala, Gampaha", "4.9 ★ (86 reviews)"),
                        ClinicInfo("Dr. S. Sivasamy", "Homeopathic General Care", "Jaffna Homeopathy Medical, Jaffna", "4.7 ★ (34 reviews)"),
                        ClinicInfo("Dr. Amara Perera", "Allopathic Pediatrics", "Galle Children Clinic, Galle", "4.6 ★ (52 reviews)")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        clinics.forEach { clinic ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightGrayBackground, RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(clinic.first, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Black)
                                    Text("${clinic.second} • ${clinic.third}", fontSize = 10.sp, color = Black.copy(alpha = 0.7f))
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                        Icon(Icons.Filled.Star, contentDescription = null, tint = IconOrange, modifier = Modifier.size(11.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(clinic.fourth, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PrimaryDarkTeal)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(PrimaryDarkTeal, RoundedCornerShape(8.dp))
                                        .clickable {
                                            android.widget.Toast.makeText(context, "Call initiating to ${clinic.first}...", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Filled.Phone, contentDescription = "Call", tint = White, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. CHILDHOOD & ADULT VACCINES BOOKLET + ACTIVE SYMPTOM BOOKLET
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .shadow(4.dp, RoundedCornerShape(24.dp))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(PastelPurple, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Healing, contentDescription = null, tint = IconPurple, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                TrilingualText("Symptom & Vaccine Guides", "රෝග ලක්ෂණ සහ එන්නත් පොත", "அறிகுறி மற்றும் தடுப்பூசி", color = PrimaryDarkTeal, scale = 1.0f)
                                Text("Offline tracker - dynamic digital health logbook", fontSize = 10.sp, color = GrayText)
                            }
                        }
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Button(
                                onClick = { showAddSymptomDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Symptom", fontSize = 9.sp)
                            }
                            Button(
                                onClick = { showAddVaccineDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                modifier = Modifier.height(26.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("+ Vaccine", fontSize = 9.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Active Tracked Symptoms:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Black)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        symptomLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightGrayBackground, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (log.status == "Active") ErrorRed else IconGreen, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("${log.name} (Severity: ${log.severity}/10)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Black)
                                }
                                Text("${log.status} • ${log.date}", fontSize = 10.sp, color = GrayText)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { navController.navigate("symptoms") },
                        colors = ButtonDefaults.buttonColors(containerColor = BackgroundLightTeal),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        TrilingualText(
                            english = "Open Symptom Journal ➔",
                            sinhala = "පූර්ණ රෝග ලක්ෂණ සටහන ➔",
                            tamil = "அறிகுறி இதழைத் திறக்கவும் ➔",
                            color = PrimaryDarkTeal,
                            scale = 0.8f
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Childhood & Seasonal Vaccination Booklet:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Black)
                    Spacer(modifier = Modifier.height(6.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        vaccineLogs.forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(LightGrayBackground, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(log.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Black)
                                    Text("Logged member: ${log.memberName}", fontSize = 9.sp, color = GrayText)
                                }
                                Box(
                                    modifier = Modifier
                                        .background(if (log.status == "Completed") PastelGreen else PastelOrange, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(log.status, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (log.status == "Completed") IconGreen else IconOrange)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ================= Interactive Overlay Modal Dialogs for Arogya Hub =================
        if (showAddProfileDialog) {
            AlertDialog(
                onDismissRequest = { showAddProfileDialog = false },
                title = { Text("Family Member Details", fontWeight = FontWeight.Bold, color = PrimaryDarkTeal) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newProfileName,
                            onValueChange = { newProfileName = it },
                            label = { Text("Full Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newProfileAge,
                            onValueChange = { newProfileAge = it },
                            label = { Text("Age (years)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Relationship:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Wife", "Husband", "Son", "Father", "Mother").forEach { rel ->
                                val isR = newProfileRelation == rel
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isR) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isR) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newProfileRelation = rel }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(rel, fontSize = 9.sp, color = if (isR) White else Black)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Blood Group:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("A+", "B+", "O+", "AB+").forEach { bg ->
                                val isB = newProfileBloodGroup == bg
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isB) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isB) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newProfileBloodGroup = bg }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(bg, fontSize = 10.sp, color = if (isB) White else Black)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        onClick = {
                            if (newProfileName.isNotBlank() && newProfileAge.isNotBlank()) {
                                val added = viewModel.addFamilyMember(newProfileName, newProfileRelation, newProfileAge, newProfileBloodGroup)
                                if (!added) {
                                    showPremiumSubscriptionDialog = true
                                } else {
                                    android.widget.Toast.makeText(context, "Added family member successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                showAddProfileDialog = false
                            }
                        }
                    ) {
                        Text("Save Profile")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddProfileDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddExpenseDialog) {
            AlertDialog(
                onDismissRequest = { showAddExpenseDialog = false },
                title = { Text("Log Medical Expense", fontWeight = FontWeight.Bold, color = PrimaryDarkTeal) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newExpenseDesc,
                            onValueChange = { newExpenseDesc = it },
                            label = { Text("Expense Details (e.g., Blood Test, Consultation)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newExpenseAmount,
                            onValueChange = { newExpenseAmount = it },
                            label = { Text("Expenditure Amount (LKR)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Category:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Doctor", "Medicine", "Lab", "Other").forEach { cat ->
                                val isC = newExpenseCategory == cat
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isC) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isC) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newExpenseCategory = cat }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(cat, fontSize = 9.sp, color = if (isC) White else Black)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Linked Family Profile:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            familyMembers.map { it.name.split(" ").first() }.forEach { fn ->
                                val isP = newExpenseProfile == fn
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isP) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isP) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newExpenseProfile = fn }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(fn, fontSize = 9.sp, color = if (isP) White else Black)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        onClick = {
                            val amt = newExpenseAmount.toDoubleOrNull()
                            if (newExpenseDesc.isNotBlank() && amt != null) {
                                viewModel.addExpense(newExpenseDesc, amt, newExpenseCategory, newExpenseProfile)
                                android.widget.Toast.makeText(context, "Logged medical expense offline!", android.widget.Toast.LENGTH_SHORT).show()
                                showAddExpenseDialog = false
                            }
                        }
                    ) {
                        Text("Log Cost")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddExpenseDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddSymptomDialog) {
            AlertDialog(
                onDismissRequest = { showAddSymptomDialog = false },
                title = { Text("Log Current Symptom", fontWeight = FontWeight.Bold, color = PrimaryDarkTeal) },
                text = {
                    Column {
                        listOf("Fever / Influenza", "Indigestion & Bloating", "Cough & Sore Throat", "Joint/Muscle Ache", "Skin Rash / Allergy").forEach { sym ->
                            val isS = newSymptomName == sym
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isS) PrimaryDarkTeal.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { newSymptomName = sym }
                                    .padding(10.dp)
                            ) {
                                Text(sym, fontSize = 12.sp, fontWeight = if (isS) FontWeight.Bold else FontWeight.Normal, color = if (isS) PrimaryDarkTeal else Black)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Active Severity level: (Current: ${newSymptomSeverity}/10)", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Slider(
                            value = newSymptomSeverity.toFloat(),
                            onValueChange = { newSymptomSeverity = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8
                        )
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        onClick = {
                            viewModel.addSymptomLog(newSymptomName, newSymptomSeverity, "Active")
                            android.widget.Toast.makeText(context, "Logged symptom offline!", android.widget.Toast.LENGTH_SHORT).show()
                            showAddSymptomDialog = false
                        }
                    ) {
                        Text("Log Symptom")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddSymptomDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showAddVaccineDialog) {
            AlertDialog(
                onDismissRequest = { showAddVaccineDialog = false },
                title = { Text("Log Vaccination Card", fontWeight = FontWeight.Bold, color = PrimaryDarkTeal) },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newVaccineName,
                            onValueChange = { newVaccineName = it },
                            label = { Text("Vaccine Name (e.g. MMR, Hep B)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Linked Profile Member:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            familyMembers.map { it.name.split(" ").first() }.forEach { fn ->
                                val isV = newVaccineMember == fn
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isV) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isV) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newVaccineMember = fn }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(fn, fontSize = 9.sp, color = if (isV) White else Black)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Vaccination Status:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 4.dp)) {
                            listOf("Completed", "Scheduled").forEach { stat ->
                                val isSt = newVaccineStatus == stat
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, if (isSt) PrimaryDarkTeal else BackgroundLightTeal, RoundedCornerShape(6.dp))
                                        .background(if (isSt) PrimaryDarkTeal else Color.Transparent)
                                        .clickable { newVaccineStatus = stat }
                                        .padding(vertical = 4.dp, horizontal = 8.dp)
                                ) {
                                    Text(stat, fontSize = 9.sp, color = if (isSt) White else Black)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        onClick = {
                            if (newVaccineName.isNotBlank()) {
                                viewModel.addVaccineLog(newVaccineName, newVaccineDate, newVaccineStatus, newVaccineMember)
                                android.widget.Toast.makeText(context, "Logged vaccination!", android.widget.Toast.LENGTH_SHORT).show()
                                showAddVaccineDialog = false
                            }
                        }
                    ) {
                        Text("Save Result")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddVaccineDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPremiumSubscriptionDialog) {
            AlertDialog(
                onDismissRequest = { showPremiumSubscriptionDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CardMembership, contentDescription = null, tint = IconOrange, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Arogya Premium Plan", color = IconOrange, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text("Unlock unlimited medical trackers and family sharing:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("✅ Unlimited Family Profiles (Free capped at 3)", fontSize = 12.sp, color = Black)
                        Text("✅ Price Comparison Engine over 500+ local meds", fontSize = 12.sp, color = Black)
                        Text("✅ Export Rich Diagnostics Reports to PDF/Excel", fontSize = 12.sp, color = Black)
                        Text("✅ Multi-device offline backup & secure cloud vault", fontSize = 12.sp, color = Black)
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PastelOrange, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Text("Simulated checkout: click below to toggle Premium status instantly!", fontSize = 10.sp, color = Black, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        colors = ButtonDefaults.buttonColors(containerColor = IconOrange),
                        onClick = {
                            viewModel.togglePremiumStatus()
                            android.widget.Toast.makeText(context, if (!isPremiumUser) "Premium Activated! Welcome to Arogya Plus!" else "Downgraded membership.", android.widget.Toast.LENGTH_SHORT).show()
                            showPremiumSubscriptionDialog = false
                        }
                    ) {
                        Text(if (isPremiumUser) "Mock Downgrade Plan" else "Mock Activate Life Plan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPremiumSubscriptionDialog = false }) {
                        Text("Maybe Later")
                    }
                }
            )
        }
    }
}

@Composable
fun SnapshotItem(icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, title: String, value: String, valueColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, fontSize = 12.sp, color = GrayText)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun MiniStatCard(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, bgColor: Color) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = androidx.compose.foundation.BorderStroke(1.dp, BackgroundLightTeal),
        modifier = Modifier.width(62.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = iconTint)
            }
            Text(label, fontSize = 8.sp, color = GrayText, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
fun SectionHeader(title: String, actionText: String, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Black)
        Text(
            actionText, 
            fontSize = 14.sp, 
            color = PrimaryDarkTeal, 
            fontWeight = FontWeight.Medium,
            modifier = if (onActionClick != null) Modifier.clickable { onActionClick() }.padding(4.dp) else Modifier
        )
    }
}

@Composable
fun QuickActionCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, iconTint: Color, bgColor: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        onClick = onClick,
        modifier = modifier.height(80.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
                Text(subtitle, fontSize = 11.sp, color = GrayText)
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun ActivityItem(icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, title: String, subtitle: String, time: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
            Text(subtitle, fontSize = 12.sp, color = GrayText)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time, fontSize = 12.sp, color = GrayText)
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = GrayText, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = BackgroundLightTeal, thickness = 1.dp)
}

@Composable
fun ReadingClassificationBadge(value1: Int, value2: Int, title: String) {
    val (text, color, icon) = remember(value1, value2, title) {
        if (title.contains("Blood Pressure", ignoreCase = true)) {
            when {
                value1 < 120 && value2 < 80 -> Triple("Optimal Normal", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
                value1 in 120..129 && value2 < 80 -> Triple("Elevated (Watch)", Color(0xFFE65100), Icons.Filled.Warning)
                else -> Triple("High (Consult)", Color(0xFFC62828), Icons.Filled.Error)
            }
        } else if (title.contains("Blood Sugar", ignoreCase = true)) {
            when {
                value1 < 100 -> Triple("Optimal Fasting", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
                value1 in 100..125 -> Triple("Prediabetes Status", Color(0xFFE65100), Icons.Filled.Warning)
                else -> Triple("High Sugar Alert", Color(0xFFC62828), Icons.Filled.Error)
            }
        } else { // Cholesterol
            when {
                value1 < 200 -> Triple("Desirable Level", Color(0xFF2E7D32), Icons.Filled.CheckCircle)
                value1 in 200..239 -> Triple("Borderline High", Color(0xFFE65100), Icons.Filled.Warning)
                else -> Triple("High Risk Alert", Color(0xFFC62828), Icons.Filled.Error)
            }
        }
    }
    
    Row(
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ClinicalTrendChart(
    title: String,
    titleSi: String = title,
    titleTa: String = title,
    vitals: List<com.example.data.model.VitalSign>,
    primaryColor: Color,
    secondaryColor: Color = primaryColor,
    isDouble: Boolean = false,
    unit: String = ""
) {
    var selectedPointIndex by remember(vitals) { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    TrilingualText(
                        english = title,
                        sinhala = titleSi,
                        tamil = titleTa,
                        scale = 0.875f,
                        color = Black
                    )
                    TrilingualText(
                        english = "Tap graph to inspect precise readings",
                        sinhala = "නිශ්චිත පරික්ෂා කිරීමට ප්‍රස්ථාරය තට්ටු කරන්න",
                        tamil = "துல்லியமான அளவீடுகளை ஆய்வு செய்ய வரைபடத்தைத் தட்டவும்",
                        scale = 0.625f,
                        color = GrayText
                    )
                }
                if (vitals.isNotEmpty()) {
                    val latest = vitals.last()
                    val displayValue = if (isDouble) {
                        "${latest.value1}/${latest.value2}"
                    } else {
                        "${latest.value1}"
                    }
                    val latestEn = "Latest: $displayValue $unit"
                    val latestSi = "නවතම: $displayValue $unit"
                    val latestTa = "சமீபத்தியது: $displayValue $unit"
                    TrilingualText(
                        english = latestEn,
                        sinhala = latestSi,
                        tamil = latestTa,
                        scale = 0.6875f,
                        color = primaryColor,
                        modifier = Modifier
                            .background(primaryColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (vitals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No clinical data loaded on this selected range", color = GrayText, fontSize = 12.sp)
                }
            } else {
                val displayPoints = vitals
                
                val maxVal = if (isDouble) {
                    displayPoints.maxOf { it.value1.coerceAtLeast(it.value2) }.toFloat()
                } else {
                    displayPoints.maxOf { it.value1 }.toFloat()
                }
                
                val minVal = if (isDouble) {
                    displayPoints.minOf { it.value1.coerceAtMost(it.value2) }.toFloat()
                } else {
                    displayPoints.minOf { it.value1 }.toFloat()
                }
                
                val yMax = (maxVal * 1.15f).coerceAtLeast(100f)
                val yMin = (minVal * 0.85f).coerceAtLeast(0f)
                val yRange = yMax - yMin
                
                val leftPaddingPx = with(density) { 45.dp.toPx() }
                val rightPaddingPx = with(density) { 15.dp.toPx() }
                val topPaddingPx = with(density) { 15.dp.toPx() }
                val bottomPaddingPx = with(density) { 30.dp.toPx() }

                Canvas(
                    modifier = Modifier
                         .fillMaxWidth()
                         .height(160.dp)
                         .pointerInput(displayPoints) {
                             detectTapGestures { offset ->
                                 val pointCount = displayPoints.size
                                 if (pointCount > 0) {
                                     val graphWidth = size.width - leftPaddingPx - rightPaddingPx
                                     val segmentWidth = if (pointCount > 1) graphWidth / (pointCount - 1) else graphWidth
                                     var closestIdx = -1
                                     var minDiff = Float.MAX_VALUE
                                     for (i in 0 until pointCount) {
                                         val px = leftPaddingPx + i * segmentWidth
                                         val diff = kotlin.math.abs(px - offset.x)
                                         if (diff < minDiff) {
                                             minDiff = diff
                                             closestIdx = i
                                         }
                                     }
                                     if (closestIdx != -1 && minDiff < segmentWidth * 0.5f) {
                                         selectedPointIndex = closestIdx
                                     }
                                 }
                             }
                         }
                ) {
                    val graphWidth = size.width - leftPaddingPx - rightPaddingPx
                    val graphHeight = size.height - topPaddingPx - bottomPaddingPx
                    
                    val pointCount = displayPoints.size
                    val segmentWidth = if (pointCount > 1) graphWidth / (pointCount - 1) else graphWidth
                    
                    // 1. Draw Shaded Health Zones
                    var bandMin = 0f
                    var bandMax = 0f
                    if (title.contains("Blood Pressure", ignoreCase = true)) {
                        bandMin = 90f
                        bandMax = 120f
                    } else if (title.contains("Blood Sugar", ignoreCase = true)) {
                        bandMin = 70f
                        bandMax = 100f
                    } else if (title.contains("Cholesterol", ignoreCase = true)) {
                        bandMin = 130f
                        bandMax = 200f
                    }
                    if (bandMax > bandMin && yRange > 0f) {
                        val bMinCoerced = bandMin.coerceIn(yMin, yMax)
                        val bMaxCoerced = bandMax.coerceIn(yMin, yMax)
                        val fracMax = (bMaxCoerced - yMin) / yRange
                        val fracMin = (bMinCoerced - yMin) / yRange
                        val yMaxPos = topPaddingPx + graphHeight - (fracMax * graphHeight)
                        val yMinPos = topPaddingPx + graphHeight - (fracMin * graphHeight)
                        if (yMinPos > yMaxPos) {
                            drawRect(
                                color = Color(0xFF4CAF50).copy(alpha = 0.08f),
                                topLeft = Offset(leftPaddingPx, yMaxPos),
                                size = androidx.compose.ui.geometry.Size(graphWidth, yMinPos - yMaxPos)
                            )
                        }
                    }

                    // 2. Draw Y-Gridlines & Labels
                    val gridLines = 3
                    for (i in 0..gridLines) {
                        val fraction = i.toFloat() / gridLines
                        val y = topPaddingPx + graphHeight * (1f - fraction)
                        drawLine(
                            color = BackgroundLightTeal,
                            start = Offset(leftPaddingPx, y),
                            end = Offset(size.width - rightPaddingPx, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        
                        val gridValue = yMin + fraction * yRange
                        val labelText = gridValue.toInt().toString()
                        drawText(
                            textMeasurer = textMeasurer,
                            text = labelText,
                            style = TextStyle(
                                color = GrayText,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            topLeft = Offset(leftPaddingPx - 38.dp.toPx(), y - 7.dp.toPx())
                        )
                    }
                    
                    val linePath = Path()
                    val areaPath = Path()
                    
                    displayPoints.forEachIndexed { idx, p ->
                        val x = leftPaddingPx + idx * segmentWidth
                        val yFrac = if (yRange > 0f) (p.value1 - yMin) / yRange else 0.5f
                        val y = topPaddingPx + graphHeight - (yFrac * graphHeight)
                        
                        if (idx == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, topPaddingPx + graphHeight)
                            areaPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            areaPath.lineTo(x, y)
                        }
                        if (idx == pointCount - 1) {
                            areaPath.lineTo(x, topPaddingPx + graphHeight)
                            areaPath.close()
                        }
                    }
                    
                    if (!isDouble && pointCount > 1) {
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                                startY = topPaddingPx,
                                endY = topPaddingPx + graphHeight
                            )
                        )
                    }
                    
                    if (pointCount > 1) {
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    
                    if (isDouble) {
                        val linePath2 = Path()
                        displayPoints.forEachIndexed { idx, p ->
                            val x = leftPaddingPx + idx * segmentWidth
                            val yFrac2 = if (yRange > 0f) (p.value2 - yMin) / yRange else 0.5f
                            val y2 = topPaddingPx + graphHeight - (yFrac2 * graphHeight)
                            
                            if (idx == 0) {
                                linePath2.moveTo(x, y2)
                            } else {
                                linePath2.lineTo(x, y2)
                            }
                        }
                        if (pointCount > 1) {
                            drawPath(
                                path = linePath2,
                                color = secondaryColor,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }
                    
                    // 3. Draw Points & Cursors
                    displayPoints.forEachIndexed { idx, p ->
                        val x = leftPaddingPx + idx * segmentWidth
                        val yFrac = if (yRange > 0f) (p.value1 - yMin) / yRange else 0.5f
                        val y = topPaddingPx + graphHeight - (yFrac * graphHeight)
                        
                        val isSelected = (idx == selectedPointIndex)
                        val radiusMultiplier = if (isSelected) 1.5f else 1.0f
                        
                        drawCircle(
                            color = primaryColor.copy(alpha = 0.2f),
                            radius = 6.dp.toPx() * radiusMultiplier,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx() * radiusMultiplier,
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = White,
                            radius = 2.dp.toPx() * radiusMultiplier,
                            center = Offset(x, y)
                        )
                        
                        if (isDouble) {
                            val yFrac2 = if (yRange > 0f) (p.value2 - yMin) / yRange else 0.5f
                            val y2 = topPaddingPx + graphHeight - (yFrac2 * graphHeight)
                            
                            drawCircle(
                                color = secondaryColor.copy(alpha = 0.2f),
                                radius = 6.dp.toPx() * radiusMultiplier,
                                center = Offset(x, y2)
                            )
                            drawCircle(
                                color = secondaryColor,
                                radius = 4.dp.toPx() * radiusMultiplier,
                                center = Offset(x, y2)
                            )
                            drawCircle(
                                color = White,
                                radius = 2.dp.toPx() * radiusMultiplier,
                                center = Offset(x, y2)
                            )
                        }

                        if (isSelected) {
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.7f),
                                start = Offset(x, topPaddingPx),
                                end = Offset(x, topPaddingPx + graphHeight),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(10.dp))
                
                // Bottom X-axis Dates Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 45.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val firstDate = formatLabelDate(displayPoints.first().date)
                    val lastDate = formatLabelDate(displayPoints.last().date)
                    Text(firstDate, fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold)
                    if (displayPoints.size > 2) {
                        val midIndex = displayPoints.size / 2
                        val midDate = formatLabelDate(displayPoints[midIndex].date)
                        Text(midDate, fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold)
                    }
                    Text(lastDate, fontSize = 9.sp, color = GrayText, fontWeight = FontWeight.Bold)
                }

                // 4. Detailed Reading Inspector Card
                selectedPointIndex?.let { idx ->
                    if (idx in displayPoints.indices) {
                        val p = displayPoints[idx]
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = LightGrayBackground.copy(alpha = 0.7f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Inspected Reading:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = GrayText
                                    )
                                    val formattedDateStr = try {
                                        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.US)
                                        val d = parser.parse(p.date)
                                        if (d != null) formatter.format(d) else p.date
                                    } catch(e: Exception) {
                                        p.date
                                    }
                                    Text(formattedDateStr, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Black)
                                }
                                
                                Spacer(modifier = Modifier.height(6.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val valText = if (isDouble) "${p.value1}/${p.value2} $unit" else "${p.value1} $unit"
                                    Text(
                                        text = valText,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = primaryColor
                                    )
                                    ReadingClassificationBadge(p.value1, p.value2, title)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatLabelDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("MMM yyyy", Locale.US)
        val d = parser.parse(dateStr)
        if (d != null) formatter.format(d) else dateStr
    } catch(e: Exception) {
        dateStr
    }
}

fun filterVitalsByTimeframe(vitals: List<com.example.data.model.VitalSign>, timeframe: String): List<com.example.data.model.VitalSign> {
    if (vitals.isEmpty()) return vitals
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val now = Date()
    val cal = java.util.Calendar.getInstance()
    cal.time = now
    when (timeframe) {
        "3M" -> cal.add(java.util.Calendar.MONTH, -3)
        "6M" -> cal.add(java.util.Calendar.MONTH, -6)
        "1Y" -> cal.add(java.util.Calendar.YEAR, -1)
        else -> return vitals
    }
    val cutoffDate = cal.time
    return vitals.filter {
        try {
            val d = format.parse(it.date)
            d != null && d.after(cutoffDate)
        } catch(e: Exception) {
            true
        }
    }
}

@Composable
fun ClinicalOverviewSection(viewModel: HealthViewModel) {
    val overviewState by viewModel.clinicalOverviewState.collectAsState()
    
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = PrimaryDarkTeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Patient Clinical Overview",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Black
                    )
                }
                
                IconButton(
                    onClick = { viewModel.generateClinicalOverview() },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = PrimaryDarkTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(10.dp))
            
            when (val state = overviewState) {
                is com.example.ui.ClinicalOverviewState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = PrimaryDarkTeal, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AI is compiling clinical records...", fontSize = 12.sp, color = GrayText)
                    }
                }
                is com.example.ui.ClinicalOverviewState.Success -> {
                    Text(
                        text = state.overviewText,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Black
                    )
                    
                    if (state.keyObservations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Key Observations:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryDarkTeal)
                        state.keyObservations.forEach { obs ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Text("• ", color = PrimaryDarkTeal, fontSize = 12.sp)
                                Text(obs, fontSize = 12.sp, color = GrayText)
                            }
                        }
                    }
                    
                    if (state.riskIndicators.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Identified Risks:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = ErrorRed)
                        state.riskIndicators.forEach { risk ->
                            val color = when(risk.severity) {
                                "High" -> ErrorRed
                                "Medium" -> IconOrange
                                else -> IconGreen
                            }
                            val bgColor = when(risk.severity) {
                                "High" -> PastelPink
                                "Medium" -> PastelOrange
                                else -> PastelGreen
                            }
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = bgColor.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .background(color, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(risk.severity, color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(risk.title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Black)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(risk.description, fontSize = 11.sp, color = GrayText)
                                }
                            }
                        }
                    }
                    
                    if (state.recommendations.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("AI Recommendations:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryDarkTeal)
                        state.recommendations.forEach { rec ->
                            Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = IconGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(rec, fontSize = 11.sp, color = Black)
                            }
                        }
                    }
                }
                is com.example.ui.ClinicalOverviewState.Error -> {
                    Text(
                        text = state.message,
                        color = ErrorRed,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.generateClinicalOverview() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp).align(Alignment.End)
                    ) {
                        Text("Retry", fontSize = 11.sp, color = White)
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "No AI overview loaded yet.",
                            fontSize = 12.sp,
                            color = GrayText
                        )
                        Button(
                            onClick = { viewModel.generateClinicalOverview() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Generate", fontSize = 12.sp, color = White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupPromptCard(
    lastBackupTime: String?,
    isBackingUp: Boolean,
    onExportBackup: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (lastBackupTime == null) WarningLightAmber else BackgroundLightTeal
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        border = BorderStroke(
            1.dp,
            if (lastBackupTime == null) IconOrange.copy(alpha = 0.4f) else PrimaryDarkTeal.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (lastBackupTime == null) IconOrange.copy(alpha = 0.15f) else PrimaryDarkTeal.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (lastBackupTime == null) Icons.Filled.Warning else Icons.Filled.CloudUpload,
                            contentDescription = null,
                            tint = if (lastBackupTime == null) IconOrange else PrimaryDarkTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (lastBackupTime == null) "Data Loss Risk Alert" else "Backup Status Safe",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (lastBackupTime == null) IconOrange else PrimaryDarkTeal
                        )
                        Text(
                            text = if (lastBackupTime == null) "Offline storage only" else "Last backup: $lastBackupTime",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                    }
                }
                
                if (lastBackupTime == null) {
                    Box(
                        modifier = Modifier
                            .background(IconOrange, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "CRITICAL",
                            fontSize = 9.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(PrimaryDarkTeal, RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "SECURED",
                            fontSize = 9.sp,
                            color = White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = if (lastBackupTime == null) {
                    "Your personal health records, medical documents, and daily medications are stored locally on this phone. Ensure they remain safe in case of device damage or replacement."
                } else {
                    "Your health records are backed up. However, please back up regularly after adding new documents or changing your medication schedules."
                },
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Black
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onExportBackup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (lastBackupTime == null) IconOrange else PrimaryDarkTeal
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                enabled = !isBackingUp
            ) {
                if (isBackingUp) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting Backup...", fontSize = 12.sp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (lastBackupTime == null) "Perform Local Data Export Now" else "Perform New Backup Export",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickMedicationTracker(
    activeMeds: List<Medication>,
    logsToday: List<MedicationLog>,
    onToggleTaken: (Medication, Boolean) -> Unit,
    onAddMedication: (String, String, String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("quick_medication_tracker")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Medication, contentDescription = null, tint = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Daily Medication Inbox", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Black)
                }
                IconButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Filled.AddCircle, contentDescription = "Add Medication", tint = PrimaryDarkTeal)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            if (activeMeds.isEmpty()) {
                Text("No active medications found. Tap + to input a new prescription.", fontSize = 12.sp, color = GrayText)
            } else {
                activeMeds.take(4).forEach { med -> // Show up to 4 to save space
                    val isTaken = logsToday.any { it.medicationId == med.id }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(med.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Black)
                            Text("${med.dose} • ${med.frequency}", fontSize = 11.sp, color = GrayText)
                        }
                        Switch(
                            checked = isTaken,
                            onCheckedChange = { checked -> onToggleTaken(med, checked) },
                            colors = SwitchDefaults.colors(checkedThumbColor = White, checkedTrackColor = IconGreen)
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = BackgroundLightTeal)
                }
            }
        }
    }
    
    if (showAddDialog) {
        var inputName by remember { mutableStateOf("") }
        var inputDose by remember { mutableStateOf("") }
        var inputSchedule by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Quick Add Medication", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("Medication Name") },
                        modifier = Modifier.fillMaxWidth().testTag("quick_add_med_name")
                    )
                    OutlinedTextField(
                        value = inputDose,
                        onValueChange = { inputDose = it },
                        label = { Text("Dosage (e.g. 500mg)") },
                        modifier = Modifier.fillMaxWidth().testTag("quick_add_med_dose")
                    )
                    OutlinedTextField(
                        value = inputSchedule,
                        onValueChange = { inputSchedule = it },
                        label = { Text("Daily Schedule (e.g. 2 times)") },
                        modifier = Modifier.fillMaxWidth().testTag("quick_add_med_schedule")
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if(inputName.isNotBlank()) {
                        onAddMedication(inputName, inputDose, inputSchedule)
                        showAddDialog = false
                    }
                }) {
                    Text("Save Prescription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class ClinicInfo(
    val first: String,
    val second: String,
    val third: String,
    val fourth: String
)


