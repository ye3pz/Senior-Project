package com.example.shade

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PermissionInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity

class Permissions(private val context: Context) {

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
            Toast.makeText(context, "Package Usage Stats permission granted.", Toast.LENGTH_SHORT).show()
        }
    }

    fun scanAppPermissions() {
        val pm: PackageManager = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in apps) {
            try {
                val pkgInfo: PackageInfo =
                    pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = pkgInfo.requestedPermissions

                permissions?.forEach { permission ->
                    if (permission == android.Manifest.permission.CAMERA ||
                        permission == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                        permission == android.Manifest.permission.READ_PHONE_STATE ||
                        permission == android.Manifest.permission.READ_CONTACTS ||
                        permission == android.Manifest.permission.READ_SMS
                    ) {
                        showToast("App ${app.packageName} has dangerous permissions.")
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
            try {
                val pkgInfo: PackageInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)
                val permissions = pkgInfo.requestedPermissions

                if (permissions != null && permissions.size > 10) {
                    showToast("App ${app.packageName} requests a large number of permissions.")
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
            try {
                val pkgInfo = pm.getPackageInfo(app.packageName, PackageManager.GET_SIGNING_CERTIFICATES or PackageManager.GET_META_DATA)
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pkgInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    pkgInfo.signatures
                }

                val signatureInfo = signatures?.joinToString { it.toCharsString() } ?: "No signature"
                val developerName = app.packageName // Alternatively, fetch from metadata if available

                Log.i("AppSignatureInfo", "App: ${app.packageName}, Signature: $signatureInfo, Developer: $developerName")

            } catch (e: Exception) {
                Log.e("Permissions", "Error retrieving signature or developer info for ${app.packageName}", e)
            }
        }
    }
    fun getSafetyRating(packageInfo: PackageInfo): Int {
        val pm = context.packageManager
        var riskScore = 0

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
                } else {
                    riskScore += 1
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
            }
        }

        // If not a system app, increase risk
        if ((packageInfo.applicationInfo?.flags ?: 0 and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0) {
            riskScore += 5
        }

        // Signature trust check
        if (!isSignatureTrusted(packageInfo)) {
            riskScore += 5
        }

        val requestCount = NetworkMonitorService().getRequestCountForApp(packageInfo.packageName)
        riskScore += addNetworkRiskScore(packageInfo, requestCount)

        return riskScore
    }
    fun addNetworkRiskScore(packageInfo: PackageInfo, requestCount: Int): Int {
        return when {
            requestCount > 500 -> 10
            requestCount > 100 -> 5
            requestCount > 10 -> 2
            else -> 0
        }
    }


    fun getSafetyLabel(score: Int): String {
        return when {
            score < 10 -> "Safe"
            score < 20 -> "Moderate"
            else -> "Dangerous"
        }
    }

    fun isSignatureTrusted(packageInfo: PackageInfo): Boolean {
        // Example: Accept all for now; insert trusted signature hashes here if needed
        return true
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
