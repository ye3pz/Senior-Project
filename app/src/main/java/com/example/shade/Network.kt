package com.example.shade

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import  com.example.shade.data.FirebaseClient
import com.example.shade.utils.ThreatItem



object ThreatsList {
    val threatMap = ConcurrentHashMap<String, ThreatItem>()

    val activeThreats = mutableListOf<ThreatItem>()
   // @Volatile var isReady = false
}

val appsToMonitor = listOf(
    "com.example.malwaretester",
)

class Network : VpnService() {
    val tag = "NetworkMonitor"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var monitorJob: Job? = null

    companion object {
        val appTrafficCounter = ConcurrentHashMap<String, Int>()
    }

    private val cpuUsageMap = ConcurrentHashMap<String, Long>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FirebaseClient.cleanInvalidIpsFromFirebase()
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "shade_channel")
            .setContentTitle("Shade is monitoring")
            .setContentText("Scanning app traffic in background")
            .setSmallIcon(R.drawable.ic_shield) // Replace with your icon
            .build()

        startForeground(1, notification)

        if (monitorJob?.isActive == true) {
            Log.w(tag, "VPN already running — ignoring duplicate start")
            return START_STICKY
        }

        FirebaseClient.getUntrustedIps { ipList ->

            // --- KEY CHANGE: Populate the new map instead of the list ---
            ThreatsList.threatMap.clear()
            ipList.forEach { ip -> ThreatsList.threatMap[ip.title] = ip }
            //ThreatsList.isReady = true
            // Get untrusted IPs from Firebase before building VPN interface


            vpnInterface = Builder()
                .addAddress("10.8.0.2", 8)
                .setSession("ShadeVPN")
                .setBlocking(true)
                //.addRoute("0.0.0.0",0)
                .addDnsServer("8.8.8.8")
               .addDnsServer("1.1.1.1")
                .apply {
                    // Collect /24 subnets
                    val subnets = mutableSetOf<String>()
                    for (ipKey in ThreatsList.threatMap.keys) {
                        val ip = ipKey.replace('_', '.')
                        val parts = ip.split('.')
                        if (parts.size == 4) {
                            val subnet = "${parts[0]}.${parts[1]}.${parts[2]}.0"
                            subnets.add(subnet)
                        }
                    }

// Add one route per /24 network
                    subnets.take(200).forEach { subnet ->
                        try {
                            addRoute(subnet, 24)
                            Log.d("ShadeVPN", "Added route for $subnet/24")
                        } catch (e: Exception) {
                            Log.e("ShadeVPN", "Route add failed: $subnet", e)
                        }
                    }
                }
                .establish()

            if (vpnInterface != null) {
                Log.i(tag, ">>> Starting readPackets() coroutine <<<")
                monitorJob = CoroutineScope(Dispatchers.IO).launch {
                    startCpuMonitoring()
                    while (isActive) {
                        updateAppTrafficCounters(appsToMonitor)
                        delay(5000) // every 5 seconds
                    }
                }
            } else {
                // If establish() fails here (e.g., VPN permission revoked, system error)
                Log.e(tag, "Failed to build VPN interface. Shutting down service.")
                stopSelf() // Shut down the service cleanly if setup failed.
            }
        }



        return START_STICKY
    }

    fun updateAppTrafficCounters(packageNames: List<String>) {
        packageNames.forEach { pkg ->
            try {
                val uid = packageManager.getApplicationInfo(pkg, 0).uid
                val rxBytes = android.net.TrafficStats.getUidRxBytes(uid)
                val txBytes = android.net.TrafficStats.getUidTxBytes(uid)

                val totalBytes = (rxBytes.takeIf { it >= 0 } ?: 0) + (txBytes.takeIf { it >= 0 } ?: 0)
                appTrafficCounter[pkg] = totalBytes.toInt()

                Log.i(tag, "App: $pkg → RX: $rxBytes, TX: $txBytes, Total: $totalBytes")
            } catch (e: Exception) {
                Log.e(tag, "Error fetching traffic for $pkg", e)
            }
        }
    }







    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel("shade_channel", "Shade VPN", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        Log.e(tag, "Monutir Job is being canceled?", Throwable())
        monitorJob?.cancel()

        Log.e("VPNFD", "vpnInterface.close() CALLED in onDestory!", Throwable())
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }
    override fun onRevoke() {
        Log.w(tag, "VPN revoked by system!")
        monitorJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(tag, "Error closing on revoke", e)
        }
        stopSelf()
    }

    private fun startCpuMonitoring() {
        CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                appsToMonitor.forEach { pkg ->
                    val usage = Permissions(applicationContext).getBackgroundCpuUsage(pkg)
                    cpuUsageMap[pkg] = usage
                }
                delay(5000) // collect CPU usage every 5 seconds
            }
        }
    }

}
