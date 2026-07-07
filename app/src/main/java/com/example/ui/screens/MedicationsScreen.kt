package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import com.example.ui.components.ResponsiveMedicationForm
import com.example.ui.theme.*
import com.example.data.model.Medication
import com.example.data.model.MedicationLog
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationsScreen(navController: NavController, viewModel: HealthViewModel) {
    val medications by viewModel.medications.collectAsState()
    val medicationLogs by viewModel.medicationLogs.collectAsState()
    val context = LocalContext.current
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.testMedicationNotification()
        }
    }

    var selectedSectionTab by remember { mutableStateOf(0) } // 0: Daily Tracker, 1: Prescriptions Hub

    // Medication add/edit state
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedMedication by remember { mutableStateOf<Medication?>(null) }

    // Quick Log State
    var showLogDialog by remember { mutableStateOf(false) }
    var loggingMedication by remember { mutableStateOf<Medication?>(null) }
    var loggingStatus by remember { mutableStateOf("Taken") }
    var logNotes by remember { mutableStateOf("") }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    fun openAddEditDialog(medication: Medication? = null) {
        selectedMedication = medication
        showAddEditDialog = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Medication Hub", "ඔෟෂධ මධ්‍යස්ථානය", "மருந்து மையம்", color = White, scale = 0.95f)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal),
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = White)
                    IconButton(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.testMedicationNotification()
                        }
                    }) {
                        Icon(Icons.Filled.Notifications, contentDescription = "Test Notification", tint = White)
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedSectionTab == 1) {
                FloatingActionButton(
                    onClick = { openAddEditDialog(null) },
                    containerColor = PrimaryDarkTeal,
                    contentColor = White
                ) {
                    Icon(Icons.Filled.Add, "Add Medication")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(LightGrayBackground)
        ) {
            // Screen Navigation Tabs
            TabRow(
                selectedTabIndex = selectedSectionTab,
                containerColor = White,
                contentColor = PrimaryDarkTeal,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSectionTab]),
                        color = PrimaryDarkTeal
                    )
                }
            ) {
                Tab(
                    selected = selectedSectionTab == 0,
                    onClick = { selectedSectionTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FactCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            TrilingualText(
                                english = "Daily Tracker", 
                                sinhala = "දෛනික ලුහුබැඳීම", 
                                tamil = "தினசரி கண்காணிப்பு", 
                                scale = 0.8125f
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedSectionTab == 1,
                    onClick = { selectedSectionTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Medication, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            TrilingualText(
                                english = "Prescriptions List", 
                                sinhala = "නිර්දේශිත ලැයිස්තුව", 
                                tamil = "மருந்து பட்டியல்", 
                                scale = 0.8125f
                            )
                        }
                    }
                )
            }

            if (selectedSectionTab == 0) {
                // DAILY TRACKER TAB
                val activeMeds = medications.filter { it.status == "Active" }
                val logsRef = medicationLogs.filter { it.logDate == todayStr }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    // Adherence progress card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    TrilingualText(
                                        english = "Today's Intake Adherence",
                                        sinhala = "අද දින ඖෂධ ලබා ගැනීම",
                                        tamil = "இன்றைய மருந்து உட்கொள்ளல்",
                                        scale = 0.9375f,
                                        color = Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (activeMeds.isEmpty()) {
                                        TrilingualText(
                                            english = "No active medications prescribed currently.",
                                            sinhala = "දැනට සක්‍රිය ඖෂධ නොමැත",
                                            tamil = "தற்போது மருந்துகள் இல்லை",
                                            scale = 0.75f,
                                            color = GrayText
                                        )
                                    } else {
                                        val takenCount = logsRef.count { it.status == "Taken" }
                                        val missedCount = logsRef.count { it.status == "Missed" }
                                        Text(
                                            "Logged $takenCount of ${activeMeds.size} medications as Taken today.",
                                            fontSize = 12.sp,
                                            color = GrayText
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        val statusText = when {
                                            takenCount == activeMeds.size -> "Perfect adherence! Great job."
                                            takenCount > 0 -> "You are on track. Maintain the streak!"
                                            else -> "Mark your scheduled medications once taken."
                                        }
                                        Text(
                                            statusText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryDarkTeal
                                        )
                                    }
                                }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(0.7f)
                                        .padding(start = 12.dp)
                                ) {
                                    val adherenceFrac = if (activeMeds.isEmpty()) 0f else {
                                        logsRef.count { it.status == "Taken" }.toFloat() / activeMeds.size.toFloat()
                                    }
                                    CircularProgressIndicator(
                                        progress = { adherenceFrac },
                                        modifier = Modifier.size(68.dp),
                                        color = PrimaryDarkTeal,
                                        strokeWidth = 6.dp,
                                        trackColor = BackgroundLightTeal,
                                    )
                                    Text(
                                        text = "${(adherenceFrac * 100).toInt()}%",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = PrimaryDarkTeal
                                    )
                                }
                            }
                        }
                    }

                    // Today's schedule checklist
                    item {
                        TrilingualText(
                            english = "Today's Checklist (Schedule)",
                            sinhala = "අද දින කාලසටහන",
                            tamil = "இன்றைய அட்டவணை",
                            scale = 0.875f,
                            color = PrimaryDarkTeal,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    if (activeMeds.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        Icons.Outlined.Assignment,
                                        contentDescription = null,
                                        tint = GrayText,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TrilingualText(
                                        english = "No medications are set to Active.",
                                        sinhala = "සක්‍රිය ඖෂධ නොමැත.",
                                        tamil = "மருந்துகள் இல்லை.",
                                        scale = 0.8125f,
                                        color = Black,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    TrilingualText(
                                        english = "Go to 'Prescriptions List' to add active prescriptions.",
                                        sinhala = "සක්‍රිය ඖෂධ එකතු කිරීමට 'ඖෂධ ලැයිස්තුව' වෙත යන්න.",
                                        tamil = "மருந்துகளை சேர்க்க 'மருந்து பட்டியல்' செல்லவும்.",
                                        scale = 0.6875f,
                                        color = GrayText,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    )
                                }
                            }
                        }
                    } else {
                        items(activeMeds) { med ->
                            val currentLog = logsRef.find { it.medicationId == med.id }
                            
                            Card(
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                med.name,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 15.sp,
                                                color = Black
                                            )
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Filled.AccessTime,
                                                    contentDescription = null,
                                                    tint = GrayText,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "${med.dose} • ${med.frequency} • Scheduled at ${med.reminderTime}",
                                                    fontSize = 11.sp,
                                                    color = GrayText
                                                )
                                            }
                                        }

                                        // Badge if logged
                                        if (currentLog != null) {
                                            val (badgeBg, badgeText, bColor) = when (currentLog.status) {
                                                "Taken" -> Triple(IconGreen.copy(alpha = 0.15f), "Taken", IconGreen)
                                                "Missed" -> Triple(ErrorRed.copy(alpha = 0.15f), "Missed", ErrorRed)
                                                else -> Triple(IconOrange.copy(alpha = 0.15f), "Skipped", IconOrange)
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .background(badgeBg, RoundedCornerShape(8.dp))
                                                    .border(0.5.dp, bColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    if (currentLog.status == "Taken") Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                                    contentDescription = null,
                                                    tint = bColor,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    badgeText,
                                                    color = bColor,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }

                                    if (currentLog != null && currentLog.notes.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Note: \"${currentLog.notes}\"",
                                            fontSize = 11.sp,
                                            color = GrayText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Actions Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // TAKEN
                                        Button(
                                            onClick = {
                                                loggingMedication = med
                                                loggingStatus = "Taken"
                                                logNotes = currentLog?.notes ?: ""
                                                showLogDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (currentLog?.status == "Taken") IconGreen else BackgroundLightTeal,
                                                contentColor = if (currentLog?.status == "Taken") White else IconGreen
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text("Taken", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // MISSED
                                        Button(
                                            onClick = {
                                                loggingMedication = med
                                                loggingStatus = "Missed"
                                                logNotes = currentLog?.notes ?: ""
                                                showLogDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (currentLog?.status == "Missed") ErrorRed else AlertLightRed.copy(alpha = 0.5f),
                                                contentColor = if (currentLog?.status == "Missed") White else ErrorRed
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text("Missed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // SKIPPED
                                        Button(
                                            onClick = {
                                                loggingMedication = med
                                                loggingStatus = "Skipped"
                                                logNotes = currentLog?.notes ?: ""
                                                showLogDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (currentLog?.status == "Skipped") IconOrange else LightGrayBackground,
                                                contentColor = if (currentLog?.status == "Skipped") White else IconOrange
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f).height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(Modifier.width(2.dp))
                                                Text("Skipped", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Historical log list title
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        TrilingualText(
                            english = "Historical Status Records (Past Logs)",
                            sinhala = "අතීත වාර්තා",
                            tamil = "கடந்த கால பதிவுகள்",
                            scale = 0.875f,
                            color = PrimaryDarkTeal,
                            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                        )
                    }

                    if (medicationLogs.isEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "No intake history logged yet. Use the buttons above to log.",
                                        fontSize = 11.sp,
                                        color = GrayText,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(medicationLogs) { log ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            log.medicationName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Black
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Filled.Event,
                                                contentDescription = null,
                                                tint = GrayText,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "${log.logDate} • Recorded at ${log.logTime}",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                        
                                        if (log.notes.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Note: \"${log.notes}\"",
                                                fontSize = 11.sp,
                                                color = GrayText
                                            )
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val badgeColor = when (log.status) {
                                            "Taken" -> IconGreen
                                            "Missed" -> ErrorRed
                                            else -> IconOrange
                                        }
                                        Text(
                                            text = log.status,
                                            color = badgeColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { viewModel.deleteMedicationLog(log) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete Log",
                                                tint = ErrorRed.copy(alpha = 0.7f),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ALL PRESCRIPTIONS TAB (Manage master items)
                if (medications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Outlined.Medication,
                                contentDescription = null,
                                tint = PrimaryDarkTeal,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TrilingualText(
                                "No prescription meds yet",
                                "තවම ඖෂධ එකතු කර නැත",
                                "இதுவரை மருந்து இல்லை",
                                color = PrimaryDarkTeal,
                                horizontalAlignment = Alignment.CenterHorizontally
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(medications) { med ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = White),
                                shape = RoundedCornerShape(14.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                med.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Black
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (med.status == "Active") PrimaryDarkTeal.copy(alpha = 0.1f) else ErrorRed.copy(alpha = 0.1f),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    med.status,
                                                    color = if (med.status == "Active") PrimaryDarkTeal else ErrorRed,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                        Row {
                                            IconButton(onClick = { 
                                                val sendIntent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(
                                                        android.content.Intent.EXTRA_TEXT,
                                                        "Medication Reminder Alert:\n" +
                                                        "💊 Med: ${med.name}\n" +
                                                        "📏 Dose: ${med.dose}\n" +
                                                        "🔄 Frequency: ${med.frequency}\n" +
                                                        "⏰ Scheduled Reminder: ${med.reminderTime}\n" +
                                                        "👨‍⚕️ Prescribed by: ${med.doctorName}"
                                                    )
                                                    type = "text/plain"
                                                    setPackage("com.whatsapp")
                                                }
                                                try {
                                                    context.startActivity(sendIntent)
                                                } catch (e: Exception) {
                                                    val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                                                    context.startActivity(shareIntent)
                                                }
                                            }) {
                                                Icon(Icons.Filled.Share, contentDescription = "Share", tint = PrimaryDarkTeal)
                                            }
                                            IconButton(onClick = { openAddEditDialog(med) }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = PrimaryDarkTeal)
                                            }
                                            IconButton(onClick = { viewModel.deleteMedication(med) }) {
                                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = AlertLightRed)
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(4.dp))
                                    
                                    val freqDisplay = if (med.frequencyType == "Weekly") {
                                        val days = med.frequencyDaysOfWeek.ifEmpty { "No Days" }
                                        "Weekly on ($days) at ${med.reminderTime}"
                                    } else {
                                        "Daily at ${med.reminderTime}"
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "${med.dose} • ${med.frequency}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Black,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        // Notification reminder enabled badge
                                        val isNotifActive = med.notificationEnabled && med.status == "Active"
                                        val icon = if (isNotifActive) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff
                                        val badgeColor = if (isNotifActive) PrimaryDarkTeal else GrayText
                                        val badgeBg = if (isNotifActive) BackgroundLightTeal else LightGrayBackground
                                        val label = if (isNotifActive) "Reminders On" else "Reminders Off"
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .background(badgeBg, RoundedCornerShape(6.dp))
                                                .border(0.5.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = badgeColor,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = label,
                                                color = badgeColor,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        "Reminder Scheme: $freqDisplay",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PrimaryDarkTeal,
                                        fontWeight = FontWeight.Medium
                                    )
                                    
                                    if (med.startDate.isNotEmpty() || med.endDate.isNotEmpty()) {
                                        val dates = listOf(
                                            if (med.startDate.isNotEmpty()) "Start: ${med.startDate}" else null,
                                            if (med.endDate.isNotEmpty()) "End: ${med.endDate}" else null
                                        ).mapNotNull { it }.joinToString(" - ")
                                        Text(dates, style = MaterialTheme.typography.bodySmall, color = PrimaryDarkTeal)
                                    }
                                    Text("Prescribed by: ${med.doctorName}", style = MaterialTheme.typography.bodySmall, color = GrayText)

                                    if (med.status == "Active") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                setSystemAlarm(context, med.name, med.dose, med.reminderTime, repeatDaily = true)
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = BackgroundLightTeal,
                                                contentColor = PrimaryDarkTeal
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Filled.Alarm,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Set Physical Phone Alarm Clock (Loud Ringing)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 1. ADD / EDIT PRESCRIPTION DIALOG
        if (showAddEditDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddEditDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.9f)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    ResponsiveMedicationForm(
                        selectedMedication = selectedMedication,
                        onSave = { updatedMed ->
                            if (selectedMedication == null) {
                                viewModel.insertMedication(updatedMed)
                            } else {
                                viewModel.updateMedication(updatedMed)
                            }
                            showAddEditDialog = false
                        },
                        onCancel = { showAddEditDialog = false }
                    )
                }
            }
        }

        // 2. LOG ENTRY HABIT / TAKEN DIALOG WITH NOTES
        if (showLogDialog && loggingMedication != null) {
            val loggingMed = loggingMedication!!
            AlertDialog(
                onDismissRequest = { showLogDialog = false },
                title = { Text("Mark Medication Log", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Record dosage status for:",
                            fontSize = 11.sp,
                            color = GrayText
                        )
                        Text(
                            text = "${loggingMed.name} (${loggingMed.dose})",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryDarkTeal
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Status Log:")
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .background(
                                        when (loggingStatus) {
                                            "Taken" -> IconGreen.copy(alpha = 0.15f)
                                            "Missed" -> ErrorRed.copy(alpha = 0.15f)
                                            else -> IconOrange.copy(alpha = 0.15f)
                                        },
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = loggingStatus,
                                    color = when (loggingStatus) {
                                        "Taken" -> IconGreen
                                        "Missed" -> ErrorRed
                                        else -> IconOrange
                                    },
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = logNotes,
                            onValueChange = { logNotes = it },
                            label = { Text("Add daily note (e.g. side effects, with meal?)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date())
                        val newLog = MedicationLog(
                            medicationId = loggingMed.id,
                            medicationName = loggingMed.name,
                            logDate = todayStr,
                            logTime = timeStr,
                            status = loggingStatus,
                            notes = logNotes
                        )
                        viewModel.insertMedicationLog(newLog)
                        showLogDialog = false
                    }) {
                        Text("Log Statement")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SegmentedButton(selected: Boolean, onClick: () -> Unit, text: String) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) PrimaryDarkTeal else BackgroundLightTeal,
            contentColor = if (selected) White else PrimaryDarkTeal
        ),
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.height(36.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

fun setSystemAlarm(context: android.content.Context, name: String, dose: String, reminderTimeStr: String, repeatDaily: Boolean = true) {
    try {
        var hour = 9
        var minute = 0
        
        val cleanTime = reminderTimeStr.trim().uppercase()
        val parsed = try {
            val format1 = SimpleDateFormat("hh:mm a", Locale.US)
            format1.parse(cleanTime)
        } catch (e: Exception) {
            try {
                val format2 = SimpleDateFormat("h:mm a", Locale.US)
                format2.parse(cleanTime)
            } catch (e2: Exception) {
                try {
                    val format3 = SimpleDateFormat("HH:mm", Locale.US)
                    format3.parse(cleanTime)
                } catch (e3: Exception) {
                    null
                }
            }
        }
        
        if (parsed != null) {
            val calendar = Calendar.getInstance().apply { time = parsed }
            hour = calendar.get(Calendar.HOUR_OF_DAY)
            minute = calendar.get(Calendar.MINUTE)
        } else {
            val parts = cleanTime.split(":")
            if (parts.size >= 2) {
                var hourPart = parts[0].filter { it.isDigit() }.toIntOrNull() ?: 9
                val minAndAmPm = parts[1]
                val minPart = minAndAmPm.filter { it.isDigit() }.toIntOrNull() ?: 0
                minute = minPart
                
                if (minAndAmPm.contains("PM") && hourPart < 12) {
                    hourPart += 12
                } else if (minAndAmPm.contains("AM") && hourPart == 12) {
                    hourPart = 0
                }
                hour = hourPart
            }
        }
        
        val intent = android.content.Intent(android.provider.AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(android.provider.AlarmClock.EXTRA_HOUR, hour)
            putExtra(android.provider.AlarmClock.EXTRA_MINUTES, minute)
            putExtra(android.provider.AlarmClock.EXTRA_MESSAGE, "💊 Take Med: $name ($dose)")
            if (repeatDaily) {
                val days = arrayListOf(
                    Calendar.SUNDAY,
                    Calendar.MONDAY,
                    Calendar.TUESDAY,
                    Calendar.WEDNESDAY,
                    Calendar.THURSDAY,
                    Calendar.FRIDAY,
                    Calendar.SATURDAY
                )
                putExtra(android.provider.AlarmClock.EXTRA_DAYS, days)
            }
            putExtra(android.provider.AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        android.widget.Toast.makeText(context, "Redirecting to set system alarm for $reminderTimeStr...", android.widget.Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        e.printStackTrace()
        android.widget.Toast.makeText(context, "Cannot register system alarm: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    }
}
