package com.example.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
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
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.data.api.NominatimClient
import com.example.data.api.OverpassClient
import com.example.ui.HealthViewModel
import com.example.ui.components.AppTitleText
import com.example.ui.components.TrilingualText
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PharmacyLocatorScreen(navController: NavController, viewModel: HealthViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val profile by viewModel.profile.collectAsState()
    
    // UI state
    var locationQuery by remember { mutableStateOf(profile?.location ?: "Colombo") }
    var pharmacies by remember { mutableStateOf<List<com.example.data.model.SavedPharmacy>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // GPS coordinate details
    var userLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var gpsStatusText by remember { mutableStateOf<String?>(null) }
    
    // Default to 10 Km as requested by user
    var radiusKm by remember { mutableStateOf(10) } 
    var filterNightOpenOnly by remember { mutableStateOf(false) }

    // Kick off automatic initial search on load
    LaunchedEffect(Unit) {
        if (locationQuery.isNotBlank()) {
            isLoading = true
            searchPharmacies(viewModel, locationQuery, radiusInMeters = radiusKm * 1000) { result, err ->
                pharmacies = result
                errorMessage = err
                isLoading = false
            }
        }
    }

    // GPS location permissions request launcher
    val locationPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            gpsStatusText = "Retrieving GPS coordinates..."
            isLoading = true
            getCurrentLocation(context, { lat, lon ->
                userLocation = Pair(lat, lon)
                gpsStatusText = null
                coroutineScope.launch {
                    searchPharmaciesByCoords(viewModel, lat, lon, radiusInMeters = radiusKm * 1000) { result, err ->
                        pharmacies = result
                        errorMessage = err
                        isLoading = false
                    }
                }
            }, { err ->
                gpsStatusText = null
                errorMessage = err
                isLoading = false
            })
        } else {
            errorMessage = "Location precision permissions are required to seek nearby pharmacies. Please grant them in System Settings."
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = PrimaryDarkTeal,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "10Km GPS Pharmacy Search",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryDarkTeal
                    )
                    Text(
                        text = "Scan OpenStreetMap details for late-night open options and exact distance.",
                        fontSize = 11.sp,
                        color = GrayText
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            // Town/GPS Selection Form Block
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = locationQuery,
                            onValueChange = { 
                                locationQuery = it 
                                userLocation = null // reset GPS coordinates since user manually typed a town
                            },
                            label = { Text("Town / City (e.g. Colombo)") },
                            leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, tint = PrimaryDarkTeal) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryDarkTeal,
                                focusedLabelColor = PrimaryDarkTeal
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                                userLocation = null
                                coroutineScope.launch {
                                    searchPharmacies(viewModel, locationQuery, radiusInMeters = radiusKm * 1000) { result, err ->
                                        pharmacies = result
                                        errorMessage = err
                                        isLoading = false
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                            modifier = Modifier.height(56.dp)
                        ) {
                            Icon(Icons.Filled.Search, contentDescription = "Search", tint = White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // GPS integration Option
                    Button(
                        onClick = {
                            val fineGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarseGranted = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fineGranted || coarseGranted) {
                                gpsStatusText = "Connecting GPS receiver..."
                                isLoading = true
                                getCurrentLocation(context, { lat, lon ->
                                    userLocation = Pair(lat, lon)
                                    locationQuery = "My Current GPS Location"
                                    gpsStatusText = null
                                    coroutineScope.launch {
                                        searchPharmaciesByCoords(viewModel, lat, lon, radiusInMeters = radiusKm * 1000) { result, err ->
                                            pharmacies = result
                                            errorMessage = err
                                            isLoading = false
                                        }
                                    }
                                }, { err ->
                                    gpsStatusText = null
                                    errorMessage = err
                                    isLoading = false
                                })
                            } else {
                                locationPermissionsLauncher.launch(
                                    arrayOf(
                                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                                        android.Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(Icons.Filled.MyLocation, contentDescription = null, tint = White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search Nearby with Live GPS technology", fontWeight = FontWeight.Bold, color = White)
                    }

                    if (gpsStatusText != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = gpsStatusText!!, fontSize = 11.sp, color = PrimaryDarkTeal, fontWeight = FontWeight.Medium)
                    }
                    if (userLocation != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "✔️ GPS Pinpoint Resolved: Lat ${String.format("%.4f", userLocation!!.first)}, Lon ${String.format("%.4f", userLocation!!.second)}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Settings Picker Controls (Customizable Radius selector & Night Only toggle)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Radius Selection Row
                Column {
                    Text("Search Radius Limit:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val radiusOptions = listOf(5, 10, 15, 20)
                        radiusOptions.forEach { r ->
                            val isSelected = radiusKm == r
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryDarkTeal else BackgroundLightTeal)
                                    .clickable { radiusKm = r }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$r Km" + if (r == 10) " (Default)" else "",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) White else PrimaryDarkTeal
                                )
                            }
                        }
                    }
                }

                // Late Night Open toggle chip
                Column(horizontalAlignment = Alignment.End) {
                    Text("Service Filter:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal)
                    Spacer(modifier = Modifier.height(4.dp))
                    FilterChip(
                        selected = filterNightOpenOnly,
                        onClick = { filterNightOpenOnly = !filterNightOpenOnly },
                        label = { Text("🌙 Night Open / 24x7", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF6B11A1).copy(alpha = 0.15f),
                            selectedLabelColor = Color(0xFF6B11A1),
                            selectedLeadingIconColor = Color(0xFF6B11A1)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter results list client-side
            val filteredPharmacies = remember(pharmacies, filterNightOpenOnly) {
                if (filterNightOpenOnly) {
                    pharmacies.filter { ph ->
                        val moshi = com.squareup.moshi.Moshi.Builder().build()
                        val adapter = moshi.adapter<Map<String, String>>(com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
                        val tags = ph.tagsJson?.let { try { adapter.fromJson(it) } catch(e: Exception) { null } }
                        
                        val openingHours = tags?.get("opening_hours")
                        val nightService = tags?.get("night_service") ?: tags?.get("emergency") ?: tags?.get("opening_hours:covid19")
                        
                        val is24x7 = openingHours?.contains("24/7", ignoreCase = true) == true ||
                                     openingHours?.contains("24 hours", ignoreCase = true) == true ||
                                     openingHours?.contains("00:00-24:00", ignoreCase = true) == true
                        val isNightOpen = is24x7 || 
                                          openingHours?.contains("night", ignoreCase = true) == true ||
                                          openingHours?.contains("sunset", ignoreCase = true) == true ||
                                          nightService?.contains("yes", ignoreCase = true) == true ||
                                          nightService?.contains("night", ignoreCase = true) == true ||
                                          tags?.get("emergency")?.contains("yes", ignoreCase = true) == true
                        isNightOpen
                    }
                } else {
                    pharmacies
                }
            }

            // Results List, Loading indicator or error info state
            if (isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryDarkTeal)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Reading Live OSM Heath Satellite Maps...", fontSize = 12.sp, color = GrayText)
                    }
                }
            } else if (errorMessage != null) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(errorMessage!!, color = ErrorRed, modifier = Modifier.padding(16.dp))
                }
            } else if (filteredPharmacies.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = PrimaryDarkTeal.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (filterNightOpenOnly) 
                                "No night-open / 24x7 pharmacies in this $radiusKm Km radius." 
                            else 
                                "No pharmacies matched your search in a $radiusKm Km radius.",
                            fontWeight = FontWeight.Bold,
                            color = GrayText,
                            fontSize = 14.sp
                        )
                        if (filterNightOpenOnly) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Try turning off 'Night Open / 24x7' toggles. Some locations might have late hours without precise label descriptors in OpenStreetMap public models.",
                                fontSize = 11.sp,
                                color = GrayText,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    items(filteredPharmacies) { pharmacy ->
                        PharmacySmartCard(pharmacy, userLocation)
                    }
                }
            }
        }
    }
}

