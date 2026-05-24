package com.example.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.models.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(private val context: Context) {

    private val packageManager: PackageManager = context.packageManager
    private val settingsStore = FirewallSettingsStore(context)

    /**
     * Checks if WhatsApp Business is installed.
     */
    fun isWhatsAppBusinessInstalled(): Boolean {
        return isPackageInstalled(FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE)
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Loads list of all apps installed on the device.
     * Orders WhatsApp Business to the top, followed by user apps, then system apps.
     */
    suspend fun getInstalledApps(searchQuery: String = ""): List<AppInfo> = withContext(Dispatchers.IO) {
        val result = mutableListOf<AppInfo>()
        val installedApps = try {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        } catch (e: Exception) {
            emptyList()
        }

        val blockedPackages = settingsStore.getBlockedPackages()

        // Ensure WhatsApp Business is represented even if QUERY_ALL_PACKAGES is restricted
        var isW4bFound = false

        for (app in installedApps) {
            // Filter out system apps if desired or keep them but distinct, skip our own app
            if (app.packageName == context.packageName) continue

            val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            val appLabel = app.loadLabel(packageManager).toString()
            val pkgName = app.packageName

            if (pkgName == FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE) {
                isW4bFound = true
            }

            if (searchQuery.isNotEmpty() &&
                !appLabel.contains(searchQuery, ignoreCase = true) &&
                !pkgName.contains(searchQuery, ignoreCase = true)
            ) {
                continue
            }

            val appInfo = AppInfo(
                appName = appLabel,
                packageName = pkgName,
                isBlocked = blockedPackages.contains(pkgName),
                isSystemApp = isSystem
            )
            result.add(appInfo)
        }

        // Fallback for WhatsApp Business if query visibility limits it but it is actually installed
        if (!isW4bFound && isWhatsAppBusinessInstalled()) {
            val label = try {
                val appInfo = packageManager.getApplicationInfo(FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE, 0)
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                "WhatsApp Business"
            }
            
            if (searchQuery.isEmpty() || 
                label.contains(searchQuery, ignoreCase = true) || 
                FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE.contains(searchQuery, ignoreCase = true)
            ) {
                result.add(
                    AppInfo(
                        appName = label,
                        packageName = FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE,
                        isBlocked = blockedPackages.contains(FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE),
                        isSystemApp = false
                    )
                )
            }
        }

        // Sort: Group blocked apps first, then WhatsApp Business, then user apps alphabetically
        result.sortedWith(compareByDescending<AppInfo> { it.packageName == FirewallSettingsStore.WHATSAPP_BUSINESS_PACKAGE }
            .thenByDescending { it.isBlocked }
            .thenBy { it.isSystemApp }
            .thenBy { it.appName.lowercase() }
        )
    }

    /**
     * Loads the launcher icon for a package. Solves loading performance.
     */
    fun getAppIcon(packageName: String) = try {
        packageManager.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }
}
