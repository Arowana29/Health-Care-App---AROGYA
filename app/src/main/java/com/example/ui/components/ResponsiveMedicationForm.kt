package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Medication
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveMedicationForm(
    selectedMedication: Medication?,
    onSave: (Medication) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // 1. Local State initialized from selectedMedication
    var name by remember { mutableStateOf(selectedMedication?.name ?: "") }
    var dose by remember { mutableStateOf(selectedMedication?.dose ?: "") }
    var frequency by remember { mutableStateOf(selectedMedication?.frequency ?: "") }
    var doctorName by remember { mutableStateOf(selectedMedication?.doctorName ?: "") }
    var status by remember { mutableStateOf(selectedMedication?.status ?: "Active") }
    var startDate by remember { mutableStateOf(selectedMedication?.startDate ?: "") }
    var endDate by remember { mutableStateOf(selectedMedication?.endDate ?: "") }
    var reminderTime by remember { mutableStateOf(selectedMedication?.reminderTime ?: "09:00 AM") }
    var frequencyType by remember { mutableStateOf(selectedMedication?.frequencyType ?: "Daily") }
    
    val initialDays = selectedMedication?.frequencyDaysOfWeek
    var frequencyDaysOfWeek by remember { 
        mutableStateOf(if (initialDays.isNullOrEmpty()) setOf<String>() else initialDays.split(",").map { it.trim() }.toSet()) 
    }
    var notificationEnabled by remember { mutableStateOf(selectedMedication?.notificationEnabled ?: true) }
    var autoCreateSystemAlarm by remember { mutableStateOf(selectedMedication == null) }

    // Validation State
    var showValidationError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    // Date Picker States
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    val startDatePickerState = rememberDatePickerState()
    val endDatePickerState = rememberDatePickerState()

    // Time picker custom dialog state
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Suggestion lists
    val doseSuggestions = listOf("1 Tablet", "2 Tablets", "1 Capsule", "5 ml", "10 ml", "1 Puff", "2 Puffs", "10 mg", "500 mg")
    val freqSuggestions = listOf("Once daily", "Twice daily", "Three times daily", "Every 8 hours", "Before bed", "As needed")
    val timeSuggestions = listOf("08:00 AM", "09:00 AM", "12:00 PM", "02:00 PM", "06:00 PM", "09:00 PM", "10:00 PM")

    // Formatting date helper
    fun formatDate(millis: Long?): String {
        if (millis == null) return ""
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.US)
        return formatter.format(Date(millis))
    }

    if (showStartDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startDate = formatDate(startDatePickerState.selectedDateMillis)
                    showStartDatePicker = false
                }) {
                    Text("Select", color = PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        ) {
            DatePicker(
                state = startDatePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = PrimaryDarkTeal,
                    headlineContentColor = PrimaryDarkTeal,
                    selectedDayContainerColor = PrimaryDarkTeal,
                    selectedDayContentColor = Color.White
                )
            )
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    endDate = formatDate(endDatePickerState.selectedDateMillis)
                    showEndDatePicker = false
                }) {
                    Text("Select", color = PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        ) {
            DatePicker(
                state = endDatePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = PrimaryDarkTeal,
                    headlineContentColor = PrimaryDarkTeal,
                    selectedDayContainerColor = PrimaryDarkTeal,
                    selectedDayContentColor = Color.White
                )
            )
        }
    }

    // Modern custom Time Selector Dialog
    if (showTimePickerDialog) {
        var localTimeHour by remember { mutableStateOf(9) }
        var localTimeMinute by remember { mutableStateOf(0) }
        var localTimePeriod by remember { mutableStateOf("AM") }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                TrilingualText(
                    english = "Select Time",
                    sinhala = "වේලාව තෝරන්න",
                    tamil = "நேரத்தைத் தேர்ந்தெடுக்கவும்",
                    color = PrimaryDarkTeal
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Quick selection Row
                    Text("Quick Presets:", fontSize = 12.sp, color = GrayText, modifier = Modifier.padding(bottom = 6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        items(timeSuggestions) { suggested ->
                            Box(
                                modifier = Modifier
                                    .background(BackgroundLightTeal, RoundedCornerShape(12.dp))
                                    .clickable {
                                        reminderTime = suggested
                                        showTimePickerDialog = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(suggested, color = PrimaryDarkTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Divider(color = LightGrayBackground, modifier = Modifier.padding(bottom = 16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Hour Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { if (localTimeHour < 12) localTimeHour++ else localTimeHour = 1 }) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Increase Hour", tint = PrimaryDarkTeal)
                            }
                            Text(
                                text = String.format("%02d", localTimeHour),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = PrimaryDarkTeal
                            )
                            IconButton(onClick = { if (localTimeHour > 1) localTimeHour-- else localTimeHour = 12 }) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Decrease Hour", tint = PrimaryDarkTeal)
                            }
                        }

                        Text(" : ", style = MaterialTheme.typography.headlineMedium, color = PrimaryDarkTeal)

                        // Minute Selector
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            IconButton(onClick = { localTimeMinute = (localTimeMinute + 5) % 60 }) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = "Increase Minute", tint = PrimaryDarkTeal)
                            }
                            Text(
                                text = String.format("%02d", localTimeMinute),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = PrimaryDarkTeal
                            )
                            IconButton(onClick = { localTimeMinute = if (localTimeMinute >= 5) localTimeMinute - 5 else 55 }) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = "Decrease Minute", tint = PrimaryDarkTeal)
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // AM/PM Selector
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (localTimePeriod == "AM") PrimaryDarkTeal else BackgroundLightTeal)
                                    .clickable { localTimePeriod = "AM" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("AM", color = if (localTimePeriod == "AM") Color.White else PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (localTimePeriod == "PM") PrimaryDarkTeal else BackgroundLightTeal)
                                    .clickable { localTimePeriod = "PM" }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("PM", color = if (localTimePeriod == "PM") Color.White else PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reminderTime = String.format("%02d:%02d %s", localTimeHour, localTimeMinute, localTimePeriod)
                        showTimePickerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, PrimaryDarkTeal.copy(alpha = 0.15f), RoundedCornerShape(24.dp)),
        color = Color.White,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(BackgroundLightTeal, RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Medication,
                        contentDescription = null,
                        tint = PrimaryDarkTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    TrilingualText(
                        english = if (selectedMedication == null) "New Prescription Entry" else "Modify Prescription Details",
                        sinhala = if (selectedMedication == null) "අලුත් ඖෂධ සටහන" else "ඖෂධ විස්තර සංස්කරණය කරන්න",
                        tamil = if (selectedMedication == null) "புதிய மருந்து நுழைவு" else "மருந்து விவரங்களை மாற்றவும்",
                        color = PrimaryDarkTeal,
                        scale = 1.05f
                    )
                    Text(
                        text = "Fill in the parameters below to verify dosage compliance.",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }

            Divider(color = LightGrayBackground, modifier = Modifier.padding(bottom = 16.dp))

            // Real-time animation warning banner if error exists
            AnimatedVisibility(
                visible = showValidationError,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .background(AlertLightRed.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .border(1.dp, ErrorRed.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = "Warning", tint = ErrorRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = validationMessage,
                            color = ErrorRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Adaptive Multi-Column Form Grid
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth > 600.dp

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column
                        Column(modifier = Modifier.weight(1f)) {
                            FormInputsSectionLeft(
                                name = name,
                                onNameChange = { name = it; showValidationError = false },
                                dose = dose,
                                onDoseChange = { dose = it; showValidationError = false },
                                frequency = frequency,
                                onFrequencyChange = { frequency = it; showValidationError = false },
                                startDate = startDate,
                                onStartDateClick = { showStartDatePicker = true },
                                doseSuggestions = doseSuggestions,
                                freqSuggestions = freqSuggestions
                            )
                        }

                        // Right Column
                        Column(modifier = Modifier.weight(1f)) {
                            FormInputsSectionRight(
                                reminderTime = reminderTime,
                                onReminderTimeClick = { showTimePickerDialog = true },
                                frequencyType = frequencyType,
                                onFrequencyTypeChange = { frequencyType = it },
                                frequencyDaysOfWeek = frequencyDaysOfWeek,
                                onFrequencyDaysOfWeekChange = { frequencyDaysOfWeek = it },
                                doctorName = doctorName,
                                onDoctorNameChange = { doctorName = it },
                                endDate = endDate,
                                onEndDateClick = { showEndDatePicker = true },
                                onClearEndDate = { endDate = "" },
                                status = status,
                                onStatusChange = { status = it },
                                notificationEnabled = notificationEnabled,
                                onNotificationEnabledChange = { notificationEnabled = it },
                                autoCreateSystemAlarm = autoCreateSystemAlarm,
                                onAutoCreateSystemAlarmChange = { autoCreateSystemAlarm = it }
                            )
                        }
                    }
                } else {
                    // Vertical stacked column for Compact screens (Mobile portrait)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                    ) {
                        FormInputsSectionLeft(
                            name = name,
                            onNameChange = { name = it; showValidationError = false },
                            dose = dose,
                            onDoseChange = { dose = it; showValidationError = false },
                            frequency = frequency,
                            onFrequencyChange = { frequency = it; showValidationError = false },
                            startDate = startDate,
                            onStartDateClick = { showStartDatePicker = true },
                            doseSuggestions = doseSuggestions,
                            freqSuggestions = freqSuggestions
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FormInputsSectionRight(
                            reminderTime = reminderTime,
                            onReminderTimeClick = { showTimePickerDialog = true },
                            frequencyType = frequencyType,
                            onFrequencyTypeChange = { frequencyType = it },
                            frequencyDaysOfWeek = frequencyDaysOfWeek,
                            onFrequencyDaysOfWeekChange = { frequencyDaysOfWeek = it },
                            doctorName = doctorName,
                            onDoctorNameChange = { doctorName = it },
                            endDate = endDate,
                            onEndDateClick = { showEndDatePicker = true },
                            onClearEndDate = { endDate = "" },
                            status = status,
                            onStatusChange = { status = it },
                            notificationEnabled = notificationEnabled,
                            onNotificationEnabledChange = { notificationEnabled = it },
                            autoCreateSystemAlarm = autoCreateSystemAlarm,
                            onAutoCreateSystemAlarmChange = { autoCreateSystemAlarm = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action controls buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(contentColor = PrimaryDarkTeal),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            validationMessage = "Medication name is required."
                            showValidationError = true
                        } else if (dose.isBlank()) {
                            validationMessage = "Dose parameter is required (e.g. 1 pill)."
                            showValidationError = true
                        } else if (frequency.isBlank()) {
                            validationMessage = "Frequency protocol is required."
                            showValidationError = true
                        } else {
                            val verifiedStartDate = startDate.ifEmpty {
                                SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date())
                            }
                            val savedMed = Medication(
                                id = selectedMedication?.id ?: 0,
                                name = name,
                                dose = dose,
                                frequency = frequency,
                                doctorName = doctorName,
                                status = status,
                                startDate = verifiedStartDate,
                                endDate = endDate,
                                reminderTime = reminderTime,
                                frequencyType = frequencyType,
                                frequencyDaysOfWeek = frequencyDaysOfWeek.joinToString(","),
                                notificationEnabled = notificationEnabled
                            )
                            onSave(savedMed)
                            
                            if (autoCreateSystemAlarm && status == "Active") {
                                com.example.ui.screens.setSystemAlarm(context, name, dose, reminderTime, repeatDaily = true)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Prescription", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormInputsSectionLeft(
    name: String,
    onNameChange: (String) -> Unit,
    dose: String,
    onDoseChange: (String) -> Unit,
    frequency: String,
    onFrequencyChange: (String) -> Unit,
    startDate: String,
    onStartDateClick: () -> Unit,
    doseSuggestions: List<String>,
    freqSuggestions: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Medication Name
        Text("Medication Name *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = { Text("e.g. Paracetamol / Metformin", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Medication, contentDescription = null, tint = PrimaryDarkTeal) },
            trailingIcon = if (name.isNotEmpty()) {
                { IconButton(onClick = { onNameChange("") }) { Icon(Icons.Filled.Cancel, contentDescription = "Clear", tint = GrayText) } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            isError = name.isBlank() && name.isNotEmpty(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Dosage
        Text("Dose *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = dose,
            onValueChange = onDoseChange,
            placeholder = { Text("e.g. 1 Tablet / 10mg", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Category, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Quick Dose chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(doseSuggestions) { suggestion ->
                val isSelected = dose == suggestion
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                        .clickable { onDoseChange(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else PrimaryDarkTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 3. Frequency
        Text("Frequency *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = frequency,
            onValueChange = onFrequencyChange,
            placeholder = { Text("e.g. Twice daily / Every 12 Hours", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Loop, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Quick Freq chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(freqSuggestions) { suggestion ->
                val isSelected = frequency == suggestion
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                        .clickable { onFrequencyChange(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = suggestion,
                        fontSize = 11.sp,
                        color = if (isSelected) Color.White else PrimaryDarkTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 4. Start Date
        Text("Start Date", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBackground.copy(alpha = 0.5f))
                .border(1.dp, PrimaryDarkTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { onStartDateClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = startDate.ifEmpty { "Today (Select Date)" },
                        fontSize = 14.sp,
                        color = if (startDate.isEmpty()) GrayText else Black,
                        fontWeight = if (startDate.isEmpty()) FontWeight.Normal else FontWeight.Medium
                    )
                }
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PrimaryDarkTeal)
            }
        }
    }
}

@Composable
fun FormInputsSectionRight(
    reminderTime: String,
    onReminderTimeClick: () -> Unit,
    frequencyType: String,
    onFrequencyTypeChange: (String) -> Unit,
    frequencyDaysOfWeek: Set<String>,
    onFrequencyDaysOfWeekChange: (Set<String>) -> Unit,
    doctorName: String,
    onDoctorNameChange: (String) -> Unit,
    endDate: String,
    onEndDateClick: () -> Unit,
    onClearEndDate: () -> Unit,
    status: String,
    onStatusChange: (String) -> Unit,
    notificationEnabled: Boolean,
    onNotificationEnabledChange: (Boolean) -> Unit,
    autoCreateSystemAlarm: Boolean,
    onAutoCreateSystemAlarmChange: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Reminder Time
        Text("Daily Notification Reminder Time", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBackground.copy(alpha = 0.5f))
                .border(1.dp, PrimaryDarkTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { onReminderTimeClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AccessTime, contentDescription = null, tint = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = reminderTime,
                        fontSize = 14.sp,
                        color = Black,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(Icons.Filled.Edit, contentDescription = "Edit Time", tint = PrimaryDarkTeal, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Notification Schedule Type
        Text("Adherence Reminder Type", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (frequencyType == "Daily") PrimaryDarkTeal else BackgroundLightTeal)
                    .clickable { onFrequencyTypeChange("Daily") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Daily Interval", color = if (frequencyType == "Daily") Color.White else PrimaryDarkTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (frequencyType == "Weekly") PrimaryDarkTeal else BackgroundLightTeal)
                    .clickable { onFrequencyTypeChange("Weekly") }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Weekly Select", color = if (frequencyType == "Weekly") Color.White else PrimaryDarkTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Animated Days Selection
        AnimatedVisibility(visible = frequencyType == "Weekly") {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text("Select active days:", fontSize = 11.sp, color = GrayText, modifier = Modifier.padding(bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    days.forEach { d ->
                        val isSelected = frequencyDaysOfWeek.contains(d)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                                .clickable {
                                    if (isSelected) onFrequencyDaysOfWeekChange(frequencyDaysOfWeek - d)
                                    else onFrequencyDaysOfWeekChange(frequencyDaysOfWeek + d)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(d.take(1), color = if (isSelected) Color.White else PrimaryDarkTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 3. Status Selector
        Text("Prescription Status", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val statusOptions = listOf("Active", "Completed", "Stopped")
            statusOptions.forEach { opt ->
                val isSelected = status == opt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                        .clickable { onStatusChange(opt) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(opt, color = if (isSelected) Color.White else PrimaryDarkTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 4. Doctor Name
        Text("Prescribing Doctor", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = doctorName,
            onValueChange = onDoctorNameChange,
            placeholder = { Text("e.g. Dr. K. Silva", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 5. End Date
        Text("End Date (Optional)", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBackground.copy(alpha = 0.5f))
                .border(1.dp, PrimaryDarkTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { onEndDateClick() }
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = endDate.ifEmpty { "Never Ends" },
                        fontSize = 14.sp,
                        color = if (endDate.isEmpty()) GrayText else Black,
                        fontWeight = if (endDate.isEmpty()) FontWeight.Normal else FontWeight.Medium
                    )
                }
                if (endDate.isNotEmpty()) {
                    IconButton(
                        onClick = { onClearEndDate() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Filled.Cancel, contentDescription = "Clear End Date", tint = ErrorRed, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Switches List
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNotificationEnabledChange(!notificationEnabled) }
                .padding(vertical = 4.dp)
        ) {
            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationEnabledChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryDarkTeal
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("In-App Push Prompts", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Black)
                Text("Send visual in-app and push notification reminders", fontSize = 10.sp, color = GrayText)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAutoCreateSystemAlarmChange(!autoCreateSystemAlarm) }
                .padding(vertical = 4.dp)
        ) {
            Switch(
                checked = autoCreateSystemAlarm,
                onCheckedChange = onAutoCreateSystemAlarmChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PrimaryDarkTeal
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("System Device Alarm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Black)
                Text("Create an automatic ringtone alarm in your device clock", fontSize = 10.sp, color = GrayText)
            }
        }
    }
}
