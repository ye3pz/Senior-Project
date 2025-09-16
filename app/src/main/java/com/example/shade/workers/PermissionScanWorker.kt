package com.example.shade.workers

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters



class PermissionScanWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d("PermissionScanWorker", "Running permission scan...")

            val results = scanAppPermissions(context) // no context if your function doesn't take one

            results.forEach { (packageName, permissions) ->
                Log.d("PermissionScanWorker", "$packageName uses: $permissions")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PermissionScanWorker", "Error scanning permissions", e)
            Result.failure()
        }
    }
}

fun scanAppPermissions(context: Context): Map<String, List<String>> {
    val pm: PackageManager = context.packageManager
    val results = mutableMapOf<String, List<String>>()

    // Retrieves list of installed apps
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    for (app in apps) {
        try {
            val pkgInfo: PackageInfo =
                pm.getPackageInfo(app.packageName, PackageManager.GET_PERMISSIONS)

            val permissions = pkgInfo.requestedPermissions?.toList() ?: emptyList()

            if (permissions.isNotEmpty()) {
                results[app.packageName] = permissions
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("Permissions", "Error checking app permissions", e)
        }
    }

    return results
}

