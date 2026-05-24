package com.example.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.repository.FirewallSettingsStore
import com.example.services.FirewallVpnService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device boot completed, checking firewall settings...")
            
            val settingsStore = FirewallSettingsStore(context)
            if (settingsStore.isStartOnBootEnabled()) {
                Log.d("BootReceiver", "StartOnBoot is enabled, initiating Firewall VPN Service...")
                
                val serviceIntent = Intent(context, FirewallVpnService::class.java).apply {
                    action = FirewallVpnService.ACTION_START
                }
                
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Failed to start FirewallVpnService on boot", e)
                }
            } else {
                Log.d("BootReceiver", "StartOnBoot is disabled. Firewall remains idle.")
            }
        }
    }
}
