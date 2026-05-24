package com.example.models

import android.graphics.drawable.Drawable

/**
 * Model representing an application installed on the device.
 */
data class AppInfo(
    val appName: String,
    val packageName: String,
    val isBlocked: Boolean,
    val isSystemApp: Boolean,
    // Transient field for icon, loaded dynamically and not serialized
    @Transient var icon: Drawable? = null
)
