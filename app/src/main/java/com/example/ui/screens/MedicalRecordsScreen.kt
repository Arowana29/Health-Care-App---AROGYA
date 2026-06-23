package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundLightTeal
import com.example.ui.theme.PrimaryDarkTeal
import com.example.ui.theme.White
import com.example.ui.theme.Black
import com.example.data.model.MedicalRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRecordsScreen(navController: NavController, viewModel: HealthViewModel) {
    val records by viewModel.medicalRecords.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredRecords = records.filter { record ->
        record.diagnosis.contains(searchQuery, ignoreCase = true) ||
        record.doctorName.contains(searchQuery, ignoreCase = true) ||
        record.date.contains(searchQuery, ignoreCase = true) ||
        record.notes.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Medical Records", "වෛද්‍ය වාර්තා", "மருத்துவ பதிவுகள்", color = White, scale = 0.95f)
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
                contentColor = White
            ) {
                Icon(Icons.Filled.Add, "Add Record")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search records...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryDarkTeal,
                    unfocusedBorderColor = PrimaryDarkTeal.copy(alpha = 0.5f)
                ),
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = PrimaryDarkTeal)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear search", tint = PrimaryDarkTeal)
                        }
                    }
                }
            )

            if (records.isEmpty()) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    TrilingualText("No records yet", "තවම වාර්තා නැත", "இதுவரை பதிவுகள் இல்லை", color = PrimaryDarkTeal, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally)
                }
            } else if (filteredRecords.isEmpty()) {
                Column(modifier = Modifier.weight(1f).fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                     TrilingualText("No matching records found", "ගැලපෙන වාර්තා හමු නොවීය", "பொருத்தமான பதிவுகள் கண்டறியப்படவில்லை", color = PrimaryDarkTeal, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally)
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
                    items(filteredRecords) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(record.diagnosis, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            
                            Text("Date: ${record.date}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.6f), modifier = Modifier.padding(start = 28.dp))
                            
                            Spacer(Modifier.height(4.dp))
                            Text("Doctor: ${record.doctorName}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.8f), modifier = Modifier.padding(start = 28.dp))
                            
                            if (record.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Notes: ${record.notes}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.8f), modifier = Modifier.padding(start = 28.dp))
                            }
                        }
                    }
                }
            }
        }
        }
        
        if (showAddDialog) {
            AddMedicalRecordDialog(
                onDismiss = { showAddDialog = false },
                onSave = { record ->
                    viewModel.insertMedicalRecord(record)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedicalRecordDialog(
    onDismiss: () -> Unit,
    onSave: (MedicalRecord) -> Unit
) {
    var diagnosis by remember { mutableStateOf("") }
    var doctorName by remember { mutableStateOf("") }
    var rawDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { TrilingualText("Add Record", "වාර්තාව එකතු කරන්න", "பதிவை சேர்க்க", color = PrimaryDarkTeal, scale = 1.0f) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = diagnosis,
                    onValueChange = { diagnosis = it },
                    label = { Text("Diagnosis / Illness") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryDarkTeal)
                )
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryDarkTeal)
                )
                OutlinedTextField(
                    value = rawDate,
                    onValueChange = { rawDate = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryDarkTeal)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryDarkTeal),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (diagnosis.isNotBlank() && doctorName.isNotBlank() && rawDate.isNotBlank()) {
                        onSave(
                            MedicalRecord(
                                diagnosis = diagnosis,
                                doctorName = doctorName,
                                date = rawDate,
                                notes = notes
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryDarkTeal)
            }
        }
    )
}
