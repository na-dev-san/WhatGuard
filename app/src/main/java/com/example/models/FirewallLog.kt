package com.example.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room Entity representing a log of a blocked connection attempt.
 */
@Entity(tableName = "firewall_logs")
data class FirewallLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val appName: String,
    val destinationAddress: String,
    val protocol: String,
    val port: Int,
    val isBlocked: Boolean = true
)
