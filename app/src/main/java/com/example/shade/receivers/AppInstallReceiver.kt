package com.example.shade.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.shade.workers.PermissionScanWorker

class AppInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Trigger PermissionScanWorker when an app is installed or permissions change
        if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
            intent.action == Intent.ACTION_PACKAGE_CHANGED
        ) {

            // Create the work request for PermissionScanWorker
            val permissionScanRequest =
                OneTimeWorkRequest.Builder(PermissionScanWorker::class.java).build()

            // Enqueue the work request to start the worker
            WorkManager.getInstance(context).enqueue(permissionScanRequest)
        }
    }
}