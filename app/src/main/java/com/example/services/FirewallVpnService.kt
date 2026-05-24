package com.example.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.database.LogDatabase
import com.example.models.FirewallLog
import com.example.repository.AppRepository
import com.example.repository.FirewallSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class FirewallVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Thread? = null
    private var isRunning = false

    private lateinit var settingsStore: FirewallSettingsStore
    private lateinit var logDatabase: LogDatabase
    private lateinit var appRepository: AppRepository

    companion object {
        private const val TAG = "FirewallVpnService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "firewall_vpn_channel"

        const val ACTION_START = "com.example.action.START"
        const val ACTION_STOP = "com.example.action.STOP"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        // Exposing the current list of blocked applications for immediate UI access
        private val _blockedAppsState = MutableStateFlow<Set<String>>(emptySet())
        val blockedAppsState: StateFlow<Set<String>> = _blockedAppsState.asStateFlow()
    }

    override fun onCreate() {
        super.onCreate()
        settingsStore = FirewallSettingsStore(this)
        logDatabase = LogDatabase.getDatabase(this)
        appRepository = AppRepository(this)
        _blockedAppsState.value = settingsStore.getBlockedPackages()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        Log.d(TAG, "onStartCommand with action: $action")

        if (action == ACTION_STOP) {
            stopFirewall()
            return START_NOT_STICKY
        }

        if (action == ACTION_START || isRunning) {
            startFirewall()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        stopFirewall()
        super.onDestroy()
    }

    private fun startFirewall() {
        // ALWAYS show notification / startForeground first to satisfy Android startForegroundService constraints
        showNotification()

        if (isRunning) {
            Log.d(TAG, "Firewall is already running. Dynamically rebuilding VPN interface...")
            try {
                establishVpn()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rebuild VPN interface dynamically", e)
            }
            return
        }

        Log.d(TAG, "Starting VPN Firewall Service...")
        isRunning = true
        _isServiceRunning.value = true
        settingsStore.setFirewallActive(true)

        // Initialize and establish the VPN tunnel
        try {
            establishVpn()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN interface", e)
            stopFirewall()
            return
        }

        // Dedicated thread for reading and blackholing packets
        vpnThread = Thread({ runVpnLoop() }, "WABFirewallThread").apply {
            start()
        }
    }

    private fun stopFirewall() {
        if (!isRunning) return

        Log.d(TAG, "Stopping VPN Firewall Service...")
        isRunning = false
        _isServiceRunning.value = false
        settingsStore.setFirewallActive(false)

        try {
            vpnThread?.interrupt()
            vpnThread = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping vpn thread", e)
        }

        try {
            vpnInterface?.close()
            vpnInterface = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing VPN interface", e)
        }

        stopForeground(true)
        stopSelf()
    }

    private fun showNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Firewall Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of the WhatsApp Business Firewall"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("WhatsApp Business Firewall is ON")
            .setContentText("Local VPN is active. Blocked apps cannot access the internet.")
            .setSmallIcon(R.drawable.ic_shield_lock)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun establishVpn() {
        val builder = Builder()
        builder.setSession("WhatsApp Business Firewall")
        builder.setMtu(1500)

        // Assign mock IP for routing. 
        // This is standard practice in no-root local VPN tunnels.
        builder.addAddress("10.8.0.1", 24)
        builder.addRoute("0.0.0.0", 0)

        if (settingsStore.isBlockIpv6Enabled()) {
            builder.addAddress("fd00::1", 64)
            builder.addRoute("::", 0)
        }

        // Retrieve current list of apps to block
        val blockedApps = settingsStore.getBlockedPackages()
        var addedAny = false

        for (packageName in blockedApps) {
            try {
                // Route all traffic from this application exclusively into the local VPN tunnel
                builder.addAllowedApplication(packageName)
                addedAny = true
                Log.d(TAG, "Added firewall block filter for application: $packageName")
            } catch (e: PackageManager.NameNotFoundException) {
                // If package isn't installed, swallow exception and proceed
                Log.w(TAG, "Package mapped for blocking is not installed: $packageName")
            }
        }

        // If no apps are selected for blocking, we add our own package name 
        // as a placeholder so VpnService maintains its lifecycle without blocking others.
        if (!addedAny) {
            builder.addAllowedApplication(packageName)
        }

        // Build the interface. This will prompts standard Android VPN dialog if first-run.
        val oldInterface = vpnInterface
        vpnInterface = builder.establish()
        Log.d(TAG, "VPN established with FD: ${vpnInterface?.fd}")
        try {
            oldInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing outdated VPN interface", e)
        }
    }

    private fun runVpnLoop() {
        val pfd = vpnInterface ?: return
        val inputStream = FileInputStream(pfd.fileDescriptor)
        val outputStream = FileOutputStream(pfd.fileDescriptor)
        val buffer = ByteArray(32768)

        try {
            while (isRunning && !Thread.currentThread().isInterrupted) {
                val length = inputStream.read(buffer)
                if (length > 0) {
                    // Packet received from blocked app!
                    // Read the IP/Port info for privacy logs, then drop/blackhole it (do NOT write back)
                    if (settingsStore.isLoggingEnabled()) {
                        parseAndLogPacket(buffer, length)
                    }
                } else if (length < 0) {
                    break
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "IOException reading from VPN interface descriptor", e)
        } catch (e: InterruptedException) {
            Log.d(TAG, "VPN Loop interrupted.")
        } finally {
            try {
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing streams", e)
            }
        }
    }

    private fun parseAndLogPacket(packet: ByteArray, length: Int) {
        if (length < 20) return // IP header too small

        val version = (packet[0].toInt() ushr 4) and 0x0F
        var protocolStr = "IP"
        var destIp = ""
        var destPort = 0

        try {
            if (version == 4) {
                // IPv4 Parsing
                val ihl = (packet[0].toInt() and 0x0F) * 4
                if (length < ihl) return

                val protocol = packet[9].toInt() and 0xFF
                protocolStr = when (protocol) {
                    6 -> "TCP"
                    17 -> "UDP"
                    1 -> "ICMP"
                    else -> "IP-$protocol"
                }

                if (protocolStr == "UDP" && !settingsStore.isBlockUdpEnabled()) {
                    return // If user deactivated UDP blocking, ignore logs/blocking for UDP where possible (we still blackhole if routed, but here we ignore logging)
                }

                // Des IP at index 16-19
                val d1 = packet[16].toInt() and 0xFF
                val d2 = packet[17].toInt() and 0xFF
                val d3 = packet[18].toInt() and 0xFF
                val d4 = packet[19].toInt() and 0xFF
                destIp = "$d1.$d2.$d3.$d4"

                // Destination Port
                if ((protocol == 6 || protocol == 17) && length >= ihl + 4) {
                    destPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
                }

            } else if (version == 6 && length >= 40) {
                // IPv6 Parsing
                val nextHeader = packet[6].toInt() and 0xFF
                protocolStr = when (nextHeader) {
                    6 -> "TCP"
                    17 -> "UDP"
                    58 -> "ICMPv6"
                    else -> "IPv6-$nextHeader"
                }

                if (protocolStr == "UDP" && !settingsStore.isBlockUdpEnabled()) {
                    return
                }

                // Destination IP at bytes 24-39
                val ipParts = mutableListOf<String>()
                for (i in 0 until 8) {
                    val idx = 24 + i * 2
                    val b1 = packet[idx].toInt() and 0xFF
                    val b2 = packet[idx + 1].toInt() and 0xFF
                    val v = (b1 shl 8) or b2
                    ipParts.add(v.toString(16))
                }
                destIp = ipParts.joinToString(":")

                // TCP/UDP Port
                if ((nextHeader == 6 || nextHeader == 17) && length >= 44) {
                    destPort = ((packet[42].toInt() and 0xFF) shl 8) or (packet[43].toInt() and 0xFF)
                }
            } else {
                return
            }

            // Map IP structure into logged items.
            // Since all packets hitting our local VPN are originating exclusively from the blocked applications catalog,
            // we associate the log of this connection attempt to the targets currently configured.
            val blockedApps = settingsStore.getBlockedPackages()
            blockedApps.forEach { blockedPkg ->
                var appName = "WhatsApp Business"
                if (blockedPkg != FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE) {
                    appName = blockedPkg.substringAfterLast('.')
                }

                val logEntry = FirewallLog(
                    packageName = blockedPkg,
                    appName = appName,
                    destinationAddress = if (destPort > 0) "$destIp:$destPort" else destIp,
                    protocol = protocolStr,
                    port = destPort
                )

                // Write asynchronously to Room database, bypassing block on the VPN buffer read
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        logDatabase.logDao().insertLog(logEntry)
                        Log.v(TAG, "Logged blocked packet: $protocolStr connection to $destIp:$destPort")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error saving log entry", e)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Exception during packet parsing", e)
        }
    }
}
