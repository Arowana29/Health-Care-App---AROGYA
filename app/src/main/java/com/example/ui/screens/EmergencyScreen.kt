package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import androidx.compose.ui.Alignment
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(navController: NavController, viewModel: HealthViewModel) {
    val profile by viewModel.profile.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = ErrorRed)
                        Text(" | ", color = ErrorRed.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        Text("Emergency Card", color = ErrorRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = ErrorRed)
                    }
                },
                actions = {
                    com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = ErrorRed)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AlertLightRed)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Card(
                border = BorderStroke(3.dp, ErrorRed),
                colors = CardDefaults.cardColors(containerColor = White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text((profile?.name?.ifEmpty { "Kamal Perera" }) ?: "Kamal Perera", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            TrilingualText("Blood Group", "රුධිර වර්ගය", "இரத்த வகை", color = GrayText)
                            Text((profile?.bloodGroup?.ifEmpty { "O+" }) ?: "O+", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    TrilingualText("Allergies", "අසාත්මිකතා", "ஒவ்வாமை", color = GrayText)
                    Text((profile?.allergies?.ifEmpty { "Penicillin, Dust" }) ?: "Penicillin, Dust", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))
                    TrilingualText("Emergency Contact", "හදිසි ඇමතුම", "அவசர தொடர்பு", color = GrayText)
                    Text("Nimal Perera: 077 123 4567", fontSize = 18.sp, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(16.dp))
                    TrilingualText("Critical Medications", "අත්‍යවශ්‍ය ඖෂධ", "முக்கிய மருந்துகள்", color = GrayText)
                    Text("Metformin 500mg, Atorvastatin 20mg", fontSize = 16.sp)
                }
            }
        }
    }
}
