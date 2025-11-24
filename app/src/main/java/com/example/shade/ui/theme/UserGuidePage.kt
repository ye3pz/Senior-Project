package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable


@Composable
fun UserGuidePage(
    onBackClick: () -> Unit
) {
    val background = Color(0xFFD9CCFF)       // light lavender
    val cardColor = Color(0xFFC4A9FF)        // slightly darker card
    val textColor = Color(0xFF2E2345)        // dark purple for text

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .padding(top = 32.dp, start = 16.dp, end = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
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
                text = "User Guide",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }

        // Main scrollable card
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .background(cardColor, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Text(
                    text = "Welcome to Shade – your personal mobile security assistant!\n",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "This app is designed to be simple, smart, and user-friendly — helping you keep your device safe with just a few taps.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                // How it works
                SectionTitle("How It Works", textColor)

                Text(
                    text = "• When you tap Scan, the app starts checking all the apps on your device that haven’t been scanned yet.\n" +
                            "• Our built-in machine learning model looks at each app’s behavior, permissions, and patterns behind the scenes.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                Text(
                    text = "You’ll see real-time updates as the scan progresses:\n" +
                            "• Apps currently being scanned appear under “Scanning”.\n" +
                            "• Once completed, they move to the “Completed” section.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                // Understanding the results
                SectionTitle("Understanding the Results", textColor)

                Text(
                    text = "Each scanned app is labeled with a risk level:\n\n" +
                            "🟢 Safe – No suspicious behavior detected.\n" +
                            "🟡 Medium – May have some unusual permissions or patterns.\n" +
                            "🔴 High – Shows signs of potentially malicious behavior.\n\n" +
                            "These results update automatically as each app is analyzed.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                // What you can do next
                SectionTitle("What You Can Do Next", textColor)

                Text(
                    text = "• If an app is flagged as High Risk, we recommend uninstalling it to protect your device.\n" +
                            "• You can always tap into a result to see more details and make an informed decision.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                // Stay secure
                SectionTitle("Stay Secure", textColor)

                Text(
                    text = "Use the scan feature regularly to stay ahead of threats. We’re here to make mobile security easy, transparent, and in your control.\n",
                    color = textColor,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Thank you for using Shade!\n\nSincerely,\nTeam Shade",
                    color = textColor,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Left
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, color: Color) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title,
        color = color,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(4.dp))
}


