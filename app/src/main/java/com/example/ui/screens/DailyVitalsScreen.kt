package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.model.DailyVitals
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import com.example.ui.theme.*
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyVitalsScreen(navController: NavController, viewModel: HealthViewModel) {
    val vitalsList by viewModel.dailyVitals.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = PrimaryDarkTeal)
                        Text(" | ", color = PrimaryDarkTeal.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText(
                            english = "Daily Vitals",
                            sinhala = "දෛනික සෞඛ්‍ය දත්ත",
                            tamil = "தினசரி ஆரோக்கியம்",
                            color = PrimaryDarkTeal,
                            scale = 0.95f
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = PrimaryDarkTeal)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryDarkTeal,
                contentColor = White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Vitals")
            }
        },
        containerColor = LightGrayBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(vitalsList) { vital ->
                VitalCard(vital)
            }
        }
    }

    if (showAddDialog) {
        val context = LocalContext.current
        var dateStr by remember { mutableStateOf("") }
        var systolic by remember { mutableStateOf("") }
        var diastolic by remember { mutableStateOf("") }
        var heartRate by remember { mutableStateOf("") }

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            context,
            { _: DatePicker, y: Int, m: Int, d: Int ->
                val monthStr = (m + 1).toString().padStart(2, '0')
                val dayStr = d.toString().padStart(2, '0')
                dateStr = "$y-$monthStr-$dayStr"
            }, year, month, day
        )

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { 
                TrilingualText(
                    english = "Add Daily Vitals",
                    sinhala = "දෛනික වැදගත් ලකුණු එක් කරන්න",
                    tamil = "தினசரி ஆரோக்கிய அளவீடுகளைச் சேர்",
                    color = PrimaryDarkTeal,
                    scale = 1.1f
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = {},
                        label = { 
                            TrilingualText(
                                english = "Date",
                                sinhala = "දිනය",
                                tamil = "தேதி",
                                scale = 0.9f
                            )
                        },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = systolic,
                        onValueChange = { systolic = it },
                        label = { 
                            TrilingualText(
                                english = "Systolic Pressure",
                                sinhala = "සිස්ටොලික් පීඩනය",
                                tamil = "சிஸ்டாலிக் அழுத்தம்",
                                scale = 0.9f
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = diastolic,
                        onValueChange = { diastolic = it },
                        label = { 
                            TrilingualText(
                                english = "Diastolic Pressure",
                                sinhala = "ඩයස්ටොලික් පීඩනය",
                                tamil = "டயஸ்டாலிக் அழுத்தம்",
                                scale = 0.9f
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = heartRate,
                        onValueChange = { heartRate = it },
                        label = { 
                            TrilingualText(
                                english = "Heart Rate (bpm)",
                                sinhala = "හෘද ස්පන්දන වේගය (bpm)",
                                tamil = "இதயத் துடிப்பு விகிதம் (bpm)",
                                scale = 0.9f
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sys = systolic.toIntOrNull() ?: 0
                        val dia = diastolic.toIntOrNull() ?: 0
                        val hr = heartRate.toIntOrNull() ?: 0
                        val dt = if (dateStr.isNotEmpty()) dateStr else "Unknown Date"
                        viewModel.insertDailyVitals(
                            DailyVitals(
                                date = dt,
                                systolic = sys,
                                diastolic = dia,
                                heartRate = hr
                            )
                        )
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                ) {
                    TrilingualText(
                        english = "Save",
                        sinhala = "සුරකින්න",
                        tamil = "சேமி",
                        color = White,
                        scale = 0.9f
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    TrilingualText(
                        english = "Cancel",
                        sinhala = "අවලංගු කරන්න",
                        tamil = "ரத்துசெய்",
                        color = PrimaryDarkTeal,
                        scale = 0.9f
                    )
                }
            }
        )
    }
}

@Composable
fun VitalCard(vital: DailyVitals) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TrilingualText(
                    english = "Date: ",
                    sinhala = "දිනය: ",
                    tamil = "தேதி: ",
                    color = PrimaryDarkTeal,
                    scale = 0.95f
                )
                Text(vital.date, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = PrimaryDarkTeal)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TrilingualText(
                    english = "Blood Pressure:",
                    sinhala = "රුධිර පීඩනය:",
                    tamil = "இரத்த அழுத்தம்:",
                    color = GrayText,
                    scale = 0.95f
                )
                Text("${vital.systolic}/${vital.diastolic} mmHg", fontWeight = FontWeight.Bold, color = Black)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                TrilingualText(
                    english = "Heart Rate:",
                    sinhala = "හෘද ස්පන්දන වේගය:",
                    tamil = "இதயத் துடிப்பு:",
                    color = GrayText,
                    scale = 0.95f
                )
                Text("${vital.heartRate} bpm", fontWeight = FontWeight.Bold, color = Black)
            }
        }
    }
}
