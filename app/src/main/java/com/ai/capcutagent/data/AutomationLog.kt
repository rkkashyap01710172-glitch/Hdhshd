package com.ai.capcutagent.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_logs")
data class AutomationLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionName: String,          // e.g., "SPLIT_CLIP", "SPEED_CHANGE"
    val timestamp: Long = System.currentTimeMillis(),
    val isSuccess: Boolean,
    val targetNodeText: String?,
    val failureReason: String? = null
)
