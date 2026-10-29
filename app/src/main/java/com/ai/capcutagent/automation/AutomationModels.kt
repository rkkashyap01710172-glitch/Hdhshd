package com.ai.capcutagent.automation

import java.util.UUID

enum class ActionType {
    CLICK_TEXT,
    CLICK_COORDINATES,
    SWIPE_TIMELINE,
    WAIT,
    LAUNCH_CAPCUT,
    PINCH_ZOOM,
    PRESS_BACK
}

data class AutomationAction(
    val type: ActionType,
    val targetText: String = "",
    val xRatio: Float = 0.5f,
    val yRatio: Float = 0.5f,
    val startXRatio: Float = 0.7f,
    val startYRatio: Float = 0.65f,
    val endXRatio: Float = 0.3f,
    val endYRatio: Float = 0.65f,
    val durationMs: Long = 350L,
    val timeoutMs: Long = 4000L
)

data class AutomationStep(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val action: AutomationAction,
    val delayAfterMs: Long = 700L,
    val isEnabled: Boolean = true,
    val isOptional: Boolean = false
)

data class Workflow(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: String,
    val iconKey: String = "scissors",
    val steps: List<AutomationStep>,
    val isPreset: Boolean = false
)

sealed class ExecutionState {
    object Idle : ExecutionState()
    data class Running(val stepIndex: Int, val totalSteps: Int, val stepTitle: String) : ExecutionState()
    data class Paused(val stepIndex: Int, val totalSteps: Int, val stepTitle: String) : ExecutionState()
    data class Completed(val totalStepsExecuted: Int, val durationMs: Long) : ExecutionState()
    data class Failed(val stepIndex: Int, val stepTitle: String, val reason: String) : ExecutionState()
}

enum class LogLevel {
    INFO,
    ACTION,
    SUCCESS,
    WARNING,
    ERROR
}

data class AutomationLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class StepExecutionResult(
    val isSuccess: Boolean,
    val message: String,
    val matchedNodeText: String? = null
)
