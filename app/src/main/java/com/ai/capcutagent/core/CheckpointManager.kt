package com.ai.capcutagent.core

import com.ai.capcutagent.data.AutomationDao
import com.ai.capcutagent.data.AutomationLog

class CheckpointManager(private val dao: AutomationDao) {

    suspend fun recordAction(action: String, target: String?, success: Boolean, error: String? = null) {
        val log = AutomationLog(
            actionName = action,
            isSuccess = success,
            targetNodeText = target,
            failureReason = error
        )
        dao.insertLog(log)
    }

    suspend fun getLastVerifiedStep(): String? {
        val logs = dao.getRecentLogs()
        return logs.firstOrNull { it.isSuccess }?.actionName
    }
}
