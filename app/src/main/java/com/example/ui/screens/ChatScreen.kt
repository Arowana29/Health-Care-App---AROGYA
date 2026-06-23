package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.navigation.Screen
import com.example.ui.ChatMessage
import com.example.ui.HealthViewModel
import com.example.ui.MessageSender
import com.example.ui.components.TrilingualText
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController, viewModel: HealthViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val messages by viewModel.chatMessages.collectAsState()
    val isLoading by viewModel.isChatLoading.collectAsState()
    val selectedModel by viewModel.selectedAiModel.collectAsState()
    val selectedRole by viewModel.selectedAiRole.collectAsState()
    
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    // Available AI Models matching user constraints & SKILL.md rules
    val modelOptions = listOf(
        "gemini-3.5-flash" to "General AI (Gemini 3.5)",
        "gemini-3.1-pro-preview" to "Complex Reasoning (Pro)",
        "gemini-3.1-flash-lite-preview" to "Fast Response (Lite)"
    )
    
    // Available clinical/wellness personas
    val personaOptions = listOf(
        "general" to ("Arogya Assistant" to "General medical definitions, healthy habit tips, and wellness coaching."),
        "analyst" to ("Wallet Analyst" to "Contextual reports on your saved patient profiles, active medications, and blood trends."),
        "emergency" to ("Emergency Guide" to "Immediately actionable, high-contrast first aid guidance for priority triage."),
        "lankan" to ("Lankan Home Care" to "Empathetic family wellness combining local traditions, herbs, and national MOH guidance.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TrilingualText(
                        english = "Arogya AI Chatbot",
                        sinhala = "ආරෝග්‍යා AI උපදේශක",
                        tamil = "ஆரோக்கியா AI அரட்டை",
                        color = White,
                        scale = 1.05f
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.clearChatHistory()
                            Toast.makeText(context, "Chat history cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(LightGrayBackground)
        ) {
            
            // --- Model and Persona Configuration Card ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    
                    // --- SELECT MODEL ROW ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.SmartButton, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Model Selected:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        modelOptions.forEach { (modelId, displayName) ->
                            val isSelected = selectedModel == modelId
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BackgroundLightTeal else LightGrayBackground)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) PrimaryDarkTeal else Color.LightGray,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { viewModel.updateSelectedAiModel(modelId) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = displayName.substringBefore(" ("),
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) PrimaryDarkTeal else GrayText
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                    
                    // --- SELECT PERSONA / ROLE ROW ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clinical Persona / Role:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                    }
                    
                    ScrollableTabRow(
                        selectedTabIndex = personaOptions.indexOfFirst { it.first == selectedRole }.coerceAtLeast(0),
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        contentColor = PrimaryDarkTeal,
                        indicator = {},
                        divider = {}
                    ) {
                        personaOptions.forEach { (roleId, pair) ->
                            val (title, description) = pair
                            val isSelected = selectedRole == roleId
                            Tab(
                                selected = isSelected,
                                onClick = { 
                                    viewModel.updateSelectedAiRole(roleId)
                                    Toast.makeText(context, "$title Enabled", Toast.LENGTH_SHORT).show()
                                },
                                text = {
                                    Text(
                                        text = title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    )
                                },
                                selectedContentColor = PrimaryDarkTeal,
                                unselectedContentColor = GrayText,
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .background(
                                        if (isSelected) BackgroundLightTeal else Color.Transparent,
                                        RoundedCornerShape(16.dp)
                                    )
                            )
                        }
                    }
                    
                    // Selected Persona Description
                    val currentDesc = personaOptions.firstOrNull { it.first == selectedRole }?.second?.second ?: ""
                    Text(
                        text = currentDesc,
                        fontSize = 11.sp,
                        color = GrayText,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
            
            // --- CLINICAL DISCLAIMER ADVISORY ---
            Card(
                colors = CardDefaults.cardColors(containerColor = WarningLightAmber),
                border = BorderStroke(1.dp, IconOrange.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Advisory Warning",
                        tint = IconOrange,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Safety Note: Gemini AI provides educational assistance. It does not replace formal diagnoses or treatments from registered healthcare practitioners.",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Black
                    )
                }
            }

            // --- CHAT MESSAGE SCROLL THREAD ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (messages.isEmpty()) {
                    // Empty Chat Screen State
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(BackgroundLightTeal, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = null,
                                tint = PrimaryDarkTeal,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TrilingualText(
                            english = "Start an Arogya AI Consultation",
                            sinhala = "Arogya AI සාකච්ඡාවක් ආරම්භ කරන්න",
                            tamil = "உரையாடலைத் தொடங்குங்கள்",
                            color = PrimaryDarkTeal,
                            scale = 1.1f,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ask health queries, analyze cross-med interaction risks (using the Wallet Analyst persona), or lookup fast first-aid tips directly.",
                            fontSize = 13.sp,
                            color = GrayText,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        Text("Suggested Health Queries:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Black)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        // Clickable Suggestions
                        val suggestions = if (selectedRole == "analyst") {
                            listOf(
                                "Check interaction risks for my health medications",
                                "Verify warnings matches in my active user profile",
                                "Provide a vital logs analysis report summary"
                            )
                        } else {
                            listOf(
                                "What are standard home remedies for an upset stomach?",
                                "What are the early warning signs of high blood pressure?",
                                "First aid guide for an allergic reaction trigger"
                            )
                        }
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            suggestions.forEach { sugg ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            inputText = sugg
                                        },
                                    border = BorderStroke(0.5.dp, Color.LightGray)
                                ) {
                                    Text(
                                        text = "⚡ \"$sugg\"",
                                        fontSize = 12.sp,
                                        color = PrimaryDarkTeal,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // List of Dialog turns
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(messages) { msg ->
                            val isUser = msg.sender == MessageSender.USER
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Card(
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 16.dp
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isUser) PrimaryDarkTeal else White
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .padding(vertical = 2.dp)
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = if (isUser) "You" else "Arogya Advisor (${selectedModel.substringAfter("gemini-")})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUser) BackgroundLightTeal else PrimaryDarkTeal,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Text(
                                            text = msg.text,
                                            fontSize = 15.sp,
                                            lineHeight = 20.sp,
                                            color = if (isUser) White else Black
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // --- TYPING LOADER STATUS ---
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryDarkTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Arogya AI is retrieving safety guidelines & formulating medical diagnostics...",
                        fontSize = 12.sp,
                        color = GrayText,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // --- Quick prompt suggestions before entering message ---
            if (messages.isNotEmpty()) {
                val interactivePrompts = listOf(
                    "Interaction Check" to "Check cross-med interaction warnings",
                    "Status summary" to "Summarize my vital signs and doctor appointments",
                    "Remedies" to "Provide natural Sri Lankan remedies"
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    interactivePrompts.forEach { (shortname, prompt) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PastelGreen)
                                .clickable {
                                    inputText = prompt
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = shortname,
                                color = PrimaryDarkTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- INPUT PANEL BAR ---
            Card(
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Describe symptomatology / interact query...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryDarkTeal,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        maxLines = 4,
                        enabled = !isLoading
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val query = inputText
                                inputText = ""
                                viewModel.sendChatMessage(query) { error ->
                                    Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        containerColor = PrimaryDarkTeal,
                        contentColor = White,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("send_button")
                            .minimumInteractiveComponentSize(),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Message",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
