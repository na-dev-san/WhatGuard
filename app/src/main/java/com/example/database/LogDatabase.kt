package com.example.database

import android.content.Context
import androidx.room.*
import com.example.models.FirewallLog
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Query("SELECT * FROM firewall_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllLogs(): Flow<List<FirewallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: FirewallLog)

    @Query("DELETE FROM firewall_logs")
    suspend fun clearLogs()
}

@Database(entities = [FirewallLog::class], version = 1, exportSchema = false)
abstract class LogDatabase : RoomDatabase() {
    abstract fun logDao(): LogDao

    companion object {
        @Volatile
        private var INSTANCE: LogDatabase? = null

        fun getDatabase(context: Context): LogDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LogDatabase::class.java,
                    "firewall_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
