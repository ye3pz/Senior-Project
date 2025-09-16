package com.example.shade.receivers


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import com.example.shade.NetworkMonitorService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val vpnIntent = VpnService.prepare(context)
            if (vpnIntent == null) {
                context.startService(Intent(context, NetworkMonitorService::class.java))
            }
        }
    }
}
