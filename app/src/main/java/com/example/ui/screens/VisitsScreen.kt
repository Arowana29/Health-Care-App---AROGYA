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
import androidx.compose.material.icons.filled.Edit
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
    var selectedVisit by remember { mutableStateOf<DoctorVisit?>(null) }

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
                onClick = {
                    selectedVisit = null
                    showAddDialog = true
                },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .clickable {
                                selectedVisit = visit
                                showAddDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Filled.MedicalServices, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(visit.doctorName, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal, fontSize = 18.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryDarkTeal.copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = "Edit Log", tint = PrimaryDarkTeal, modifier = Modifier.size(14.dp))
                                }
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
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .fillMaxHeight(0.9f)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .padding(vertical = 12.dp, horizontal = 4.dp)
                ) {
                    com.example.ui.components.ResponsiveDoctorVisitForm(
                        selectedVisit = selectedVisit,
                        documents = documents,
                        onSave = { visit ->
                            viewModel.insertVisit(visit)
                            showAddDialog = false
                        },
                        onCancel = { showAddDialog = false },
                        onDelete = { visit ->
                            viewModel.deleteVisit(visit)
                            showAddDialog = false
                        }
                    )
                }
            }
        }
    }
}
