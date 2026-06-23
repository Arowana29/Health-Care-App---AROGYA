package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.AppTitleText
import com.example.ui.components.TrilingualText
import com.example.ui.theme.*
import com.example.data.model.SymptomLog
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomsScreen(navController: NavController, viewModel: HealthViewModel) {
    val symptomLogs by viewModel.symptomLogs.collectAsState()
    val context = LocalContext.current

    // Local UI State
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStatusFilter by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }

    // Add dialog input state
    var symptomNameInput by remember { mutableStateOf("") }
    var severityInput by remember { mutableStateOf(5) }
    var dateInput by remember { mutableStateOf("") }
    var statusInput by remember { mutableStateOf("Active") }
    var notesInput by remember { mutableStateOf("") }

    // Pre-fill date when dialog opens
    LaunchedEffect(showAddDialog) {
        if (showAddDialog) {
            symptomNameInput = ""
            severityInput = 5
            dateInput = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            statusInput = "Active"
            notesInput = ""
        }
    }

    // Filtered symptom list
    val filteredLogs = remember(symptomLogs, selectedStatusFilter, searchQuery) {
        symptomLogs.filter { log ->
            val matchesStatus = selectedStatusFilter == "All" || log.status.equals(selectedStatusFilter, ignoreCase = true)
            val matchesSearch = log.name.contains(searchQuery, ignoreCase = true) || log.notes.contains(searchQuery, ignoreCase = true)
            matchesStatus && matchesSearch
        }
    }

    // Calculations & Metrics
    val activeCount = symptomLogs.count { it.status.equals("Active", ignoreCase = true) }
    val resolvedCount = symptomLogs.count { it.status.equals("Resolved", ignoreCase = true) }
    val avgSeverity = if (filteredLogs.isNotEmpty()) {
        filteredLogs.map { it.severity }.average()
    } else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Symptom Journal", "රෝග ලක්ෂණ සටහන", "அறிகுறி நாட்குறிப்பு", color = White, scale = 0.95f)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = White)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryDarkTeal,
                contentColor = White,
                modifier = Modifier.testTag("add_symptom_fab")
            ) {
                Icon(Icons.Filled.Add, "Log Symptom")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(LightGrayBackground)
        ) {
            // 1. SYMPTOM ANALYTICS PANEL
            Card(
                shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            TrilingualText(
                                english = "Average Logged Severity",
                                sinhala = "සාමාන්‍ය බරපතලකම",
                                tamil = "சராசரி தீவிரத்தன்மை",
                                color = GrayText,
                                scale = 0.85f
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format("%.1f", avgSeverity),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (avgSeverity >= 7.0) ErrorRed else if (avgSeverity >= 4.0) IconOrange else IconGreen
                                )
                                Text(
                                    text = " / 10",
                                    fontSize = 18.sp,
                                    color = GrayText,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                                )
                            }
                        }

                        // Badges for Active and Resolved
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Active Badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(PastelPink.copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$activeCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                                TrilingualText("Active", "සක්‍රීය", "செயலில்", color = ErrorRed, scale = 0.7f)
                            }

                            // Resolved Badge
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .background(BackgroundLightTeal, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("$resolvedCount", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal)
                                TrilingualText("Resolved", "සුවපත්", "தீர்க்கப்பட்டது", color = PrimaryDarkTeal, scale = 0.7f)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sparkline/Quick Progress Bar representation of current filtered symptoms severity
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    ) {
                        val activeSeveritySum = filteredLogs.filter { it.status.equals("Active", ignoreCase = true) }.sumOf { it.severity }
                        val totalSeveritySum = filteredLogs.sumOf { it.severity }
                        
                        if (totalSeveritySum > 0) {
                            val activeRatio = (activeSeveritySum.toFloat() / totalSeveritySum)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(activeRatio)
                                    .background(ErrorRed)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f - activeRatio)
                                    .background(IconGreen)
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(Color.LightGray))
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TrilingualText("Active Share", "සක්‍රීය බර", "செயலில் உள்ள பங்கு", color = GrayText, scale = 0.7f)
                        TrilingualText("Resolved Share", "සුවපත් බර", "தீர்க்கப்பட்ட பங்கு", color = GrayText, scale = 0.7f)
                    }
                }
            }

            // 2. SEARCH & QUICK FILTER CHIPS
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                // Search Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { TrilingualText("Search symptoms...", "රෝග ලක්ෂණ සොයන්න...", "அறிகுறிகளைத் தேடுங்கள்...", color = GrayText, scale = 0.9f) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryDarkTeal) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("symptom_search_input"),
                    shape = RoundedCornerShape(26.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = PrimaryDarkTeal,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Active", "Resolved").forEach { status ->
                        val isSelected = selectedStatusFilter == status
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedStatusFilter = status },
                            label = { Text(status) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryDarkTeal,
                                selectedLabelColor = White,
                                containerColor = White,
                                labelColor = GrayText
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = Color.LightGray.copy(alpha = 0.5f),
                                selectedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            // 3. SECURE SYMPTOM LOG LIST
            if (filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.LibraryBooks,
                            contentDescription = "No symptoms",
                            tint = PrimaryDarkTeal.copy(alpha = 0.25f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        TrilingualText(
                            english = "No health observations recorded.",
                            sinhala = "රෝග ලක්ෂණ සටහන් කිසිවක් නැත.",
                            tamil = "அறிகுறி பதிவுகள் எதுவும் இல்லை.",
                            color = GrayText,
                            scale = 0.85f,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 86.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredLogs) { log ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(1.dp, RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(
                                                    if (log.status.equals("Active", ignoreCase = true)) ErrorRed else IconGreen,
                                                    CircleShape
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = log.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Black
                                        )
                                    }

                                    // Severity Pill Indicator
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                color = if (log.severity >= 7) ErrorRed.copy(alpha = 0.1f)
                                                       else if (log.severity >= 4) IconOrange.copy(alpha = 0.1f)
                                                       else IconGreen.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "Severity: ${log.severity}/10",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (log.severity >= 7) ErrorRed
                                                   else if (log.severity >= 4) IconOrange
                                                   else IconGreen
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CalendarMonth,
                                        contentDescription = "Date",
                                        tint = GrayText,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = log.date,
                                        fontSize = 11.sp,
                                        color = GrayText
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (log.status.equals("Active", ignoreCase = true)) PastelPink else BackgroundLightTeal,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.status.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (log.status.equals("Active", ignoreCase = true)) ErrorRed else PrimaryDarkTeal
                                        )
                                    }
                                }

                                // Observations/Notes Field
                                if (log.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(LightGrayBackground, RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            TrilingualText("Observations", "නිරීක්ෂණ", "அவதானிப்புகள்", color = PrimaryDarkTeal, scale = 0.75f)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = log.notes,
                                                fontSize = 12.sp,
                                                color = Black
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Switch Status Action
                                    TextButton(
                                        onClick = {
                                            val newStatus = if (log.status.equals("Active", ignoreCase = true)) "Resolved" else "Active"
                                            viewModel.addSymptomLogWithDate(
                                                name = log.name,
                                                severity = log.severity,
                                                date = log.date,
                                                status = newStatus,
                                                notes = log.notes
                                            )
                                            // Since we replace with same parameters but changed status (using id, or since Room upserts / we can just write insert or simple replacement logic). Wait, let's delete the old entry and add the updated one so we don't duplicate. Or wait, let's delete first.
                                            viewModel.deleteSymptomLog(log)
                                            Toast.makeText(context, "Status updated", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                if (log.status.equals("Active", ignoreCase = true)) Icons.Filled.CheckCircle else Icons.Filled.Replay,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                if (log.status.equals("Active", ignoreCase = true)) "Mark Resolved" else "Mark Active",
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    // Delete log button
                                    IconButton(
                                        onClick = {
                                            viewModel.deleteSymptomLog(log)
                                            Toast.makeText(context, "Symptom journal entry deleted", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "Delete Log",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // 4. ADD NEW SYMPTOM LOG DIALOG WITH OBSERVATIONS NOTES
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                TrilingualText(
                    english = "Log Symptom Observation",
                    sinhala = "නව රෝග ලක්ෂණයක් සටහන් කරන්න",
                    tamil = "அறிகுறி பதிவை உள்ளிடவும்",
                    color = PrimaryDarkTeal,
                    scale = 1.05f
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Symptom Name Choice/Input
                    var customSymptomMode by remember { mutableStateOf(false) }
                    
                    if (!customSymptomMode) {
                        Text("Select Common Symptom:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("Fever", "Cough & Flu", "Headache", "Body Pain", "Stomach Ache", "Fatigue", "Custom").forEach { sym ->
                                val isSelected = symptomNameInput == sym
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        if (sym == "Custom") {
                                            customSymptomMode = true
                                            symptomNameInput = ""
                                        } else {
                                            symptomNameInput = sym
                                        }
                                    },
                                    label = { Text(sym) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BackgroundLightTeal,
                                        selectedLabelColor = PrimaryDarkTeal
                                    )
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = symptomNameInput,
                            onValueChange = { symptomNameInput = it },
                            label = { Text("Symptom Name (e.g. Vertigo)") },
                            trailingIcon = {
                                IconButton(onClick = { customSymptomMode = false }) {
                                    Icon(Icons.Filled.Close, "Cancel Custom")
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_symptom_name_input"),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Severity Rating (1 to 10 Slider)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Severity Level: $severityInput / 10", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (severityInput >= 8) "Severe" else if (severityInput >= 4) "Moderate" else "Mild",
                                color = if (severityInput >= 8) ErrorRed else if (severityInput >= 4) IconOrange else IconGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Slider(
                            value = severityInput.toFloat(),
                            onValueChange = { severityInput = it.toInt() },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryDarkTeal,
                                activeTrackColor = PrimaryDarkTeal,
                                inactiveTrackColor = Color.LightGray
                            )
                        )
                    }

                    // Date Input
                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_symptom_date_input"),
                        singleLine = true
                    )

                    // Observation Notes / Comments
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Health Observations & Notes") },
                        placeholder = { Text("e.g. Took paracetamol at 9 PM. Feeling dizzy.") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(84.dp)
                            .testTag("add_symptom_notes_input"),
                        maxLines = 3
                    )

                    // Status Dropdown / Segmented Toggle
                    Text("Current Status:", fontSize = 11.sp, color = GrayText, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Active", "Resolved").forEach { st ->
                            val isSelected = statusInput == st
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) PrimaryDarkTeal else White
                                ),
                                border = BorderStroke(1.dp, if (isSelected) Color.Transparent else Color.LightGray),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { statusInput = st }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = st,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isSelected) White else GrayText
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (symptomNameInput.trim().isNotEmpty()) {
                            viewModel.addSymptomLogWithDate(
                                name = symptomNameInput,
                                severity = severityInput,
                                date = dateInput,
                                status = statusInput,
                                notes = notesInput
                            )
                            showAddDialog = false
                            Toast.makeText(context, "Symptom observation saved offline!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter symptom name", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                    modifier = Modifier.testTag("submit_symptom_button")
                ) {
                    Text("Save")
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
