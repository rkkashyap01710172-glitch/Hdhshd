package com.ai.capcutagent.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AutomationDao {
    @Insert
    suspend fun insertLog(log: AutomationLog)

    @Query("SELECT * FROM automation_logs ORDER BY timestamp DESC LIMIT 20")
    suspend fun getRecentLogs(): List<AutomationLog>

    @Query("SELECT COUNT(*) FROM automation_logs WHERE actionName = :action AND isSuccess = 1")
    suspend fun getSuccessCount(action: String): Int

    @Query("DELETE FROM automation_logs")
    suspend fun clearLogs()
}
