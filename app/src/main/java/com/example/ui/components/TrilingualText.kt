package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import com.example.ui.theme.PrimaryDarkTeal

val LocalAppLanguage = compositionLocalOf { "ALL" }

@Composable
fun TrilingualText(
    english: String,
    sinhala: String,
    tamil: String,
    modifier: Modifier = Modifier,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    color: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified,
    scale: Float = 1f
) {
    val language = LocalAppLanguage.current
    Column(modifier = modifier, horizontalAlignment = horizontalAlignment) {
        if (language == "ALL" || language == "EN") {
            Text(
                text = english,
                fontWeight = FontWeight.Bold,
                fontSize = (16 * scale).sp,
                color = color
            )
        }
        if (language == "ALL" || language == "SI") {
            Text(
                text = sinhala,
                fontSize = (12 * scale).sp,
                color = color,
                modifier = if (language == "ALL") Modifier.padding(top = 2.dp) else Modifier
            )
        }
        if (language == "ALL" || language == "TA") {
            Text(
                text = tamil,
                fontSize = (12 * scale).sp,
                color = color,
                modifier = if (language == "ALL") Modifier.padding(top = 2.dp) else Modifier
            )
        }
    }
}

@Composable
fun AppTitleText(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = PrimaryDarkTeal,
    scale: Float = 1.0f,
    fontWeight: FontWeight = FontWeight.Bold
) {
    val language = LocalAppLanguage.current
    val text = when (language) {
        "EN" -> "Arogya"
        "SI" -> "ආරෝග්‍යා"
        "TA" -> "ஆரோக்கிய"
        else -> "Arogya | ආරෝග්‍යා | ஆரோக்கிய"
    }
    Text(
        text = text,
        fontWeight = fontWeight,
        fontSize = (22 * scale).sp,
        color = color,
        modifier = modifier
    )
}

