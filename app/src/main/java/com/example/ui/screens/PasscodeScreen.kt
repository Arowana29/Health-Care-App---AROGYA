package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navigation.Screen
import com.example.ui.HealthViewModel
import com.example.ui.theme.PrimaryDarkTeal
import com.example.ui.theme.White
import com.example.ui.components.TrilingualText

@Composable
fun PasscodeScreen(navController: NavController, viewModel: HealthViewModel) {
    val existingPin = viewModel.appPin
    val isSettingPin = existingPin == null

    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(if (isSettingPin) 1 else 0) } // 0: enter, 1: create, 2: confirm
    var errorMessage by remember { mutableStateOf("") }
    var showEmergencyDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val titleEng = when (step) {
            0 -> "Enter Passcode"
            1 -> "Create a Passcode"
            2 -> "Confirm Passcode"
            else -> ""
        }
        val titleSi = when (step) {
            0 -> "මුරපදය ඇතුළත් කරන්න"
            1 -> "මුරපදයක් සාදන්න"
            2 -> "මුරපදය තහවුරු කරන්න"
            else -> ""
        }
        val titleTa = when (step) {
            0 -> "கடவுக்குறியீட்டை உள்ளிடவும்"
            1 -> "கடவுக்குறியீட்டை உருவாக்கவும்"
            2 -> "கடவுக்குறியீட்டை உறுதிப்படுத்தவும்"
            else -> ""
        }

        TrilingualText(
            english = titleEng,
            sinhala = titleSi,
            tamil = titleTa,
            scale = 1.3f,
            horizontalAlignment = Alignment.CenterHorizontally,
            color = PrimaryDarkTeal
        )
        
        if (errorMessage.isNotEmpty()) {
            val errSi = when(errorMessage) {
                "Incorrect Passcode." -> "මුරපදය වැරදියි."
                "Passcodes do not match." -> "මුරපද නොගැලපේ."
                else -> errorMessage
            }
            val errTa = when(errorMessage) {
                "Incorrect Passcode." -> "தவறான கடவுக்குறியீடு."
                "Passcodes do not match." -> "கடவுக்குறியீடுகள் பொருந்தவில்லை."
                else -> errorMessage
            }
            Spacer(modifier = Modifier.height(16.dp))
            TrilingualText(
                english = errorMessage,
                sinhala = errSi,
                tamil = errTa,
                scale = 0.9f,
                horizontalAlignment = Alignment.CenterHorizontally,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // PIN Indicators
        val currentInput = if (step == 0 || step == 1) pin else confirmPin
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (index < currentInput.length) PrimaryDarkTeal else Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        // Keypad
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf("", "0", "del")
        )

        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(0.8f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    if (key == "") {
                        Spacer(modifier = Modifier.size(64.dp))
                    } else if (key == "del") {
                        IconButton(
                            onClick = {
                                errorMessage = ""
                                if (step == 0 || step == 1) {
                                    if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                } else {
                                    if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                }
                            },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(Icons.Filled.Backspace, contentDescription = "Delete", tint = PrimaryDarkTeal)
                        }
                    } else {
                        Button(
                            onClick = {
                                errorMessage = ""
                                if (step == 0 || step == 1) {
                                    if (pin.length < 4) pin += key
                                    if (pin.length == 4) {
                                        if (step == 0) {
                                            if (pin == existingPin || (viewModel.familyPin != null && pin == viewModel.familyPin)) {
                                                navController.navigate(Screen.Home.route) {
                                                    popUpTo(Screen.Passcode.route) { inclusive = true }
                                                }
                                            } else {
                                                errorMessage = "Incorrect Passcode."
                                                pin = ""
                                            }
                                        } else if (step == 1) {
                                            step = 2
                                        }
                                    }
                                } else {
                                    if (confirmPin.length < 4) confirmPin += key
                                    if (confirmPin.length == 4) {
                                        if (pin == confirmPin) {
                                            viewModel.setAppPin(pin)
                                            navController.navigate(Screen.Home.route) {
                                                popUpTo(Screen.Passcode.route) { inclusive = true }
                                            }
                                        } else {
                                            errorMessage = "Passcodes do not match."
                                            pin = ""
                                            confirmPin = ""
                                            step = 1
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal, contentColor = White)
                        ) {
                            Text(key, fontSize = 24.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (step == 0) {
            TextButton(onClick = { 
                viewModel.setAppPin("") // allow reset for simplicity in this demo if they forget
                pin = ""
                step = 1 
            }) {
                TrilingualText(
                    english = "Forgot Passcode? Reset",
                    sinhala = "මුරපදය අමතකද? යළි සකසන්න",
                    tamil = "கடவுக்குறியீடு மறந்துவிட்டதா? மீட்டமைக்கவும்",
                    color = PrimaryDarkTeal,
                    horizontalAlignment = Alignment.CenterHorizontally
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showEmergencyDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Warning, contentDescription = null, tint = White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("EMERGENCY FAMILY BYPASS", fontWeight = FontWeight.Bold, color = White)
            }
        }
    }

    if (showEmergencyDialog) {
        val bypassEnabled = viewModel.isEmergencyBypassEnabled
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                TrilingualText(
                    english = "Emergency Family Access Portal",
                    sinhala = "හදිසි පවුලේ ප්‍රවේශය",
                    tamil = "அவசர குடும்ப அணுகல்",
                    color = MaterialTheme.colorScheme.error,
                    scale = 1.05f
                )
            },
            text = {
                Column {
                    Text(
                        text = if (bypassEnabled) {
                            "The patient has enabled PASSWORDLESS Emergency Family Access bypass in settings.\n\n" +
                            "Since they are admitted to the hospital or unwell, you are authorized to temporarily unlock the app to show profiles, active medications, allergic intolerances, and Medical Pass (QR/PDF) to clinical examiners and doctors."
                        } else {
                            "Passwordless Emergency Bypass is currently DISABLED.\n\n" +
                            "To gain access to vital logs, medical dossiers, and allergic warnings, please enter the Secondary Family Passcode (provided by the patient or family), or contact them directly.\n\n" +
                            "If you do not have the passcode, please ask the doctors to scan their physical/printed Medical Pass QR code for quick clinical extraction."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                if (bypassEnabled) {
                    Button(
                        onClick = {
                            showEmergencyDialog = false
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Passcode.route) { inclusive = true }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Filled.LockOpen, contentDescription = null, tint = White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock Active Wallet")
                    }
                } else {
                    Button(
                        onClick = { showEmergencyDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                    ) {
                        Text("Unlock with PIN")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Close", color = PrimaryDarkTeal)
                }
            }
        )
    }
}
