package com.ai.capcutagent.data

class AutomationLogRepository(private val dao: AutomationDao) {

    suspend fun insertLog(log: AutomationLog) = dao.insertLog(log)

    suspend fun getRecentLogs(): List<AutomationLog> = dao.getRecentLogs()

    suspend fun getSuccessCount(action: String): Int = dao.getSuccessCount(action)

    suspend fun clearLogs() = dao.clearLogs()
}
