package com.example.shade.ui.theme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    modifier: Modifier = Modifier,
    onClose: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onUserGuideClick: () -> Unit = {},
    onAboutUsClick: () -> Unit = {}
) {
    val backgroundColor = Color(0xFFD9CCFF)      // light purple like your design
    val textColor = Color(0xFF2E2345)           // dark purple for text

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 32.dp,
                bottom = 12.dp
            )
    ) {
        // Top bar: "Menu" + close icon
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Menu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )

            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Close menu",
                tint = textColor,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onClose() }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Menu items
        MenuItem(
            label = "Settings",
            icon = Icons.Filled.Settings,
            textColor = textColor,
            onClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        MenuItem(
            label = "User Guide",
            icon = Icons.Outlined.Info,
            textColor = textColor,
            onClick = onUserGuideClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        MenuItem(
            label = "About Us",
            icon = Icons.Filled.Info,
            textColor = textColor,
            onClick = onAboutUsClick
        )
    }
}

@Composable
private fun MenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 16.sp,
            color = textColor
        )
    }
}

@Preview(showBackground = true, widthDp = 220, heightDp = 450)
@Composable
fun MenuScreenPreview() {
    MaterialTheme {
        MenuScreen()
    }
}
