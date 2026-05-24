package com.example.repository

import android.content.Context
import com.example.database.LogDao
import com.example.models.FirewallLog
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class LogRepository(private val logDao: LogDao) {

    val allLogs: Flow<List<FirewallLog>> = logDao.getAllLogs()

    suspend fun insertLog(log: FirewallLog) {
        logDao.insertLog(log)
    }

    suspend fun clearLogs() {
        logDao.clearLogs()
    }

    /**
     * Exports logs to a formatted text string.
     */
    fun exportLogsToString(logs: List<FirewallLog>): String {
        if (logs.isEmpty()) return "No firewall logs found."

        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("  WAB FIREWALL BLOCKED CONNECTION LOGS   \n")
        sb.append("=========================================\n\n")

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
        for (log in logs) {
            val dateStr = sdf.format(log.timestamp)
            sb.append("[$dateStr] BLOCKED:\n")
            sb.append("  App: ${log.appName} (${log.packageName})\n")
            sb.append("  Dest IP: ${log.destinationAddress}\n")
            sb.append("  Protocol: ${log.protocol} | Port: ${log.port}\n")
            sb.append("-----------------------------------------\n")
        }
        return sb.toString()
    }
}
