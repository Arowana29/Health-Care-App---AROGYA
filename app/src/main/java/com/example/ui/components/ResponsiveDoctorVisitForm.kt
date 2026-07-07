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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DoctorVisit
import com.example.data.model.MedicalDocument
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResponsiveDoctorVisitForm(
    selectedVisit: DoctorVisit?,
    documents: List<MedicalDocument>,
    onSave: (DoctorVisit) -> Unit,
    onCancel: () -> Unit,
    onDelete: ((DoctorVisit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 1. Local State initialized from selectedVisit
    var doctorName by remember { mutableStateOf(selectedVisit?.doctorName ?: "") }
    var speciality by remember { mutableStateOf(selectedVisit?.speciality ?: "") }
    var hospital by remember { mutableStateOf(selectedVisit?.hospital ?: "") }
    var date by remember { mutableStateOf(selectedVisit?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var reason by remember { mutableStateOf(selectedVisit?.reason ?: "") }
    var notes by remember { mutableStateOf(selectedVisit?.notes ?: "") }
    
    val initialDocs = selectedVisit?.linkedDocumentIds
    var selectedDocs by remember {
        mutableStateOf(if (initialDocs.isNullOrEmpty()) setOf<Int>() else initialDocs.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet())
    }

    // Validation State
    var showValidationError by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }

    // Date Picker State
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // Specialty & Hospital suggestions
    val specialtySuggestions = listOf(
        "General Practitioner", "Cardiologist", "Dentist", "Pediatrician", 
        "Orthopedic", "Dermatologist", "Neurologist", "ENT Specialist"
    )
    val hospitalSuggestions = listOf(
        "General Hospital", "City Clinic", "Asiri Health", "National Hospital", 
        "Family Care Center", "Lanka Hospitals"
    )

    // Formatter for Selected Date in DatePicker
    fun parseAndFormatDate(millis: Long?): String {
        if (millis == null) return ""
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date(millis))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    date = parseAndFormatDate(datePickerState.selectedDateMillis)
                    showDatePicker = false
                }) {
                    Text("Select", color = PrimaryDarkTeal, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = PrimaryDarkTeal)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    titleContentColor = PrimaryDarkTeal,
                    headlineContentColor = PrimaryDarkTeal,
                    selectedDayContainerColor = PrimaryDarkTeal,
                    selectedDayContentColor = Color.White
                )
            )
        }
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header Section
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
                        imageVector = Icons.Filled.MedicalServices,
                        contentDescription = null,
                        tint = PrimaryDarkTeal,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    TrilingualText(
                        english = if (selectedVisit == null) "Log Doctor Visit" else "Edit Visit Details",
                        sinhala = if (selectedVisit == null) "වෛද්‍ය හමුව සටහන් කරන්න" else "හමුවීමේ විස්තර වෙනස් කරන්න",
                        tamil = if (selectedVisit == null) "மருத்துவர் வருகையை பதியவும்" else "வருகை விவரங்களைத் திருத்தவும்",
                        color = PrimaryDarkTeal,
                        scale = 1.05f
                    )
                    Text(
                        text = "Enter clinical guidelines, treatment remarks, and document records.",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }

            Divider(color = LightGrayBackground, modifier = Modifier.padding(bottom = 16.dp))

            // Animated Validation Warning if Any
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

            // High Density Multi-Column Responsive Grid Layout
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth > 600.dp

                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Left Column
                        Column(modifier = Modifier.weight(1f)) {
                            DoctorVisitInputsLeft(
                                doctorName = doctorName,
                                onDoctorNameChange = { doctorName = it; showValidationError = false },
                                speciality = speciality,
                                onSpecialityChange = { speciality = it; showValidationError = false },
                                specialtySuggestions = specialtySuggestions,
                                hospital = hospital,
                                onHospitalChange = { hospital = it },
                                hospitalSuggestions = hospitalSuggestions
                            )
                        }

                        // Right Column
                        Column(modifier = Modifier.weight(1f)) {
                            DoctorVisitInputsRight(
                                date = date,
                                onDateClick = { showDatePicker = true },
                                reason = reason,
                                onReasonChange = { reason = it },
                                notes = notes,
                                onNotesChange = { notes = it },
                                documents = documents,
                                selectedDocs = selectedDocs,
                                onSelectedDocsChange = { selectedDocs = it }
                            )
                        }
                    }
                } else {
                    // Portrait layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        DoctorVisitInputsLeft(
                            doctorName = doctorName,
                            onDoctorNameChange = { doctorName = it; showValidationError = false },
                            speciality = speciality,
                            onSpecialityChange = { speciality = it; showValidationError = false },
                            specialtySuggestions = specialtySuggestions,
                            hospital = hospital,
                            onHospitalChange = { hospital = it },
                            hospitalSuggestions = hospitalSuggestions
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        DoctorVisitInputsRight(
                            date = date,
                            onDateClick = { showDatePicker = true },
                            reason = reason,
                            onReasonChange = { reason = it },
                            notes = notes,
                            onNotesChange = { notes = it },
                            documents = documents,
                            selectedDocs = selectedDocs,
                            onSelectedDocsChange = { selectedDocs = it }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedVisit != null && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(selectedVisit) },
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete Visit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
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
                        if (doctorName.isBlank()) {
                            validationMessage = "Doctor Name is critical and cannot be blank."
                            showValidationError = true
                        } else if (speciality.isBlank()) {
                            validationMessage = "Doctor Specialty is required (e.g., General Practitioner)."
                            showValidationError = true
                        } else if (date.isBlank()) {
                            validationMessage = "Date of visit is required."
                            showValidationError = true
                        } else {
                            val savedVisit = DoctorVisit(
                                id = selectedVisit?.id ?: 0,
                                doctorName = doctorName,
                                speciality = speciality,
                                hospital = hospital,
                                date = date,
                                reason = reason,
                                notes = notes,
                                linkedDocumentIds = selectedDocs.joinToString(",")
                            )
                            onSave(savedVisit)
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
                    Text("Save Visit Logs", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorVisitInputsLeft(
    doctorName: String,
    onDoctorNameChange: (String) -> Unit,
    speciality: String,
    onSpecialityChange: (String) -> Unit,
    specialtySuggestions: List<String>,
    hospital: String,
    onHospitalChange: (String) -> Unit,
    hospitalSuggestions: List<String>
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Doctor Name
        Text("Doctor's Name *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = doctorName,
            onValueChange = onDoctorNameChange,
            placeholder = { Text("e.g. Dr. K. Silva", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = PrimaryDarkTeal) },
            trailingIcon = if (doctorName.isNotEmpty()) {
                { IconButton(onClick = { onDoctorNameChange("") }) { Icon(Icons.Filled.Cancel, contentDescription = "Clear", tint = GrayText) } }
            } else null,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Specialty
        Text("Specialty *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = speciality,
            onValueChange = onSpecialityChange,
            placeholder = { Text("e.g. Cardiologist, Dentist", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.FolderSpecial, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Quick Specialty chips (Ensure min touch targets)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(specialtySuggestions) { suggestion ->
                val isSelected = speciality == suggestion
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                        .clickable { onSpecialityChange(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 6.dp) // Generous 48dp height overall feel
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

        // 3. Hospital / Clinic
        Text("Hospital / Clinic Name", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = hospital,
            onValueChange = onHospitalChange,
            placeholder = { Text("e.g. General Hospital / City Clinic", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Hospital Quick Presets
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(hospitalSuggestions) { suggestion ->
                val isSelected = hospital == suggestion
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                        .clickable { onHospitalChange(suggestion) }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoctorVisitInputsRight(
    date: String,
    onDateClick: () -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    documents: List<MedicalDocument>,
    selectedDocs: Set<Int>,
    onSelectedDocsChange: (Set<Int>) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Visit Date Selector
        Text("Date of Visit *", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(LightGrayBackground.copy(alpha = 0.5f))
                .border(1.dp, PrimaryDarkTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable { onDateClick() }
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
                        text = date,
                        fontSize = 14.sp,
                        color = Black,
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = PrimaryDarkTeal)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // 2. Reason for Visit
        Text("Reason for Visit", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = reason,
            onValueChange = onReasonChange,
            placeholder = { Text("e.g. Annual Checkup / Sudden fever", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Help, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 3. Clinical Notes
        Text("Clinical Notes / Instructions", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
        TextField(
            value = notes,
            onValueChange = onNotesChange,
            placeholder = { Text("Enter doctor's warnings, next advice, or general notes here...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Filled.Description, contentDescription = null, tint = PrimaryDarkTeal) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            shape = RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Black,
                unfocusedTextColor = Black,
                focusedContainerColor = BackgroundLightTeal.copy(alpha = 0.4f),
                unfocusedContainerColor = LightGrayBackground.copy(alpha = 0.5f),
                focusedIndicatorColor = PrimaryDarkTeal,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(modifier = Modifier.height(14.dp))

        // 4. Link Medical Records Group if Any exist
        if (documents.isNotEmpty()) {
            Text("Link Related Medical Documents", fontSize = 12.sp, fontWeight = FontWeight.Black, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(LightGrayBackground.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .border(1.dp, LightGrayBackground, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                documents.forEach { doc ->
                    val isChecked = selectedDocs.contains(doc.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                val updated = if (isChecked) selectedDocs - doc.id else selectedDocs + doc.id
                                onSelectedDocsChange(updated)
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = {
                                val updated = if (isChecked) selectedDocs - doc.id else selectedDocs + doc.id
                                onSelectedDocsChange(updated)
                            },
                            colors = CheckboxDefaults.colors(checkedColor = PrimaryDarkTeal)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(doc.type, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                            Text("Dated ${doc.date}", fontSize = 10.sp, color = GrayText)
                        }
                    }
                }
            }
        }
    }
}
