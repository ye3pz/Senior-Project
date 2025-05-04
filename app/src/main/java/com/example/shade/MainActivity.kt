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

    private lateinit var permissions: Permissions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Enable edge-to-edge layout (if applicable)
        enableEdgeToEdge()

        // Initialize Permissions instance
        permissions = Permissions(this)

        // Find the scan button and set the click listener
        val scanButton: Button = findViewById(R.id.scan)
        scanButton.setOnClickListener {
            if (permissions.hasUsageStatsPermission()) {
                startScan()
            } else {
                permissions.requestUsageStatsPermission()
            }
        }
    }

    private fun startScan() {
        // Request permission if not already granted
        permissions.requestUsageStatsPermission()
        // Scan for apps with excessive permissions
        permissions.scanAppPermissions()

        permissions.checkNumberOfPermissions()

        permissions.scanAppSignatures()

        val pm = packageManager
        val apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)

        for (app in apps) {
            try {
                val riskScore = permissions.getSafetyRating(app)
                val label = permissions.getSafetyLabel(riskScore)

                // Log and optionally show toast (for debug/demo purposes)
                Log.i("AppSafety", "App: ${app.packageName}, Score: $riskScore, Label: $label")
                Toast.makeText(this, "App: ${app.packageName} → $label", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Log.e("MainActivity", "Error evaluating safety for ${app.packageName}", e)
            }
        }
    }
}

