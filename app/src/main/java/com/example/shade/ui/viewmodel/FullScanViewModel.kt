package com.example.shade.ui.viewmodel

import android.app.Application
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Environment
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.shade.data.FirebaseClient
import com.example.shade.Permissions
import com.example.shade.ThreatsList
import com.example.shade.utils.ThreatItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.security.MessageDigest

class FullScanViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "FullScan"
    private val permissions = Permissions(application)
    private val firebase = FirebaseClient
    private val pm: PackageManager = application.packageManager

    private val _scanLogs = MutableStateFlow<List<String>>(emptyList())
    val scanLogs: StateFlow<List<String>> get() = _scanLogs

    private val _scanCompleted = MutableStateFlow(false)
    val scanCompleted: StateFlow<Boolean> get() = _scanCompleted

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning

    var onScanCompleted: (() -> Unit)? = null

    val activeThreats = ThreatsList.activeThreats

    fun startFullScan() {
        Log.i(tag, "Full scan starting")
        viewModelScope.launch(Dispatchers.IO) {
            _isScanning.value = true
            _scanCompleted.value = false
            val logs = mutableListOf<String>()
            val threats = ThreatsList.activeThreats

            Log.i(tag, "Starting FULL SCAN...")

            permissions.requestUsageStatsPermission()
            permissions.scanAppPermissions()
            permissions.checkNumberOfPermissions()
            permissions.scanAppSignatures()

            logs.add("Scanning installed apps...")
            scanInstalledApps(logs)

            logs.add("Fetching untrusted signatures from Firebase...")
            val firebaseSignatures = fetchFirebaseSignatures()
            logs.add("Loaded ${firebaseSignatures.size} untrusted signatures.")

            logs.add("Scanning external storage...")
            scanExternalStorage(logs, threats, firebaseSignatures)

            logs.add("Scanning Downloads directory for APKs...")
            scanDownloadsForAPKs(logs, threats)

            Log.i(tag, "FULL SCAN COMPLETE")

            _isScanning.value = false
            _scanLogs.value = logs
            _scanCompleted.value = true
            onScanCompleted?.invoke()
        }
    }



    // 1. Scan installed apps
    suspend private fun scanInstalledApps(logs: MutableList<String>) {
        Log.i(tag, "starting scan of installed apps")
        val apps = pm.getInstalledPackages(PackageManager.GET_PERMISSIONS or
                PackageManager.GET_RECEIVERS or
                PackageManager.GET_SERVICES)

        for (app in apps) {
            try {
                val appInfo = app.applicationInfo ?: continue
                val appName = pm.getApplicationLabel(appInfo)
                val riskScore = permissions.getSafetyRating(app)
                val label = permissions.getSafetyLabel(riskScore)

                logs.add("App: ${app.packageName} → $label")
                Log.i(tag, "App: ${app.packageName}, Score: $riskScore, Label: $label")

                if (label == "Dangerous" || label == "Moderate") {
                    val appThreatItem = permissions.toThreatItem(app, appName)
                    activeThreats.add(appThreatItem)
                }

            } catch (e: Exception) {
                logs.add("Error scanning ${app.packageName}")
                Log.e(tag, "Error scanning ${app.packageName}", e)
            }
        }
    }


    // 2. Scan external storage using Firebase malware signatures
    private fun scanExternalStorage(
        logs: MutableList<String>,
        threats: MutableList<ThreatItem>,
        firebaseSignatures: List<String>
    ) {
        Log.i(tag, "starting Externernal directory scan")
        val dirsToScan = listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        )
        try {
            for (dir in dirsToScan) {
                scanDirectory(dir, logs, threats, firebaseSignatures)
            }
        }  catch (e: Exception){
            Log.e(tag, "failed external storage scan, e")
        }
    }

    private fun scanDirectory(
        dir: File?,
        logs: MutableList<String>,
        threats: MutableList<ThreatItem>,
        firebaseSignatures: List<String>
    ) {
        Log.i(tag, "starting directory scan")
        if (dir == null || !dir.exists()){
            Log.e(tag, "no directories found")
            return
        }


        val maxFilesToScan = 3000
        var scanned = 0

        try {
            val walk = dir.walk()
            for (file in walk) {
                if (scanned >= maxFilesToScan){
                    Log.i(tag, "scanned more than max ")
                    break
                }
                scanned++

                if (!file.isFile) continue

                val sizeMb = file.length() / (1024 * 1024)
                if (sizeMb > 20) {
                    logs.add("Large file detected: ${file.name} (${sizeMb}MB)")
                }

                try {
                    val fileMd5 = md5(file)

                    if (firebaseSignatures.contains(fileMd5)) {
                        logs.add("⚠ Firebase Malware Signature Match: ${file.name}")
                        activeThreats.add(
                            ThreatItem(
                                title = file.name,
                                description = "A known malicious signature (MD5) was detected.\nSource: Firebase Signature DB"
                            )
                        )
                    }
                } catch (e: Exception) {
                    // MD5 / read error for this file — log and continue
                    Log.w(tag, "Failed to hash file ${file.absolutePath}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error walking directory ${dir.absolutePath}", e)
        }
    }


    // 3. Scan for suspicious APKs
    private fun scanDownloadsForAPKs(logs: MutableList<String>, threats: MutableList<ThreatItem>) {

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val apkFiles = downloads.listFiles { _, name -> name.endsWith(".apk") } ?: return

        for (apk in apkFiles) {
            logs.add("APK found: ${apk.name}")

            if (apk.length() > 150 * 1024 * 1024) {
                logs.add("⚠ Suspicious large APK: ${apk.name}")
            }
        }
    }


    // 4. MD5 hashing
    private fun md5(file: File): String {
        val md = MessageDigest.getInstance("MD5")
        file.inputStream().use { fis ->
            val buffer = ByteArray(2048)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    // -------------------------------------------------------------------
    // 5. Firebase signature fetch
    // -------------------------------------------------------------------
    private suspend fun fetchFirebaseSignatures(): List<String> {
        return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
            firebase.getUntrustedSignatures { items ->
                // We only return the MD5 key titles
                cont.resume(items.map { it.title }, null)
            }
        }
    }

}