@Composable
fun PharmacySmartCard(pharmacy: com.example.data.model.SavedPharmacy, userCoordinates: Pair<Double, Double>?) {
    val moshi = remember { com.squareup.moshi.Moshi.Builder().build() }
    val adapter = remember(moshi) { 
        moshi.adapter<Map<String, String>>(com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
    }
    val tags = pharmacy.tagsJson?.let { try { adapter.fromJson(it) } catch(e: Exception) { null } }
    
    val name = pharmacy.name
    val phone = tags?.get("phone") ?: tags?.get("contact:phone")
    val openingHours = tags?.get("opening_hours")
    val website = tags?.get("website") ?: tags?.get("contact:website")
    
    val is24x7 = openingHours?.contains("24/7", ignoreCase = true) == true ||
                 openingHours?.contains("24 hours", ignoreCase = true) == true ||
                 openingHours?.contains("00:00-24:00", ignoreCase = true) == true
                 
    val isNightOpen = is24x7 || 
                      openingHours?.contains("night", ignoreCase = true) == true ||
                      openingHours?.contains("sunset", ignoreCase = true) == true ||
                      tags?.get("night_service")?.contains("yes", ignoreCase = true) == true ||
                      tags?.get("emergency")?.contains("yes", ignoreCase = true) == true

    // Calculate exact GPS distance dynamically
    val distanceText = remember(pharmacy.lat, pharmacy.lon, userCoordinates) {
        val lat = pharmacy.lat
        val lon = pharmacy.lon
        if (lat != null && lon != null && userCoordinates != null) {
            val results = FloatArray(1)
            try {
                Location.distanceBetween(userCoordinates.first, userCoordinates.second, lat, lon, results)
                val distanceInMeters = results[0]
                if (distanceInMeters < 1000) {
                    "${distanceInMeters.toInt()} meters away"
                } else {
                    String.format("%.2f Km away", distanceInMeters / 1000f)
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isNightOpen) Color(0xFFFAF2FF) else BackgroundLightTeal
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isNightOpen) androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFBA68C8)) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalPharmacy, 
                    contentDescription = null, 
                    tint = if (isNightOpen) Color(0xFF8E24AA) else PrimaryDarkTeal, 
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(name, fontWeight = FontWeight.Bold, color = if (isNightOpen) Color(0xFF4A148C) else PrimaryDarkTeal, fontSize = 16.sp)
                    if (distanceText != null) {
                        Text(
                            text = "📍 $distanceText",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                if (isNightOpen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF8E24AA))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text("🌙 NIGHT OPEN", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
            
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = (if (isNightOpen) Color(0xFFBA68C8) else PrimaryDarkTeal).copy(alpha = 0.12f))
            Spacer(Modifier.height(10.dp))

            if (is24x7) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color(0xFFD81B60), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("24x7 Continuous Pharmacy Service", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFD81B60), fontWeight = FontWeight.Bold)
                }
            } else if (openingHours != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Hours: $openingHours", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }
            
            if (phone != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
                    Icon(Icons.Filled.Phone, contentDescription = null, tint = Color.DarkGray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(phone, style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
            }
            
            if (website != null) {
                Spacer(Modifier.height(4.dp))
                Text("Website: $website", style = MaterialTheme.typography.bodySmall, color = PrimaryDarkTeal, fontWeight = FontWeight.Medium)
            }
        }
    }
}

