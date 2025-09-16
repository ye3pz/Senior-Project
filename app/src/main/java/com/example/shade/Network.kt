package com.example.shade

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.util.concurrent.ConcurrentHashMap
import  com.example.shade.data.FirebaseClient

class NetworkMonitorService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var monitorJob: Job? = null
    val appTrafficCounter = ConcurrentHashMap<String, Int>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "shade_channel")
            .setContentTitle("Shade is monitoring")
            .setContentText("Scanning app traffic in background")
            .setSmallIcon(R.drawable.ic_shield) // Replace with your icon
            .build()

        startForeground(1, notification)

        // Get untrusted IPs from Firebase before building VPN interface
        FirebaseClient.getUntrustedIps { ipList ->
            vpnInterface = Builder()
                .addAddress("10.0.0.2", 32) // Local dummy VPN IP
                .setSession("ShadeVPN")
                .setBlocking(true)
                .apply {
                    for (ip in ipList) {
                        try {
                            addRoute(ip, 32) // block each untrusted IP
                            Log.i("NetworkMonitor", "Blocking untrusted IP: $ip")
                        } catch (e: Exception) {
                            Log.e("NetworkMonitor", "Invalid IP format: $ip", e)
                        }
                    }
                }
                .establish()

            monitorJob = CoroutineScope(Dispatchers.IO).launch {
                readPackets()
            }
        }

        return START_STICKY
    }

    private fun readPackets() {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteArray(32767)

        while (true) {
            val length = input.read(buffer)
            if (length > 0) {

                val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
                val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"
                Log.d("NetworkMonitor", "Connection: $srcIp → $dstIp")
                appTrafficCounter["unknown"] = appTrafficCounter.getOrDefault("unknown", 0) + 1
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
        monitorJob?.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }
}
