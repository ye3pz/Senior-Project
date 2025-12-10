package com.example.shade

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.content.pm.PackageManager
import android.content.pm.ApplicationInfo
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.shade.data.FirebaseClient
import com.example.shade.utils.ThreatItem
import com.example.shade.utils.ThreatLevel
import com.example.shade.utils.TrustedApps
import android.app.usage.UsageStatsManager

class Permissions(private val context: Context) {
    val tag = "permissions"
    val lastPermissionAlerts = mutableListOf<String>()


    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            AppOpsManager.MODE_IGNORED
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun requestUsageStatsPermission() {
        if (!hasUsageStatsPermission()) {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            context.startActivity(intent)
        } else {
            //Toast.makeText(context, "Package Usage Stats permission granted.", Toast.LENGTH_SHORT).show()
            Log.i(tag, "Package Usage Stats permission granted." )
        }
    }

    fun scanAppPermissions() {
        // Get the PackageManager instance to interact with installed apps
        val pm: PackageManager = context.packageManager

        //Retrieves list of installed apps
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            if(isSystemApp(app)) continue
            try {
                // Get the package info for the current app
                val pkgInfo: PackageInfo =
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)

                // Extract the list of requested permissions for the app
                val permissions = pkgInfo.requestedPermissions

                permissions?.forEach { permission ->
                    // Check if the permission is considered "dangerous"
                    if (permission == android.Manifest.permission.CAMERA ||
                        permission == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                        permission == android.Manifest.permission.READ_PHONE_STATE ||
                        permission == android.Manifest.permission.READ_CONTACTS ||
                        permission == android.Manifest.permission.READ_SMS
                    ) {
                        Log.i(tag, "App ${app.packageName} has dangerous permissions.")
                    }
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Permissions", "Error checking app permissions", e)
                e.printStackTrace()
            }
        }
    }

    fun checkNumberOfPermissions() {
        val pm: PackageManager = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            if(isSystemApp(app)) continue
            try {
                val pkgInfo: PackageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = pkgInfo.requestedPermissions

                if (permissions != null && permissions.size > 10) {
                    //showToast("App ${app.packageName} requests a large number of permissions.")
                    Log.i(tag, "App ${app.packageName} requests a large number of permissions.",)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e("Permissions", "Error checking app permissions", e)
            }
        }
    }


    fun scanAppSignatures() {
        val pm: PackageManager = context.packageManager

        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            if(isSystemApp(app)) continue
            try {
                val pkgInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA)
                // Get the signing certificates for the app, handling newer and older versions of Android
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    // For older Android versions
                    pkgInfo.signatures
                }
                // Convert the signature array to a string, or default to "No signature" if signatures are null
                val signatureInfo = signatures?.joinToString { it.toCharsString() } ?: "No signature"

                val developerName = app.packageName

                Log.i("AppSignatureInfo", "App: ${app.packageName}, Signature: $signatureInfo, Developer: $developerName")

            } catch (e: Exception) {
                Log.e("Permissions", "Error retrieving signature or developer info for ${app.packageName}", e)
            }
        }
    }
    suspend fun getSafetyRating(packageInfo: PackageInfo): Int {
        val pm = context.packageManager
        val appInfo = packageInfo.applicationInfo
        var riskScore = 0

        if (isSystemApp(appInfo)) {
            return 0 // always safe
        }
        val installer = try {
            pm.getInstallSourceInfo(appInfo!!.packageName).installingPackageName
        } catch (e: Exception) {
            null
        }

        // 3. If installer is trusted → very low risk
        if (TrustedApps.isTrustedInstaller(installer)) {
            return 1
        }

        // Check trusted non-system app (exact or prefix)
        if (TrustedApps.isTrusted(packageInfo.packageName)) {
            return 1 // minimum safe-but-not-zero risk
        }


        val permissions = try {
            pm.getPackageInfo(packageInfo.packageName, PackageManager.GET_PERMISSIONS).requestedPermissions
        } catch (e: Exception) {
            null
        }

        permissions?.forEach { perm ->
            try {
                val info = pm.getPermissionInfo(perm, 0)
                val isDangerous = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    // API 28+ safe check
                    (info.protectionFlags and PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) == 0 &&
                            (info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
                } else {
                    // API 26–27 fallback
                    (info.protectionLevel and PermissionInfo.PROTECTION_MASK_BASE) == PermissionInfo.PROTECTION_DANGEROUS
                }

                if (isDangerous) {
                    riskScore += 3
                    lastPermissionAlerts.add("Requests dangerous permission: $perm")
                }
            } catch (_: Exception) {}
        }

        // Usage stats permission check
        val uid = packageInfo.applicationInfo?.uid ?: -1
        if (uid != -1) {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                uid,
                packageInfo.packageName
            )
            if (mode == AppOpsManager.MODE_ALLOWED) {
                riskScore += 10
                lastPermissionAlerts.add("App has Usage Stats access (can monitor other apps)")
            }
        }
        // If not a system app, increase risk
        if ((packageInfo.applicationInfo?.flags ?: 0 and ApplicationInfo.FLAG_SYSTEM) == 0) {
            riskScore += 2
            lastPermissionAlerts.add("App is sideloaded or non-system (higher risk)")
        }

        // Signature trust check
        if (!isSignatureTrusted(packageInfo)) {
            riskScore += 5
            lastPermissionAlerts.add("App signature is NOT trusted")
        }

        // System alert winddow check
        if (permissions?.contains(android.Manifest.permission.SYSTEM_ALERT_WINDOW) == true) {
            riskScore += 8
            lastPermissionAlerts.add("Uses SYSTEM_ALERT_WINDOW (can draw over apps / phishing)")
        }

    // Boot receiver check
        val hasBootReceiver = packageInfo.receivers?.any { receiver ->
            receiver?.name?.contains("boot", ignoreCase = true) == true
        } ?: false

        if (hasBootReceiver) {
            riskScore += 5
            lastPermissionAlerts.add("Runs at boot (persistence mechanism)")
        }

        // Accessibility service check
        val hasAccessibility = packageInfo.services?.any { service ->
            service.permission == "android.permission.BIND_ACCESSIBILITY_SERVICE"
        } ?: false

        if (hasAccessibility) {
            riskScore += 10
            lastPermissionAlerts.add("Accessibility Service detected (full control of device)")
        }

        val traffic = Network.appTrafficCounter[packageInfo.packageName] ?: 0
        if (traffic > 1024 * 100) { // more than 100 KB in the last 5s window
            riskScore += 2
            lastPermissionAlerts.add("App sending high network traffic: $traffic bytes")
        }

        val backgroundTime = getBackgroundCpuUsage(packageInfo.packageName)

        if(backgroundTime > 12000){
            riskScore += 8
        }

        return riskScore
    }


    fun getSafetyLabel(score: Int): String {
        return when {
            score < 10 -> "Safe"
            score < 20 -> "Moderate"
            else -> "Dangerous"
        }
    }

    suspend fun isSignatureTrusted(packageInfo: PackageInfo): Boolean {
        val appInfo = packageInfo.applicationInfo
        if (isSystemApp(appInfo)) return true
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        } ?: return true // no signatures → assume trusted

        val signatureBytes = signatures.first().toByteArray()
        val md5 = java.security.MessageDigest.getInstance("MD5")
            .digest(signatureBytes)
            .joinToString("") { "%02x".format(it) }

        val isUntrusted = FirebaseClient.checkSignatureUntrusted(md5)
        return !isUntrusted
    }

    private fun isSystemApp(app: ApplicationInfo?): Boolean {
        if(app == null) return false
        return (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0 ||
                (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    suspend fun toThreatItem(pkg: PackageInfo, appName: CharSequence): ThreatItem {

        lastPermissionAlerts.clear()

        val riskScore = getSafetyRating(pkg)

        val threatLevel = when {
            riskScore >= 20 -> ThreatLevel.HIGH
            riskScore >= 10 -> ThreatLevel.MEDIUM
            else -> ThreatLevel.SAFE
        }


        val displayName = resolveAppName(pkg.packageName)

        val descriptionText =
            "App Name: $displayName" +
                    "\nVersion: ${pkg.versionName ?: "N/A"}" +
                    "\nRisk Score: $riskScore" +
                    "\nSource: App Permission Scan"

        return ThreatItem(
            title =  displayName,
            description = descriptionText,
            dangers = lastPermissionAlerts.toList(),
            riskScore = riskScore,
            threatLevel = threatLevel,
            source = "Permission Scan"
        )

    }
    fun getBackgroundCpuUsage(packageName: String): Long {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val end = System.currentTimeMillis()
        val start = end - (1000 * 60 * 60) // last 1 hour

        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)

        stats?.forEach { usage ->
            if (usage.packageName == packageName) {
                // Available only API 29+
                return usage.totalTimeForegroundServiceUsed
            }
        }

        return 0L
    }

    fun resolveAppName(packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName // fallback to package name if not found
        }
    }
}

