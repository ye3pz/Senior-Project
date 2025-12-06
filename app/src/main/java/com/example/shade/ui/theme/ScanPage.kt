package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shade.ui.viewmodel.HistoryViewModel
import com.example.shade.ui.viewmodel.QuickScanViewModel
import com.example.shade.ui.viewmodel.FullScanViewModel
import com.example.shade.utils.LoadingOverlay

@Composable
fun ScanPage(
    historyViewModel: HistoryViewModel,
    quickScanViewModel: QuickScanViewModel,
    fullScanViewModel: FullScanViewModel,
    onMenuClick: () -> Unit
) {
    val background = Color(0xFF311A57)      // dark purple
    val cardColor = Color(0xFF5A3B8D)       // lighter purple for cards

    val quickScanning = quickScanViewModel.isScanning.collectAsState().value
    val fullScanning = fullScanViewModel.isScanning.collectAsState().value


    LaunchedEffect(quickScanViewModel.scanCompleted.collectAsState().value) {
        if (quickScanViewModel.scanCompleted.value  || fullScanViewModel.scanCompleted.value) {
            historyViewModel.refreshHistory()
        }
    }
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
            // Menu icon (left)
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onMenuClick() }
            )

            // Centered title
            Text(
                text = "Scan",
                fontSize = 28.sp,
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

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle text
        Text(
            text = "Choose a scan type to check your device for threats",
            fontSize = 14.sp,
            color = Color(0xFFE5DDF8),
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        //  Quick Scan card
        ScanCard(
            title = "Quick Scan",
            description = "Fast scan of recently installed apps and common threat areas. Takes about 30 seconds.",
            buttonText = "Start Quick Scan",
            cardColor = cardColor,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Quick scan icon",
                    tint = Color(0xFFFFEB3B),        // yellow-ish
                    modifier = Modifier.size(22.dp)
                )
            },
            onClick = { quickScanViewModel.startQuickScan()}
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full Scan card
        ScanCard(
            title = "Full Scan",
            description = "Comprehensive scan of all apps, files, and system areas.\nRecommended for thorough protection.",
            buttonText = "Start Full Scan",
            cardColor = cardColor,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Security,
                    contentDescription = "Full scan icon",
                    tint = Color(0xFF00E5FF),        // cyan-ish
                    modifier = Modifier.size(22.dp)
                )
            },
            onClick = { fullScanViewModel.startFullScan() }

        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (quickScanning) {
        LoadingOverlay("Running Quick Scan… Results will be on the History Page")
    }

    if (fullScanning) {
        LoadingOverlay("Running Full Scan… This may take a minute. Results will be on the History Page")
    }
}

@Composable
private fun ScanCard(
    title: String,
    description: String,
    buttonText: String,
    cardColor: Color,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
            ) {
                icon()
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = description,
            fontSize = 13.sp,
            color = Color(0xFFE5DDF8)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(Color.White, RoundedCornerShape(20.dp))
                .clickable {
                    onClick()
                }
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = buttonText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = cardColor
            )
        }
    }
}
