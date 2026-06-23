package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.R
import com.example.navigation.Screen
import com.example.ui.HealthViewModel
import com.example.ui.theme.*
import com.example.ui.components.TrilingualText
import kotlinx.coroutines.launch

@Composable
fun LaunchScreen(navController: NavController, viewModel: HealthViewModel) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top row with Language Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.components.LanguageSwitcher(viewModel = viewModel, tint = PrimaryDarkTeal)
        }

        // Swipeable Pager for Onboarding
        androidx.compose.foundation.pager.HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.weight(1f))
                
                when (page) {
                    0 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            Text(
                                text = "ආරෝග්‍යා",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = PrimaryDarkTeal,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Arogya  •  ஆரோக்கிய",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryDarkTeal.copy(alpha = 0.85f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TrilingualText(
                            english = "Your Digital Health Wallet",
                            sinhala = "ඔබේ ඩිජිටල් සෞඛ්‍ය පසුම්බිය",
                            tamil = "உங்கள் டிஜிட்டல் சுகாதார பணப்பை",
                            scale = 1.15f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Image(
                            painter = painterResource(id = R.drawable.sri_lankan_family_1781845434545),
                            contentDescription = "Welcome",
                            modifier = Modifier
                                .size(210.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        TrilingualText(
                            english = "Keep your medical records, prescriptions, and lab results safe in one place.",
                            sinhala = "ඔබගේ වෛද්‍ය වාර්තා, බෙහෙත් වට්ටෝරු සහ රසායනාගාර ප්‍රතිඵල එක් ස්ථානයක ආරක්ෂිතව තබා ගන්න.",
                            tamil = "உங்கள் மருத்துவ பதிவுகள், மருந்து சீட்டுகள் மற்றும் ஆய்வக முடிவுகளை ஒரே இடத்தில் பாதுகாப்பாக வைத்திருங்கள்.",
                            scale = 1.0f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = GrayText
                        )
                    }
                    1 -> {
                        Image(
                            painter = painterResource(id = R.drawable.heart_monitor_icon_1781202537389),
                            contentDescription = "Monitor",
                            modifier = Modifier
                                .size(180.dp)
                                .padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        TrilingualText(
                            english = "Track Your Vitals",
                            sinhala = "ඔබේ වැදගත් සෞඛ්‍ය ලකුණු නිරීක්ෂණය කරන්න",
                            tamil = "உங்கள் முக்கிய ஆரோக்கிய அளவீடுகளை கண்காணிக்கவும்",
                            scale = 1.3f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TrilingualText(
                            english = "Monitor your blood pressure, sugar levels, and overall health score easily.",
                            sinhala = "ඔබගේ රුධිර පීඩනය, සීනි මට්ටම සහ සමස්ත සෞඛ්‍ය ලකුණු පහසුවෙන් නිරීක්ෂණය කරන්න.",
                            tamil = "உங்கள் இரத்த அழுத்தம், சர்க்கரை அளவு மற்றும் ஒட்டுமொத்த சுகாதார மதிப்பீட்டை எளிதாகக் கண்காணிக்கவும்.",
                            scale = 1.0f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = GrayText
                        )
                    }
                    2 -> {
                        Image(
                            painter = painterResource(id = R.drawable.img_sri_lankan_family_1781683501360),
                            contentDescription = "Family",
                            modifier = Modifier
                                .size(240.dp)
                                .clip(RoundedCornerShape(24.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(48.dp))
                        TrilingualText(
                            english = "Share with Your Doctor",
                            sinhala = "ඔබේ වෛද්‍යවරයා සමඟ බෙදා ගන්න",
                            tamil = "உங்கள் மருத்துவருடன் பகிர்ந்து கொள்ளுங்கள்",
                            scale = 1.3f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TrilingualText(
                            english = "Present your Medical Snapshot QR to clinics for instant secure access.",
                            sinhala = "ක්ෂණික ආරක්ෂිත ප්‍රවේශය සඳහා ඔබේ වෛද්‍ය තොරතුරු QR කේතය සායන වෙත ඉදිරිපත් කරන්න.",
                            tamil = "உடனடி பாதுகாப்பான அணுகலுக்கு உங்கள் மருத்துவத் தகவலின் QR குறியீட்டை மருத்துவமனைகளிடம் காட்டுங்கள்.",
                            scale = 1.0f,
                            horizontalAlignment = Alignment.CenterHorizontally,
                            color = GrayText
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page Indicators
            Row(
                modifier = Modifier.padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryDarkTeal else LightGrayBackground)
                    )
                }
            }

            if (pagerState.currentPage == 2) {
                Button(
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Launch.route) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryDarkTeal),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TrilingualText(
                        english = "Get Started",
                        sinhala = "ආරම්භ කරන්න",
                        tamil = "තொடங்குங்கள்",
                        color = White,
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                }
            } else {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PastelGreen),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    TrilingualText(
                        english = "Next",
                        sinhala = "මීළඟ",
                        tamil = "அடுத்தது",
                        color = PrimaryDarkTeal,
                        horizontalAlignment = Alignment.CenterHorizontally
                    )
                }
            }
        }
    }
}


