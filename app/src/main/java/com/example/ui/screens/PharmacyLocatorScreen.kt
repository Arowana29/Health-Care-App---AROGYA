package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.data.api.NominatimClient
import com.example.data.api.OverpassClient
import com.example.data.api.OverpassElement
import com.example.ui.HealthViewModel
import com.example.ui.components.AppTitleText
import com.example.ui.components.TrilingualText
import com.example.ui.theme.BackgroundLightTeal
import com.example.ui.theme.PrimaryDarkTeal
import com.example.ui.theme.White
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyLocatorScreen(navController: NavController, viewModel: HealthViewModel) {
    val profile by viewModel.profile.collectAsState()
    var locationQuery by remember { mutableStateOf(profile?.location ?: "Colombo") }
    var pharmacies by remember { mutableStateOf<List<com.example.data.model.SavedPharmacy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (locationQuery.isNotBlank()) {
            searchPharmacies(viewModel, locationQuery) { result, err ->
                pharmacies = result
                errorMessage = err
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppTitleText(scale = 0.85f, color = White)
                        Text(" | ", color = White.copy(alpha = 0.5f), fontSize = 16.sp, modifier = Modifier.padding(horizontal = 4.dp))
                        TrilingualText("Pharmacies", "ෆාමසි", "மருந்தகங்கள்", color = White, scale = 0.95f)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryDarkTeal)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            Text("Find Pharmacies & ePharmacies", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = { Text("Town / Location (Sri Lanka)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            searchPharmacies(viewModel, locationQuery) { result, err ->
                                pharmacies = result
                                errorMessage = err
                                isLoading = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                    modifier = Modifier.height(56.dp).padding(top = 8.dp)
                ) {
                    Icon(Icons.Filled.Search, contentDescription = "Search", tint = White)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryDarkTeal)
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            } else if (pharmacies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pharmacies found in this area.", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(pharmacies) { pharmacy ->
                        PharmacyCard(pharmacy)
                    }
                }
            }
        }
    }
}

suspend fun searchPharmacies(viewModel: HealthViewModel, town: String, onResult: (List<com.example.data.model.SavedPharmacy>, String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            // First get location coordinates using Nominatim
            val searchResults = NominatimClient.service.searchLocation("$town, Sri Lanka")
            if (searchResults.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult(emptyList(), "Location not found in Sri Lanka. Try another town name.")
                }
                return@withContext
            }
            
            val lat = searchResults[0].lat.toDouble()
            val lon = searchResults[0].lon.toDouble()
            
            // Now search for pharmacies in a 5km radius
            val query = "[out:json][timeout:25];node(around:5000,$lat,$lon)[\"amenity\"=\"pharmacy\"];out;"
            val overpassResults = OverpassClient.service.getPharmacies(query)
            
            val moshi = com.squareup.moshi.Moshi.Builder().build()
            val adapter = moshi.adapter<Map<String, String>>(
                com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            )

            val savedPharmacies = overpassResults.elements.map { element ->
                com.example.data.model.SavedPharmacy(
                    id = element.id,
                    name = element.tags?.get("name") ?: element.tags?.get("name:en") ?: "Unknown Pharmacy",
                    type = element.type,
                    lat = element.lat,
                    lon = element.lon,
                    tagsJson = element.tags?.let { adapter.toJson(it) },
                    town = town,
                    timestamp = System.currentTimeMillis()
                )
            }
            viewModel.savePharmacies(savedPharmacies)
            
            withContext(Dispatchers.Main) {
                onResult(savedPharmacies, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            viewModel.getSavedPharmaciesByTown(town) { cached ->
                if (cached.isNotEmpty()) {
                    onResult(cached, "Network Error. Showing offline saved results for '$town'")
                } else {
                    onResult(emptyList(), "Network Error: ${e.message}\nNo offline data found for '$town'.")
                }
            }
        }
    }
}

@Composable
fun PharmacyCard(pharmacy: com.example.data.model.SavedPharmacy) {
    val moshi = remember { com.squareup.moshi.Moshi.Builder().build() }
    val adapter = remember(moshi) { 
        moshi.adapter<Map<String, String>>(com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    }
    val tags = pharmacy.tagsJson?.let { try { adapter.fromJson(it) } catch(e: Exception) { null } }
    
    val name = pharmacy.name
    val phone = tags?.get("phone") ?: tags?.get("contact:phone")
    val openingHours = tags?.get("opening_hours")
    val website = tags?.get("website") ?: tags?.get("contact:website")
    val distance = tags?.get("distance")
    
    val is24x7 = openingHours?.contains("24/7", ignoreCase = true) == true
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalPharmacy, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(name, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal, fontSize = 18.sp, modifier = Modifier.weight(1f))
            }
            
            Spacer(Modifier.height(8.dp))
            
            if (is24x7) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color(0xFFE81E63), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("24x7 Open Pharmacy", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE81E63), fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(4.dp))
            } else if (openingHours != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Hours: $openingHours", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
                Spacer(Modifier.height(4.dp))
            }
            
            if (phone != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(phone, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
                Spacer(Modifier.height(4.dp))
            }
            
            if (website != null) {
                Spacer(Modifier.height(4.dp))
                Text("Website: $website", style = MaterialTheme.typography.bodySmall, color = PrimaryDarkTeal)
            }
        }
    }
}
