package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ui.HealthViewModel
import com.example.ui.components.LocalAppLanguage
import com.example.ui.components.TrilingualText
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumFamilyScreen(navController: NavController, viewModel: HealthViewModel) {
    val context = LocalContext.current
    val isPremium by viewModel.isPremiumUser.collectAsState()
    val scrollState = rememberScrollState()
    val language = LocalAppLanguage.current

    val titleEn = "Premium Family"
    val titleSi = "ප්‍රිමියම් පවුල"
    val titleTa = "பிரீமியம் குடும்பம்"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TrilingualText(
                        english = titleEn,
                        sinhala = titleSi,
                        tamil = titleTa,
                        scale = 1.1f,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.testTag("premium_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryDarkTeal
                )
            )
        },
        containerColor = LightGrayBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Image/Card with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(SnapshotGradientStart, SnapshotGradientEnd)
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = "Premium Badge",
                            tint = Color.Yellow,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isPremium) "ACTIVE PLAN: PREMIUM FAMILY" else "UPGRADE TO PREMIUM FAMILY",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = if (isPremium) "Full access unlocked for your household" else "Empower your family's health digital wallet",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Summary of benefits section
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = GrayText.copy(alpha = 0.3f))
                Text(
                    text = when (language) {
                        "SI" -> "ප්‍රතිලාභ සාරාංශය"
                        "TA" -> "பலன்கள் சுருக்கம்"
                        else -> "MEMBERSHIP BENEFITS"
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryDarkTeal,
                    letterSpacing = 1.5.sp
                )
                Divider(modifier = Modifier.weight(1f), color = GrayText.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Unlimited family members
            BenefitCard(
                icon = Icons.Filled.People,
                iconColor = IconBlue,
                titleEn = "Unlimited Family Profiles",
                titleSi = "සීමාවකින් තොර පවුලේ සාමාජිකයින්",
                titleTa = "வரம்பற்ற குடும்ப சுயவிவரங்கள்",
                descEn = "Add more than 3 profiles. Keep records for parents, children, and spouses in one sandbox wallet.",
                descSi = "සාමාජිකයින් 3 කට වඩා එක් කරන්න. දෙමාපියන්, දරුවන් සහ කලත්‍රයාගේ වෛද්‍ය වාර්තා එකම සුරක්ෂිත තැනක තබන්න.",
                descTa = "3 க்கும் மேற்பட்ட சுயவிவரங்களைச் சேர்க்கவும். பெற்றோர், குழந்தைகள் மற்றும் துணையின் மருத்துவப் பதிவுகளை ஒரே பாதுகாப்பான வாலட்டில் வைக்கவும்."
            )

            // Advanced ZIP / PDF exports
            BenefitCard(
                icon = Icons.Filled.CloudDownload,
                iconColor = IconGreen,
                titleEn = "Advanced Document & Report Exports",
                titleSi = "උසස් ලේඛන සහ වාර්තා අපනයනය",
                titleTa = "மேம்பட்ட ஆவண மற்றும் அறிக்கை ஏற்றுமதி",
                descEn = "Export fully formatted health analytics tables, symptom logs, and medication summaries to password-protected ZIP/JSON files.",
                descSi = "ආරක්ෂිත මුරපද සහිත ZIP සහ JSON ගොනු ලෙස රෝග ලක්ෂණ සහ ඖෂධ වාර්තා අපනයනය කරන්න.",
                descTa = "கடவுச்சொல் பாதுகாக்கப்பட்ட ZIP மற்றும் JSON கோப்புகளாக முழுக் குடும்பத்தின் ஆரோக்கிய ஆவணங்களை ஏற்றுமதி செய்யுங்கள்."
            )

            // Gemini Clinical summaries
            BenefitCard(
                icon = Icons.Filled.AutoAwesome,
                iconColor = IconPurple,
                titleEn = "Smart Clinical Overview & Insights",
                titleSi = "ස්මාර්ට් සායනික දළ විශ්ලේෂණය",
                titleTa = "ஸ்மார்ட் மருத்துவ கண்ணோட்டம்",
                descEn = "Access real-time trilingual clinical AI overview generated from daily vitals, laboratory tests and medical documents.",
                descSi = "දෛනික වැදගත් ලකුණු, පරීක්ෂණ වාර්තා මඟින් උත්පාදනය කල ස්මාර්ට් සායනික දළ විශ්ලේෂණ ලබා ගන්න.",
                descTa = "தினசரி முக்கிய அறிகுறிகள் மற்றும் ஆவணங்களின் அடிப்படையில் உருவாக்கப்படும் ஸ்மார்ட் மருத்துவ பகுப்பாய்வுகளைப் பெறுங்கள்."
            )

            // Secure offline and cloud
            BenefitCard(
                icon = Icons.Filled.Lock,
                iconColor = IconPink,
                titleEn = "High Priority Encrypted Backups",
                titleSi = "ඉහළ ප්‍රමුඛතා සංකේතාත්මක උපස්ථ",
                titleTa = "உயர் முன்னுரிமை மறைகுறியாக்கப்பட்ட காப்புப்பிரதிகள்",
                descEn = "Unlocks secure offline AES passcode options and prioritized Firebase server sync pipeline with secondary backup slots.",
                descSi = "නිරන්තරයෙන්ම ඔබගේ උපාංගයේ දත්ත සුරක්ෂිතව තබාගැනීමට PIN කේත මඟින් ඇතුල්වීම් සක්‍රීය කරයි.",
                descTa = "சாதனத்தின் ஆவணப் பாதுகாப்பை அதிகரிக்க பின் குறியீட்டு அனுமதியுடன் கூடிய அணுகலை செயல்படுத்துகிறது."
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subscribing Section
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPremium) PastelGreen else BackgroundLightTeal
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (isPremium) Icons.Filled.CheckCircle else Icons.Filled.Star,
                        contentDescription = "Subscription plan",
                        tint = if (isPremium) IconGreen else IconOrange,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isPremium) {
                        TrilingualText(
                            english = "You are a Premium Family Member!",
                            sinhala = "ඔබ ප්‍රිමියම් පවුලේ සාමාජිකයෙකි!",
                            tamil = "நீங்கள் ஒரு பிரீமியம் குடும்ப உறுப்பினர்!",
                            color = PrimaryDarkTeal,
                            scale = 1.1f,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Enjoy unlimited family member profiles, sandbox cloud backups, and smart insights on all devices.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = GrayText
                        )
                    } else {
                        TrilingualText(
                            english = "$4.99 / Month (LKR 1,500)",
                            sinhala = "මසකට රුපියල් 1,500ක් පමණි",
                            tamil = "மாதத்திற்கு ரூ. 1,500 மட்டுமே",
                            color = PrimaryDarkTeal,
                            scale = 1.2f,
                            horizontalAlignment = Alignment.CenterHorizontally
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Standard 7-day free trial applies. Cancel anytime through Google Play Settings.",
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            color = GrayText
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            viewModel.togglePremiumStatus()
                            val msg = if (!isPremium) "Welcome to Premium Family!" else "Premium status deactivated."
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPremium) Color.Red else PrimaryDarkTeal
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("premium_toggle_button")
                    ) {
                        Text(
                            text = if (isPremium) "Deactivate License (Test)" else "Upgrade & Activate Trial",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BenefitCard(
    icon: ImageVector,
    iconColor: Color,
    titleEn: String,
    titleSi: String,
    titleTa: String,
    descEn: String,
    descSi: String,
    descTa: String
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                TrilingualText(
                    english = titleEn,
                    sinhala = titleSi,
                    tamil = titleTa,
                    color = PrimaryDarkTeal,
                    scale = 0.95f
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                val language = LocalAppLanguage.current
                Text(
                    text = when (language) {
                        "SI" -> descSi
                        "TA" -> descTa
                        else -> descEn
                    },
                    fontSize = 13.sp,
                    color = GrayText,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
