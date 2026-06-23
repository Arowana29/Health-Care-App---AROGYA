package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.draw.clip
import com.example.ui.theme.BackgroundLightTeal
import com.example.ui.theme.PrimaryDarkTeal
import com.example.ui.theme.White
import com.example.ui.theme.Black
import com.example.ui.theme.IconOrange
import com.example.data.model.DoctorVisit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitsScreen(navController: NavController, viewModel: HealthViewModel) {
    val visits by viewModel.visits.collectAsState()
    val documents by viewModel.documents.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Doctor Visits", "රෝහල් පැමිණීම්", "மருத்துவர் வருகைகள்", color = White, scale = 0.95f)
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
                Icon(Icons.Filled.Add, "Add Visit")
            }
        }
    ) { padding ->
        if (visits.isEmpty()) {
            Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                TrilingualText("No visits yet", "තවම පැමිණීම් නැත", "இதுவரை வருகைகள் இல்லை", color = PrimaryDarkTeal, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally)
            }
        } else {
            LazyColumn(modifier = Modifier.padding(padding).padding(16.dp)) {
                items(visits) { visit ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(visit.doctorName, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal, fontSize = 18.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.LocalHospital, contentDescription = null, tint = Black.copy(alpha=0.6f), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("${visit.hospital} • ${visit.speciality}", style = MaterialTheme.typography.bodyMedium, color = Black.copy(alpha=0.8f))
                            }
                            
                            Spacer(Modifier.height(4.dp))
                            Text("Date: ${visit.date}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.6f), modifier = Modifier.padding(start = 24.dp))
                            
                            if (visit.reason.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Reason: ${visit.reason}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.8f), modifier = Modifier.padding(start = 24.dp))
                            }
                            if (visit.notes.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Notes: ${visit.notes}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.8f), modifier = Modifier.padding(start = 24.dp))
                            }
                            
                            val linkedIds = visit.linkedDocumentIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toIntOrNull() }
                            if (linkedIds.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                HorizontalDivider(color = PrimaryDarkTeal.copy(alpha = 0.2f))
                                Spacer(Modifier.height(8.dp))
                                Text("Linked Documents:", style = MaterialTheme.typography.labelMedium, color = PrimaryDarkTeal)
                                Spacer(Modifier.height(4.dp))
                                val linkedDocs = documents.filter { it.id in linkedIds }
                                linkedDocs.forEach { doc ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp, horizontal = 0.dp)) {
                                        Icon(Icons.Filled.Description, contentDescription = null, tint = IconOrange, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("${doc.type} - ${doc.date}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AddVisitDialog(
                documents = documents,
                onDismiss = { showAddDialog = false },
                onSave = { visit ->
                    viewModel.insertVisit(visit)
                    showAddDialog = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddVisitDialog(
    documents: List<com.example.data.model.MedicalDocument>,
    onDismiss: () -> Unit,
    onSave: (DoctorVisit) -> Unit
) {
    var doctorName by remember { mutableStateOf("") }
    var speciality by remember { mutableStateOf("") }
    var hospital by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Set of selected document IDs
    var selectedDocs by remember { mutableStateOf(setOf<Int>()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Log Doctor Visit", color = PrimaryDarkTeal, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = doctorName,
                    onValueChange = { doctorName = it },
                    label = { Text("Doctor Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = speciality,
                    onValueChange = { speciality = it },
                    label = { Text("Speciality") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Hospital / Clinic") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Visit") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    maxLines = 3
                )
                
                if (documents.isNotEmpty()) {
                    Text("Link Documents", fontWeight = FontWeight.SemiBold, color = PrimaryDarkTeal, modifier = Modifier.padding(bottom = 8.dp))
                    documents.forEach { doc ->
                        val isSelected = selectedDocs.contains(doc.id)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDocs = if (isSelected) {
                                        selectedDocs - doc.id
                                    } else {
                                        selectedDocs + doc.id
                                    }
                                }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                colors = CheckboxDefaults.colors(checkedColor = PrimaryDarkTeal)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(doc.type, fontWeight = FontWeight.Medium, color = Black.copy(alpha=0.8f))
                                Text("Date: ${doc.date}", style = MaterialTheme.typography.bodySmall, color = Black.copy(alpha=0.6f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        DoctorVisit(
                            doctorName = doctorName,
                            speciality = speciality,
                            hospital = hospital,
                            date = date,
                            reason = reason,
                            notes = notes,
                            linkedDocumentIds = selectedDocs.joinToString(",")
                        )
                    )
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
