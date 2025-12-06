package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shade.ui.viewmodel.HistoryViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.shade.utils.HistoryItem
import com.example.shade.utils.ThreatLevel

@Composable
fun HistoryPage(
    viewModel: HistoryViewModel,
    onMenuClick: () -> Unit,
    onItemClick: (HistoryItem) -> Unit
) {
    val background = Color(0xFF311A57)
    val cardColor = Color(0xFF5A3B8D)
    val textColor = Color.White

    // Sample data – you can replace with real scan results later
    val items by viewModel.items.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        // ─── Top bar ───────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open menu",
                tint = textColor,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.CenterStart)
                    .clickable { onMenuClick() }
            )

            Text(
                text = "App Scans History",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                modifier = Modifier.align(Alignment.Center)
            )


            Text(
                text = "Clear",
                color = Color.Red,
                fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable { viewModel.clearHistory() }
            )
        }



        Spacer(modifier = Modifier.height(12.dp))

        // Divider line under title (to match Home / Scan)
        Divider(
            color = Color(0xFFB9A6D9),
            thickness = 2.dp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Summary row ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "•",
                color = textColor,
                fontSize = 20.sp,
                modifier = Modifier.padding(end = 6.dp)
            )
            Text(
                text = "${items.size} Total Apps Found to be a Dangerous",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.weight(1f)) // pushes refresh to the end

            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = "Refresh",
                tint = Color.Cyan,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { viewModel.refreshHistory() }
            )
        }

        // ─── History list ──────────────────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(items) { item ->
                HistoryCard(
                    item = item,
                    cardColor = cardColor,
                    onClick = { onItemClick(item) }   // open App Details
                )
            }
        }
    }
}

@Composable
private fun HistoryCard(
    item: HistoryItem,
    cardColor: Color,
    onClick: () -> Unit
) {
    val (badgeText, badgeColor) = when (item.threatLevel) {
        ThreatLevel.SAFE   -> "Safe" to Color(0xFF2ECC71)
        ThreatLevel.MEDIUM -> "Medium Threat" to Color(0xFFFFA726)
        ThreatLevel.HIGH   -> "High Threat" to Color(0xFFE53935)
    }

    val (iconImage, iconColor) = when (item.threatLevel) {
        ThreatLevel.SAFE   -> Icons.Filled.CheckCircle to Color(0xFF2ECC71)
        ThreatLevel.MEDIUM -> Icons.Filled.Warning     to Color(0xFFFFA726)
        ThreatLevel.HIGH   -> Icons.Filled.Error       to Color(0xFFE53935)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(cardColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Icon(
                imageVector = iconImage,
                contentDescription = "Threat icon",
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.name,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.dateTime,
                    color = Color(0xFFE5DDF8),
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .background(badgeColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = badgeText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

