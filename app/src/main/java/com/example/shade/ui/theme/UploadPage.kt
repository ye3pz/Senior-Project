package com.example.shade.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.example.shade.utils.UploadHelper
import com.example.shade.utils.ThreatItem
import com.example.shade.utils.ThreatLevel
import com.example.shade.utils.LoadingOverlay
import com.example.shade.ui.viewmodel.HistoryViewModel


object ScanCallbacks {
    var onScanCompleted: ((ThreatItem?) -> Unit)? = null
    var onHashCheckResult: ((status: String, fileName: String) -> Unit)? = null
}

@Composable
fun UploadPage(
    onMenuClick: () -> Unit,
    historyViewModel: HistoryViewModel,
) {
    val context = LocalContext.current
    val uploadHelper = remember { UploadHelper(context) }
    val coroutineScope = rememberCoroutineScope()
    var scanResult by remember { mutableStateOf<ThreatItem?>(null) }
    var hashStatus by remember { mutableStateOf<Pair<String, String>?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        ScanCallbacks.onScanCompleted = { result ->
            scanResult = result
        }

        ScanCallbacks.onHashCheckResult = { status, fileName ->
            hashStatus = status to fileName
        }

        onDispose {
            ScanCallbacks.onScanCompleted = null
            ScanCallbacks.onHashCheckResult = null
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploading = true
                uploadHelper.processFileUri(it)
                isUploading = false
                historyViewModel.refreshHistory()
            }
        }
    }

    val background = Color(0xFF311A57)      // dark purple
    val cardColor = Color(0xFF5A3B8D)       // lighter purple for cards
    val textColor = Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {

        //Top bar
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
                text = "Upload APK",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
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

        // Upload card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(18.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CloudUpload,
                    contentDescription = "Upload",
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Upload APK File",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Select an APK file to scan for\npotential security threats",
                fontSize = 13.sp,
                color = Color(0xFFE5DDF8),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // “Choose File” button
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(20.dp))
                    .clickable {
                        filePickerLauncher.launch("application/vnd.android.package-archive")
                    }
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Choose File",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cardColor
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // How it works card
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .background(cardColor, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "How it works",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "• Upload your APK file for analysis\n" +
                        "• Our system scans for malware and\n  suspicious permissions\n" +
                        "• Receive a detailed risk assessment\n  and recommendations",
                fontSize = 13.sp,
                color = Color(0xFFE5DDF8),
                lineHeight = 18.sp
            )
        }
        if(hashStatus !=null){
            val (status, fileName) = hashStatus!!
            HashResultDialog(
                fileName = fileName,
                status = status,
                onDismiss = { hashStatus = null }
            )
        }
        if (scanResult != null) {
            ScanResultDialog(
                threatItem = scanResult!!,
                onDismiss = { scanResult = null }
            )
        }
    }
    if (isUploading) {
        LoadingOverlay("Uploading and scanning APK…")
    }

}

@Composable
fun ScanResultDialog(
    threatItem: ThreatItem,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Scan Result: ${threatItem.threatLevel}",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("File: ${threatItem.title}")
                Text("Confidence: ${threatItem.threatLevel}%")
                Text("Summary: ${threatItem.description}")
                Spacer(modifier = Modifier.height(8.dp))

                if (threatItem.threatLevel == ThreatLevel.HIGH) {
                    Text("⚠ Malware detected", color = Color.Red)
                } else {
                    Text("✓ No threats found", color = Color.Green)
                }
            }
        },
        confirmButton = {
            Text(
                "OK",
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    )
}

@Composable
fun HashResultDialog(
    fileName: String,
    status: String, // "MALWARE" or "SAFE"
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (status == "MALWARE") "Malware Detected" else "File is Safe",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("File: $fileName")
                Spacer(modifier = Modifier.height(8.dp))
                if (status == "MALWARE") {
                    Text("$fileName is known to be malicious!", color = Color.Red)
                } else {
                    Text("$fileName is safe.", color = Color.Green)
                }
            }
        },
        confirmButton = {
            Text(
                "OK",
                modifier = Modifier.clickable { onDismiss() }
            )
        }
    )
}


