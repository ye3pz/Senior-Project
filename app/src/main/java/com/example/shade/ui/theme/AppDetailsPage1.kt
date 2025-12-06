package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shade.utils.HistoryItem
import com.example.shade.utils.ThreatLevel

@Composable
fun AppDetailsPage1(
    app: HistoryItem,          // from HistoryPage
    onBackClick: () -> Unit
) {
    val background = Color(0xFF311A57)
    val cardColor = Color(0xFF5A3B8D)
    val textColor = Color.White

    // Badge + risk score per threat level
    val (badgeText, badgeColor, riskScore) = when (app.threatLevel) {
        ThreatLevel.SAFE   -> Triple("Safe", Color(0xFF2ECC71), 1)
        ThreatLevel.MEDIUM -> Triple("Medium Threat", Color(0xFFFFA726), 5)
        ThreatLevel.HIGH   -> Triple("High Threat", Color(0xFFE53935), 8)
    }

    // Summary text based on threat level
    val summaryText = app.description

    // Key dangers based on threat level
    val dangersText = app.dangerList.joinToString("\n") { "• $it" }

    // Recommended alternative apps for Medium / High
    val recommendedApps: List<String> = when (app.threatLevel) {
        ThreatLevel.SAFE -> emptyList()
        ThreatLevel.MEDIUM -> {
            // Example: Quick Notes → notes alternatives
            if (app.name.contains("note", ignoreCase = true)) {
                listOf("Google Notes", "Samsung Notes", "UpNotes")
            } else {
                listOf("Google Notes", "Samsung Notes", "UpNotes")
            }
        }
        ThreatLevel.HIGH -> {
            // Example: flashlight app → flashlight alternatives
            if (app.name.contains("flash", ignoreCase = true)) {
                listOf("Flashlight Lite", "Flashlight Okay", "Best Flashlight")
            } else {
                listOf("Trusted Alternative 1", "Trusted Alternative 2", "Trusted Alternative 3")
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = textColor,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { onBackClick() }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "App Details",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Divider(
            color = Color(0xFFB9A6D9),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // App + risk score card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Risk Score",
                fontSize = 13.sp,
                color = Color(0xFFE5DDF8)
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = riskScore / 10f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = badgeColor,
                trackColor = Color(0xFF2B174A)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$riskScore/10",
                fontSize = 12.sp,
                color = Color(0xFFE5DDF8),
                modifier = Modifier.align(Alignment.End)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Summary",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = summaryText,
                fontSize = 13.sp,
                color = Color(0xFFE5DDF8)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        //  Key dangers card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Key dangers",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Key Dangers",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dangersText,
                fontSize = 13.sp,
                color = Color(0xFFE5DDF8)
            )
        }

        // Recommended alternatives (Medium / High only)
        if (recommendedApps.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Recommended App Alternatives",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )

                Spacer(modifier = Modifier.height(8.dp))

                recommendedApps.forEach { alt ->
                    Text(
                        text = "• $alt",
                        fontSize = 13.sp,
                        color = Color(0xFFE5DDF8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        //Bottom banner
        val bottomText: String
        val bottomColor: Color

        if (app.threatLevel == ThreatLevel.SAFE) {
            bottomText = "Safe to Keep"
            bottomColor = Color(0xFF2ECC71)
        } else {
            bottomText = "Consider uninstalling the app for device protection."
            bottomColor = Color(0xFFE53935)  // red like your Figma
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(bottomColor, RoundedCornerShape(20.dp))
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bottomText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}
