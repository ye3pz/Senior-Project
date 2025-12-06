package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shade.ui.viewmodel.QuickScanViewModel
import com.example.shade.utils.LoadingOverlay
import com.example.shade.ThreatsList.activeThreats

@Composable
fun HomePage(
    viewModel: QuickScanViewModel,
    onMenuClick: () -> Unit,
    onQuickScanClick: () -> Unit = {

    },
    onTapScanClick: () -> Unit = {}
) {
    val background = Color(0xFF311A57)      // dark purple
    val cardColor = Color(0xFF5A3B8D)       // quick scan card
    val textSoft = Color(0xFFE5DDF8)

    val isScanning = viewModel.isScanning.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onMenuClick() }
            )

            Text(
                text = "Home",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }


        Spacer(modifier = Modifier.height(12.dp))

        Divider(
            color = Color(0xFFB9A6D9),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Scan card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(18.dp))
                .clickable { onQuickScanClick }
                .padding(vertical = 18.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Quick Scan",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Run a quick scan to check for\nrecent threats",
                fontSize = 13.sp,
                color = textSoft
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Big scan circle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .background(Color.White, CircleShape)
                    .clickable { viewModel.startQuickScan() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Scan shield",
                    tint = background,
                    modifier = Modifier.size(72.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Tap to scan apps",
                fontSize = 16.sp,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Device Health Card
            DeviceHealthCard()

        }
    }
    if (isScanning) {
        LoadingOverlay("Running Quick Scan… Results will be on the History Page")
    }
}

@Composable
fun DeviceHealthCard() {
    val activeThreatsCount = activeThreats.size

    // Determine health label
    val healthLabel = when {
        activeThreatsCount == 0 -> "Excellent"
        activeThreatsCount <= 3 -> "Good"
        activeThreatsCount <= 6 -> "Fair"
        else -> "Poor"
    }

    // Create dynamic list of observations
    val observations = listOf(
        "${activeThreats.count { it.threatLevel == com.example.shade.utils.ThreatLevel.HIGH }} High risk apps detected",
        "${activeThreats.count { it.threatLevel == com.example.shade.utils.ThreatLevel.MEDIUM }} Medium risk apps detected",
        "${activeThreats.count { it.threatLevel == com.example.shade.utils.ThreatLevel.SAFE }} Safe apps detected"
    )

    val cardColor = Color(0xFF5A3B8D)
    val textSoft = Color(0xFFE5DDF8)

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Device Health",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .background(
                        color = when (healthLabel) {
                            "Excellent" -> Color(0xFF2CC67A)
                            "Good" -> Color(0xFF7ED321)
                            "Fair" -> Color(0xFFFFA726)
                            else -> Color(0xFFE53935)
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(healthLabel, color = Color.White, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        observations.forEach { obs ->
            Text(obs, color = textSoft, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

