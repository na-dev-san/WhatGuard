package com.example.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * Manages local preferences and selected blocked package names.
 * Uses lightweight and ultra-fast SharedPreferences.
 */
class FirewallSettingsStore(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "firewall_settings"
        private const val KEY_BLOCKED_PACKAGES = "blocked_packages"
        private const val KEY_START_ON_BOOT = "start_on_boot"
        private const val KEY_BLOCK_IPV6 = "block_ipv6"
        private const val KEY_BLOCK_UDP = "block_udp"
        private const val KEY_PERSISTENT_NOTIFICATION = "persistent_notification"
        private const val KEY_LOGGING_ENABLED = "logging_enabled"
        private const val KEY_FIREWALL_ACTIVE = "firewall_active"
        
        // Target app defaults
        const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    }

    /**
     * Set of package names currently configured for blocking.
     * Starts with WhatsApp Business pre-selected.
     */
    fun getBlockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_BLOCKED_PACKAGES, setOf(WHATSAPP_BUSINESS_PACKAGE)) ?: setOf(WHATSAPP_BUSINESS_PACKAGE)
    }

    fun setBlockedPackages(packages: Set<String>) {
        prefs.edit().putStringSet(KEY_BLOCKED_PACKAGES, packages).apply()
        Log.d("FirewallSettingsStore", "Blocked apps updated to: $packages")
    }

    fun isAppBlocked(packageName: String): Boolean {
        return getBlockedPackages().contains(packageName)
    }

    fun toggleAppBlock(packageName: String) {
        val current = getBlockedPackages().toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        setBlockedPackages(current)
    }

    fun isStartOnBootEnabled(): Boolean {
        return prefs.getBoolean(KEY_START_ON_BOOT, false)
    }

    fun setStartOnBootEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_START_ON_BOOT, enabled).apply()
    }

    fun isBlockIpv6Enabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCK_IPV6, true)
    }

    fun setBlockIpv6Enabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_IPV6, enabled).apply()
    }

    fun isBlockUdpEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCK_UDP, true)
    }

    fun setBlockUdpEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_UDP, enabled).apply()
    }

    fun isPersistentNotificationEnabled(): Boolean {
        return prefs.getBoolean(KEY_PERSISTENT_NOTIFICATION, true)
    }

    fun setPersistentNotificationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PERSISTENT_NOTIFICATION, enabled).apply()
    }

    fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(KEY_LOGGING_ENABLED, true)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_LOGGING_ENABLED, enabled).apply()
    }

    fun isFirewallActive(): Boolean {
        return prefs.getBoolean(KEY_FIREWALL_ACTIVE, false)
    }

    fun setFirewallActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_FIREWALL_ACTIVE, active).apply()
    }
}
