package com.example.shade.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shade.Permissions
import com.example.shade.ThreatsList
import com.example.shade.utils.ThreatItem
import com.example.shade.utils.ThreatLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class QuickScanViewModel(application: Application) : AndroidViewModel(application) {
    val tag = "quickScan"
    private val permissions = Permissions(application)
    private val pm: PackageManager = application.packageManager

    // Expose scan results as StateFlow
    private val _scanLogs = MutableStateFlow<List<String>>(emptyList())
    val scanLogs: StateFlow<List<String>> = _scanLogs

    private val _scanCompleted = MutableStateFlow(false)
    val scanCompleted: StateFlow<Boolean> = _scanCompleted

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    var onScanCompleted: (() -> Unit)? = null

    fun startQuickScan() {
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanCompleted.value = false
            try {
                Log.i(tag, "starting quick scan")
                permissions.requestUsageStatsPermission()
                permissions.scanAppPermissions()
                permissions.checkNumberOfPermissions()
                permissions.scanAppSignatures()

                val apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS)
                val activeThreats = ThreatsList.activeThreats
                val logs = mutableListOf<String>()

                for (app in apps) {
                    try {
                        val appInfo = app.applicationInfo ?: continue
                        val appName = pm.getApplicationLabel(appInfo)
                        val riskScore = permissions.getSafetyRating(app)
                        val label = permissions.getSafetyLabel(riskScore)

                        logs.add("App: ${app.packageName} → $label")
                        Log.i("QuickScan", "App: ${app.packageName}, Score: $riskScore, Label: $label")

                        if (label == "Dangerous") {
                            val appThreatItem = permissions.toThreatItem(app, appName)
                            activeThreats.add(appThreatItem)
                        }
                    } catch (e: Exception) {
                        Log.e("QuickScan", "Error evaluating ${app.packageName}", e)
                    }
                }
                Log.i(tag,"finished scan")
                _scanLogs.value = logs
                _isScanning.value = false
                _scanCompleted.value = true

                onScanCompleted?.invoke()

            } catch (e: Exception) {
                Log.e("QuickScan", "Scan failed", e)
            }
        }
    }
}
