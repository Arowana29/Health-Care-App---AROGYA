package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.HealthViewModel
import com.example.ui.theme.PrimaryDarkTeal

@Composable
fun LanguageSwitcher(
    viewModel: HealthViewModel,
    tint: Color = LocalContentColor.current
) {
    val currentLang by viewModel.currentLanguage.collectAsState()

    // 3 clean options for high-contrast, beautiful trilingual selection
    val languages = listOf(
        Triple("EN", "EN", "English"),
        Triple("SI", "සිං", "සිංහල"),
        Triple("TA", "தமி", "தமிழ்")
    )

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (tint == Color.White) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.05f))
            .border(1.dp, tint.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        languages.forEach { (code, label, fullName) ->
            val isSelected = currentLang == code
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (tint == Color.White) Color.White else PrimaryDarkTeal
                } else {
                    Color.Transparent
                },
                label = "bgColorAnim"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) {
                    if (tint == Color.White) PrimaryDarkTeal else Color.White
                } else {
                    tint.copy(alpha = 0.8f)
                },
                label = "textColorAnim"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .clickable { viewModel.setLanguage(code) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
            }
        }
    }
}