suspend fun searchPharmacies(
    viewModel: HealthViewModel,
    town: String,
    radiusInMeters: Int = 10000,
    onResult: (List<com.example.data.model.SavedPharmacy>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val searchResults = NominatimClient.service.searchLocation("$town, Sri Lanka")
            if (searchResults.isEmpty()) {
                withContext(Dispatchers.Main) {
                    onResult(emptyList(), "Location not found in Sri Lanka. Try another town name.")
                }
                return@withContext
            }
            
            val lat = searchResults[0].lat.toDouble()
            val lon = searchResults[0].lon.toDouble()
            
            searchPharmaciesByCoords(viewModel, lat, lon, radiusInMeters, onResult)
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

suspend fun searchPharmaciesByCoords(
    viewModel: HealthViewModel,
    lat: Double,
    lon: Double,
    radiusInMeters: Int = 10000,
    onResult: (List<com.example.data.model.SavedPharmacy>, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val query = "[out:json][timeout:25];node(around:$radiusInMeters,$lat,$lon)[\"amenity\"=\"pharmacy\"];out;"
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
                    town = "GPS Location",
                    timestamp = System.currentTimeMillis()
                )
            }
            viewModel.savePharmacies(savedPharmacies)
            
            withContext(Dispatchers.Main) {
                onResult(savedPharmacies, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                onResult(emptyList(), "Error searching nearby: ${e.message}")
            }
        }
    }
}

fun getCurrentLocation(context: Context, onLocationFetched: (Double, Double) -> Unit, onError: (String) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasCoarseLocation = ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    if (!hasFineLocation && !hasCoarseLocation) {
        onError("Location permissions are required to seek nearby pharmacies.")
        return
    }
    
    try {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val l = locationManager.getLastKnownLocation(provider) ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        
        if (bestLocation != null) {
            onLocationFetched(bestLocation.latitude, bestLocation.longitude)
            return
        }
        
        val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            LocationManager.NETWORK_PROVIDER
        } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            null
        }
        
        if (provider != null) {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: android.location.Location) {
                    onLocationFetched(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager.requestLocationUpdates(provider, 0L, 0f, listener)
        } else {
            onError("GPS/Network location provider is disabled. Please enable Location services in device settings.")
        }
    } catch (e: SecurityException) {
        onError("Permission denied: ${e.message}")
    } catch (e: Exception) {
        onError("Could not fetch location: ${e.message}")
    }
}
