package com.example.shade

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.*
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

class NetworkMonitorService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    private var monitorJob: Job? = null
    val appTrafficCounter = ConcurrentHashMap<String, Int>() // Tracks request counts by app package name

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vpnInterface = Builder()
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)
            .setSession("ShadeVPN")
            .setBlocking(true)
            .establish()

        monitorJob = CoroutineScope(Dispatchers.IO).launch {
            readPackets()
        }

        return START_STICKY
    }

    private fun readPackets() {
        val input = FileInputStream(vpnInterface?.fileDescriptor)
        val buffer = ByteArray(32767)

        while (true) {
            val length = input.read(buffer)
            if (length > 0) {
                val byteBuffer = ByteBuffer.wrap(buffer, 0, length)
                val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
                Log.d("NetworkMonitor", "Data from IP: $srcIp")
                appTrafficCounter["unknown"] = appTrafficCounter.getOrDefault("unknown", 0) + 1
            }
        }
    }

    override fun onDestroy() {
        monitorJob?.cancel()
        vpnInterface?.close()
        super.onDestroy()
    }

    fun getRequestCountForApp(packageName: String): Int {
        return appTrafficCounter.getOrDefault(packageName, 0)
    }
}
