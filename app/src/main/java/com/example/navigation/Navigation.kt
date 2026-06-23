package com.example.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Chat
import androidx.compose.ui.graphics.vector.ImageVector

enum class Screen(val route: String, val icon: ImageVector?, val titleEn: String, val titleSi: String, val titleTa: String) {
    Passcode("passcode", null, "Passcode", "Passcode", "Passcode"),
    Launch("launch", null, "", "", ""),
    Emergency("emergency", null, "", "", ""),
    Home("home", Icons.Filled.Home, "Home", "මුල් පිටුව", "முகப்பு"),
    Documents("documents", Icons.Filled.Description, "Docs", "ලිපිලේඛන", "ஆவணங்கள்"),
    Medications("medications", Icons.Filled.MedicalServices, "Meds", "ඖෂධ", "மருந்துகள்"),
    Visits("visits", Icons.Filled.MedicalServices, "Visits", "රෝහල", "மருத்துவமனை"), // Only accessible from home or add
    Expenses("expenses", Icons.Filled.Description, "Expenses", "වියදම්", "செலவுகள்"),
    Symptoms("symptoms", Icons.Filled.MedicalServices, "Symptoms", "රෝග ලක්ෂණ", "அறிகுறிகள்"),
    Share("share", Icons.Filled.Share, "Share", "බෙදාගන්න", "பகிர்"),
    DailyVitals("daily_vitals", Icons.Filled.MedicalServices, "Vitals", "වෛද්‍ය වාර්තා", "உயிர் இயல்புகள்"),
    Profile("profile", Icons.Filled.Person, "Profile", "පැතිකඩ", "சுயவிவரம்"),
    PremiumFamily("premium_family", null, "Premium Family", "Premium පවුල", "பிரீமியம் குடும்பம்"),
    PharmacyLocator("pharmacy_locator", Icons.Filled.MedicalServices, "Pharmacies", "ෆාමසි", "மருந்தகங்கள்"),
    Chat("chat", Icons.Filled.Chat, "Health AI", "AI සහායක", "AI அரட்டை"),
    MedicalRecords("medical_records", Icons.Filled.Description, "Records", "වාර්තා", "பதிவுகள்")
}
