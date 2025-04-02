package com.example.shade

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import org.pcap4j.core.PcapHandle
import org.pcap4j.core.Pcaps
import org.pcap4j.core.PcapNetworkInterface
import org.pcap4j.core.PcapNativeException
import java.util.*
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import android.util.Log
import androidx.core.content.ContextCompat.startActivity


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable edge-to-edge layout (if applicable)
        enableEdgeToEdge()

        // Find the scan button and set the click listener
        val scanButton: Button = findViewById(R.id.scan)
        scanButton.setOnClickListener {
            hasUsageStatsPermission()
            // Trigger the packet capture and permission scan
            startPacketCaptureAndScan()
        }
    }

    private fun startPacketCaptureAndScan() {
       requestUsageStatsPermission()
        // Scan for apps with excessive permissions
        scanAppPermissions()
    }




    private fun scanAppPermissions() {
        // Scan installed apps and check their requested permissions
        val pm: PackageManager = packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        // Iterate over the installed apps
        for (app in apps) {
            try {
                val pkgInfo: PackageInfo =
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = pkgInfo.requestedPermissions

                // Check for dangerous permissions (e.g., CAMERA, LOCATION)
                permissions?.forEach { permission ->
                    if (permission == android.Manifest.permission.CAMERA ||
                        permission == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                        permission == android.Manifest.permission.READ_PHONE_STATE
                    ) {
                        // Show a warning if an app requests dangerous permissions
                        runOnUiThread {
                            showToast("App ${app.packageName} has dangerous permissions.")
                        }
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("PacketCapture", "Error setting up packet capture", e)
                e.printStackTrace()
            }
        }
    }

    fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                packageName
            )
        } else {
            AppOpsManager.MODE_IGNORED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            // Permission not granted, prompt the user to enable it
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        } else {
            // Permission already granted, proceed with your logic
            Toast.makeText(this, "Package Usage Stats permission granted.", Toast.LENGTH_SHORT)

        }
    }
    private fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}