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
import java.net.SocketTimeoutException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.util.concurrent.Executors
import java.net.Socket
import kotlinx.coroutines.sync.withLock
import com.google.firebase.FirebaseApp


import java.net.InetAddress



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
    val appTrafficCounter = ConcurrentHashMap<String, Int>()
    var badIP = ""

    private val packetChannel = Channel<ByteArray>(capacity = 1000) // buffered channel
    private val ioLock = Mutex() // ensures vpnInterface is used safely

    private val readDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, "ShadeReadThread").apply { priority = Thread.MAX_PRIORITY }
    }.asCoroutineDispatcher()



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
                  // readPackets()
                }
            } else {
                // If establish() fails here (e.g., VPN permission revoked, system error)
                Log.e(tag, "Failed to build VPN interface AFTER threat list was loaded. Shutting down service.")
                stopSelf() // Shut down the service cleanly if setup failed.
            }
        }



        return START_STICKY
    }

    private suspend fun readPackets() {
        Log.i(tag, "read packets is starting")
        var packetCount = 0
        val start = System.nanoTime()
        val fd = vpnInterface?.fileDescriptor ?: return
        val input = FileInputStream(fd)
        val output = FileOutputStream(fd)
        val buffer = ByteArray(4096)
        var lastReadEnd = start

        try {
            while (currentCoroutineContext().isActive) {
                val readStart = System.nanoTime()
                val delta = (readStart - lastReadEnd) / 1e6
                Log.d(tag, "Δt since last read = ${"%.3f".format(delta)}ms")

                val length = input.read(buffer)
                if (length <= 0) continue
                val afterRead = System.nanoTime()
                lastReadEnd = afterRead
                val totalSinceStart = (afterRead - start) / 1e6
                val readMs = (afterRead - readStart) / 1e6



                packetCount++

                val waitTime = (readStart - lastReadEnd) / 1e6
                val readTime = (afterRead - readStart) / 1e6
                Log.d(tag, "Δt since last packet = %.3f ms, read blocked for %.3f ms, bytes=$length".format(waitTime, readTime))


                Log.d(tag, "Got packet #$packetCount at ${"%.3f".format(totalSinceStart)}ms read=${"%.3f".format(readMs)}ms")

                val ipVersion = (buffer[0].toInt() shr 4) and 0x0F
                if (ipVersion != 4) continue

                val srcIp = "${buffer[12].toInt() and 0xFF}.${buffer[13].toInt() and 0xFF}.${buffer[14].toInt() and 0xFF}.${buffer[15].toInt() and 0xFF}"
                val dstIp = "${buffer[16].toInt() and 0xFF}.${buffer[17].toInt() and 0xFF}.${buffer[18].toInt() and 0xFF}.${buffer[19].toInt() and 0xFF}"
                Log.i(tag, "Connection: $srcIp → $dstIp")
                val activeThreat = ThreatsList.threatMap[dstIp]

                if (activeThreat != null && !ThreatsList.activeThreats.any { it.title == activeThreat.title }) {
                    badIP= dstIp
                    ThreatsList.activeThreats.add(activeThreat)
                    Log.i(tag, "Added ${activeThreat.title} to ThreatList")
                    continue
                }
                val packetCopy = buffer.copyOf(length)
                //packetChannel.trySend(packetCopy)
                output.write(packetCopy, 0,length)

               // writePackets(packetCopy, dstIp)
            }
        } catch ( e: Exception) {
            Log.e(tag, "error reading packet", e)
        }

        }

    suspend fun writePackets(packet: ByteArray, dstIp: String){
        val fd = vpnInterface?.fileDescriptor ?: return
        val dstPort = ((packet[22].toInt() and 0xFF) shl 8) or (packet[23].toInt() and 0xFF)
        //val output = FileOutputStream(fd)
        try {
            for (packet in packetChannel) {
                val writeStart = System.nanoTime()
                val socket = Socket()
                protect(socket) // prevent loop
                Log.i(tag, "fowarding to packet $dstIp")
                socket.connect(java.net.InetSocketAddress(dstIp, dstPort), 3000)

                val payloadOffset = 20 + 20 // skip IP + TCP headers (naïve)
                if (packet.size > payloadOffset) {
                    socket.getOutputStream().write(packet, payloadOffset, packet.size - payloadOffset)
                }

                val response = socket.getInputStream().readBytes()
                // Here you’d need to wrap the response in a proper IP+TCP header to send back.
                // For testing, you could just log it:
                Log.d(tag, "Got response from $dstIp:$dstPort ${response.size} bytes")

                socket.close()
                val writeEnd = System.nanoTime()
                Log.d(tag, "write=${(writeEnd - writeStart) / 1e6}ms")
            }
        } catch (e: Exception) {
            Log.e(tag, "Error in writePackets", e)
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


}
