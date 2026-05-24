package com.example.viewmodels

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.LogDatabase
import com.example.models.AppInfo
import com.example.models.FirewallLog
import com.example.repository.AppRepository
import com.example.repository.FirewallSettingsStore
import com.example.repository.LogRepository
import com.example.services.FirewallVpnService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FirewallViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val settingsStore = FirewallSettingsStore(context)
    private val logDatabase = LogDatabase.getDatabase(context)
    private val logRepository = LogRepository(logDatabase.logDao())
    private val appRepository = AppRepository(context)

    // UI States
    val isFirewallRunning: StateFlow<Boolean> = FirewallVpnService.isServiceRunning

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _isAppsLoading = MutableStateFlow(false)
    val isAppsLoading: StateFlow<Boolean> = _isAppsLoading.asStateFlow()

    val blockedLogs: StateFlow<List<FirewallLog>> = logRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isW4bInstalled = MutableStateFlow(false)
    val isW4bInstalled: StateFlow<Boolean> = _isW4bInstalled.asStateFlow()

    private val _isW4bBlocked = MutableStateFlow(false)
    val isW4bBlocked: StateFlow<Boolean> = _isW4bBlocked.asStateFlow()

    // Settings States (exposed directly)
    private val _startOnBoot = MutableStateFlow(settingsStore.isStartOnBootEnabled())
    val startOnBoot: StateFlow<Boolean> = _startOnBoot.asStateFlow()

    private val _blockIpv6 = MutableStateFlow(settingsStore.isBlockIpv6Enabled())
    val blockIpv6: StateFlow<Boolean> = _blockIpv6.asStateFlow()

    private val _blockUdp = MutableStateFlow(settingsStore.isBlockUdpEnabled())
    val blockUdp: StateFlow<Boolean> = _blockUdp.asStateFlow()

    private val _persistentNotification = MutableStateFlow(settingsStore.isPersistentNotificationEnabled())
    val persistentNotification: StateFlow<Boolean> = _persistentNotification.asStateFlow()

    private val _loggingEnabled = MutableStateFlow(settingsStore.isLoggingEnabled())
    val loggingEnabled: StateFlow<Boolean> = _loggingEnabled.asStateFlow()

    init {
        checkWhatsAppBusinessStatus()
        loadInstalledApps()
        
        // Listen to the Service's active state of blocked packages
        viewModelScope.launch {
            FirewallVpnService.blockedAppsState.collect {
                checkWhatsAppBusinessStatus()
            }
        }
    }

    fun checkWhatsAppBusinessStatus() {
        _isW4bInstalled.value = appRepository.isWhatsAppBusinessInstalled()
        _isW4bBlocked.value = settingsStore.isAppBlocked(FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadInstalledApps()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isAppsLoading.value = true
            try {
                _installedApps.value = appRepository.getInstalledApps(_searchQuery.value)
            } catch (e: Exception) {
                Log.e("FirewallViewModel", "Error loading installed apps", e)
            } finally {
                _isAppsLoading.value = false
            }
        }
    }

    /**
     * Retrieves the icon for an app smoothly on-demand inside the compose UI.
     */
    fun getAppIcon(packageName: String): Drawable? {
        return appRepository.getAppIcon(packageName)
    }

    /**
     * Action to switch firewall on/off.
     */
    fun toggleFirewall() {
        if (isFirewallRunning.value) {
            triggerServiceAction(FirewallVpnService.ACTION_STOP)
        } else {
            triggerServiceAction(FirewallVpnService.ACTION_START)
        }
    }

    /**
     * Toggles block status for a specific app package.
     */
    fun toggleAppBlockedStatus(packageName: String) {
        settingsStore.toggleAppBlock(packageName)
        checkWhatsAppBusinessStatus()
        loadInstalledApps()

        // If service is currently active, re-trigger start to dynamically rebuild TUN and block filters instantly
        if (isFirewallRunning.value) {
            triggerServiceAction(FirewallVpnService.ACTION_START)
        }
    }

    private fun triggerServiceAction(actionString: String) {
        val intent = Intent(context, FirewallVpnService::class.java).apply {
            action = actionString
        }
        try {
            if (actionString == FirewallVpnService.ACTION_STOP) {
                // To stop, startService is completely safe since the app is currently in the foreground.
                // This prevents Android from throwing a ForegroundServiceDidNotStartInTimeException.
                context.startService(intent)
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        } catch (e: Exception) {
            Log.e("FirewallViewModel", "Failed to transition Firewall Service state", e)
        }
    }

    // Settings actions
    fun setStartOnBoot(enabled: Boolean) {
        settingsStore.setStartOnBootEnabled(enabled)
        _startOnBoot.value = enabled
    }

    fun setBlockIpv6(enabled: Boolean) {
        settingsStore.setBlockIpv6Enabled(enabled)
        _blockIpv6.value = enabled
        if (isFirewallRunning.value) triggerServiceAction(FirewallVpnService.ACTION_START)
    }

    fun setBlockUdp(enabled: Boolean) {
        settingsStore.setBlockUdpEnabled(enabled)
        _blockUdp.value = enabled
    }

    fun setPersistentNotification(enabled: Boolean) {
        settingsStore.setPersistentNotificationEnabled(enabled)
        _persistentNotification.value = enabled
    }

    fun setLoggingEnabled(enabled: Boolean) {
        settingsStore.setLoggingEnabled(enabled)
        _loggingEnabled.value = enabled
    }

    fun clearBlockedLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
        }
    }

    fun getExportedLogsText(): String {
        return logRepository.exportLogsToString(blockedLogs.value)
    }
}
