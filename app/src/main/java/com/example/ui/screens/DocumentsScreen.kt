package com.example.ui.screens

import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.TrilingualText
import com.example.ui.components.AppTitleText
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.data.model.MedicalDocument
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.graphics.Bitmap
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset

fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
    return stream.toByteArray()
}

fun byteArrayToBitmap(data: ByteArray): Bitmap {
    return android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentsScreen(navController: NavController, viewModel: HealthViewModel) {
    val documents by viewModel.documents.collectAsState()
    val ocrResult by viewModel.ocrResult.collectAsState()
    val extProvider by viewModel.extractedProvider.collectAsState()
    val extDate by viewModel.extractedDate.collectAsState()
    val context = LocalContext.current
    
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var viewingDocument by remember { mutableStateOf<MedicalDocument?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var docType by remember { mutableStateOf("Other") }
    var docDate by remember { mutableStateOf(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())) }
    var docProvider by remember { mutableStateOf("") }

    LaunchedEffect(ocrResult) {
        if (ocrResult != null) {
            extProvider?.let { docProvider = it }
            extDate?.let { docDate = it }
        }
    }

    val categories = listOf("All", "Lab Report", "Prescription", "Vaccination Record", "X-Ray", "Scan", "Other")
    val documentTypes = listOf("Lab Report", "Prescription", "Vaccination Record", "X-Ray", "Scan", "Other")

    val filteredDocuments = documents.filter { doc ->
        val matchesSearch = doc.type.contains(searchQuery, ignoreCase = true) || 
                            doc.providerName.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == "All" || doc.type.equals(selectedCategory, ignoreCase = true)
        matchesSearch && matchesCategory
    }

    val fileProviderUri = remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && fileProviderUri.value != null) {
            val uri = fileProviderUri.value!!
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source).copy(Bitmap.Config.ARGB_8888, true)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri).copy(Bitmap.Config.ARGB_8888, true)
                }
                selectedBitmap = bitmap
                isAnalyzing = true
                viewModel.analyzeImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                } else {
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                selectedBitmap = bitmap
                isAnalyzing = true
                viewModel.analyzeImage(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
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
                        TrilingualText("Documents", "ලිපිලේඛන", "ஆவணங்கள்", color = White, scale = 0.95f)
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
                Icon(Icons.Filled.Add, "Add Document")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedBitmap != null) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                        Text("Scanned Medical Report", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimaryDarkTeal)
                        Spacer(modifier = Modifier.height(8.dp))
                        var scale by remember { mutableStateOf(1f) }
                        var offset by remember { mutableStateOf(Offset.Zero) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(LightGrayBackground)
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(1f, 5f)
                                        val maxX = (size.width * (scale - 1)) / 2
                                        val maxY = (size.height * (scale - 1)) / 2
                                        offset = Offset(
                                            x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                            y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                        )
                                    }
                                }
                        ) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Document Scan",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Gemini AI Status Info Area
                        if (isAnalyzing && ocrResult == null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PastelBlue.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IconBlue, strokeWidth = 2.5.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Gemini AI is analyzing document for vitals...", fontSize = 13.sp, color = Black)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        } else if (ocrResult != null) {
                            isAnalyzing = false
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PastelGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IconGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Scan Summary:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Black)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(ocrResult!!, fontSize = 12.sp, color = GrayText)
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            if (viewModel.extractedVitals.isNotEmpty()) {
                                Text("Extracted Vital Signs (Auto-linked to Dashboard):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDarkTeal)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                                ) {
                                    viewModel.extractedVitals.forEach { vital ->
                                        val label = indexVitalLabel(vital)
                                        val color = indexVitalColor(vital)
                                        val bColor = indexVitalBg(vital)
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = bColor),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when(vital.type) {
                                                        "Blood Pressure" -> Icons.Outlined.MonitorHeart
                                                        "Blood Sugar" -> Icons.Outlined.WaterDrop
                                                        else -> Icons.Outlined.Science
                                                    },
                                                    contentDescription = null,
                                                    tint = color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = label,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = color
                                                )
                                            }
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        Text("Verify Document Info", fontWeight = FontWeight.Bold, color = Black, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))

                        val focusColor = PrimaryDarkTeal
                        OutlinedTextField(
                            value = docProvider,
                            onValueChange = { docProvider = it },
                            label = { Text("Provider/Hospital/Clinic Name") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = focusColor,
                                focusedLabelColor = focusColor
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = docDate,
                            onValueChange = { docDate = it },
                            label = { Text("Document Date (YYYY-MM-DD)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = focusColor,
                                focusedLabelColor = focusColor
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text("Document Category", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            documentTypes.forEach { type ->
                                FilterChip(
                                    selected = docType == type,
                                    onClick = { docType = type },
                                    label = { Text(type) },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { 
                                selectedBitmap = null
                                viewModel.clearOcrResult()
                            }) {
                                Text("Discard", color = ErrorRed)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { 
                                    val blob = selectedBitmap?.let { bitmapToByteArray(it) }
                                    viewModel.insertDocumentWithVitals(
                                        MedicalDocument(
                                            type = docType,
                                            date = docDate,
                                            providerName = docProvider.ifEmpty { "Unknown Clinic" },
                                            uri = "content://scan",
                                            imageBlob = blob,
                                            analysis = ocrResult
                                        )
                                    )
                                    selectedBitmap = null
                                    viewModel.clearOcrResult()
                                    docProvider = ""
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                            ) {
                                Text("Save Document", color = White)
                            }
                        }
                    }
                }
            } else if (documents.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                    TrilingualText("No documents yet", "තවම ලිපිලේඛන නැත", "இதுவரை ஆவணங்கள் இல்லை", color = PrimaryDarkTeal, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally)
                }
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search Documents") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category) },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                }

                if (filteredDocuments.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        Text("No matching documents found.", color = PrimaryDarkTeal)
                    }
                } else {
                    LazyColumn(modifier = Modifier.padding(16.dp)) {
                        items(filteredDocuments) { doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).clickable { viewingDocument = doc },
                                colors = CardDefaults.cardColors(containerColor = BackgroundLightTeal)
                            ) {
                                Row(modifier = Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                    if (doc.imageBlob != null) {
                                        Image(
                                            bitmap = byteArrayToBitmap(doc.imageBlob).asImageBitmap(),
                                            contentDescription = "Doc",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    } else {
                                        Icon(Icons.Filled.Description, contentDescription = "Doc", tint = PrimaryDarkTeal, modifier = Modifier.size(40.dp))
                                    }
                                    Spacer(Modifier.width(16.dp))
                                    Column {
                                        Text(doc.type, fontWeight = FontWeight.Bold)
                                        Text("${doc.providerName} • ${doc.date}", style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Add Document") },
                text = {
                    Column {
                        Button(
                            onClick = { 
                                showAddDialog = false
                                val file = java.io.File(context.cacheDir, "temp_doc_${System.currentTimeMillis()}.jpg")
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                                fileProviderUri.value = uri
                                cameraLauncher.launch(uri) 
                            }, 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Camera Scan")
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { 
                                showAddDialog = false
                                galleryLauncher.launch("image/*") 
                            }, 
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Gallery Upload")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddDialog = false }) { Text("Cancel") }
                }
            )
        }

        if (viewingDocument != null) {
            val doc = viewingDocument!!
            AlertDialog(
                onDismissRequest = { viewingDocument = null },
                containerColor = White,
                title = { Text(doc.type, fontWeight = FontWeight.Bold, color = PrimaryDarkTeal) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (doc.imageBlob != null) {
                            var scale by remember { mutableStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            val bitmap = byteArrayToBitmap(doc.imageBlob)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(LightGrayBackground)
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            scale = (scale * zoom).coerceIn(1f, 5f)
                                            val maxX = (size.width * (scale - 1)) / 2
                                            val maxY = (size.height * (scale - 1)) / 2
                                            offset = Offset(
                                                x = (offset.x + pan.x * scale).coerceIn(-maxX, maxX),
                                                y = (offset.y + pan.y * scale).coerceIn(-maxY, maxY)
                                            )
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Medical Document",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .graphicsLayer(
                                            scaleX = scale,
                                            scaleY = scale,
                                            translationX = offset.x,
                                            translationY = offset.y
                                        ),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .background(BackgroundLightTeal, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Description, contentDescription = null, tint = PrimaryDarkTeal, modifier = Modifier.size(48.dp))
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Hospital/Clinic: ", fontWeight = FontWeight.Bold, color = Black, fontSize = 14.sp)
                            Text(doc.providerName, color = GrayText, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Document Date: ", fontWeight = FontWeight.Bold, color = Black, fontSize = 14.sp)
                            Text(doc.date, color = GrayText, fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text("Category: ", fontWeight = FontWeight.Bold, color = Black, fontSize = 14.sp)
                            Text(doc.type, color = GrayText, fontSize = 14.sp)
                        }
                        
                        if (!doc.analysis.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = PastelGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = IconGreen, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("AI Scan Result", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryDarkTeal)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(doc.analysis, fontSize = 12.sp, color = Black)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.deleteDocument(doc)
                                viewingDocument = null
                            }
                        ) {
                            Text("Delete", color = ErrorRed)
                        }
                        Button(
                            onClick = { viewingDocument = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal)
                        ) {
                            Text("Close", color = White)
                        }
                    }
                }
            )
        }
    }
}

fun indexVitalLabel(vital: com.example.data.model.VitalSign): String {
    return when(vital.type) {
        "Blood Pressure" -> "BP: ${vital.value1}/${vital.value2}"
        "Blood Sugar" -> "Glucose: ${vital.value1} mg/dL"
        "Cholesterol" -> "Cholesterol: ${vital.value1} mg/dL"
        else -> "${vital.type}: ${vital.value1}"
    }
}

fun indexVitalColor(vital: com.example.data.model.VitalSign): Color {
    return when(vital.type) {
        "Blood Pressure" -> com.example.ui.theme.IconPurple
        "Blood Sugar" -> com.example.ui.theme.ErrorRed
        "Cholesterol" -> com.example.ui.theme.IconOrange
        else -> com.example.ui.theme.PrimaryDarkTeal
    }
}

fun indexVitalBg(vital: com.example.data.model.VitalSign): Color {
    return when(vital.type) {
        "Blood Pressure" -> com.example.ui.theme.PastelPurple
        "Blood Sugar" -> com.example.ui.theme.PastelPink
        "Cholesterol" -> com.example.ui.theme.PastelOrange
        else -> com.example.ui.theme.PastelGreen
    }
}
