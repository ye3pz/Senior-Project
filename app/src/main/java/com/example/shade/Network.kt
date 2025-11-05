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
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import  com.example.shade.data.FirebaseClient
import com.example.shade.data.ThreatItem



object ThreatsList {
    val threatsList = mutableListOf<ThreatItem>()

    val activeThreats = mutableListOf<ThreatItem>()
}
class Network : VpnService() {
    val tag = "NetworkMonitor"
    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnOutput: FileOutputStream? = null
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


        FirebaseClient.getUntrustedIps { ipList ->

            // Populate the list for internal use
            ThreatsList.threatsList.clear()
            ipList.forEach { ip -> ThreatsList.threatsList.add(ip) }
            // Get untrusted IPs from Firebase before building VPN interface
            vpnInterface = Builder()
                .addAddress("10.0.0.2", 32) // Local dummy VPN IP
                .setSession("ShadeVPN")
                .setBlocking(true)
                .addRoute("0.0.0.0", 24)    // Covers 0.0.0.0 through 127.255.255.255
                .addRoute("128.0.0.0", 24) // Covers 128.0.0.0 through 255.255.255.255
                .addDnsServer("192.168.1.1")

                .establish()

            if (vpnInterface != null) {
                vpnOutput = FileOutputStream(vpnInterface!!.fileDescriptor) // Define output stream
                monitorJob = CoroutineScope(Dispatchers.IO).launch {
                    readPackets()
                }
            } else {
                Log.e(tag, "Failed to build VPN interface")
            }
        }


        return START_STICKY
    }

    private suspend fun readPackets() {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val output = vpnOutput?: return
        val buffer = ByteArray(32767)

        while (currentCoroutineContext().isActive) {
            val length = try{
                input.read(buffer)
            } catch(e: Exception){
                Log.e(tag, "Length is less than 0", e)
                break
            }

            if (length > 0) {

                val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
                val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"
                Log.d(tag , "Connection: $srcIp → $dstIp")
                appTrafficCounter["unknown"] = appTrafficCounter.getOrDefault("unknown", 0) + 1
                val activeThreat = ThreatsList.threatsList.find { threatItem ->
                    dstIp == threatItem.title
                }
                if(activeThreat != null) {
                        // If active threat isnt on the list a;ready then add it to it
                    if (!ThreatsList.activeThreats.any { it.title == activeThreat.title }) {
                        ThreatsList.activeThreats.add(activeThreat)
                        Log.i(tag, "Added ${activeThreat.title} to ThreatList")
                        }
                    continue
                    }
                try {
                    output.write(buffer,0,length)
                } catch(e: Exception){
                    Log.e(tag, "Failed to write packet ro VPN tunnel output", e)
                }
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
