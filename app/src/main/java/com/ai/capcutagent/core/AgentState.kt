package com.ai.capcutagent.core

enum class AgentState {
    IDLE,
    INITIALIZING,
    WAITING_FOR_USER,
    ANALYZING_MEDIA,
    BUILDING_EDIT_PLAN,
    APPLYING_EDITS,
    VERIFYING_EDITS,
    RECOVERING,
    READY_FOR_EXPORT,
    EXPORTING,
    COMPLETED,
    STOPPED,
    FAILED;

    val isActive: Boolean
        get() = this in listOf(
            INITIALIZING,
            ANALYZING_MEDIA,
            BUILDING_EDIT_PLAN,
            APPLYING_EDITS,
            VERIFYING_EDITS,
            RECOVERING,
            EXPORTING
        )
}

fun AgentState.getDisplayName(): String = when (this) {
    AgentState.IDLE -> "Idle"
    AgentState.INITIALIZING -> "Initializing"
    AgentState.WAITING_FOR_USER -> "Waiting for User"
    AgentState.ANALYZING_MEDIA -> "Analyzing Media"
    AgentState.BUILDING_EDIT_PLAN -> "Building Edit Plan"
    AgentState.APPLYING_EDITS -> "Applying Edits"
    AgentState.VERIFYING_EDITS -> "Verifying Edits"
    AgentState.RECOVERING -> "Self-Healing Recovery"
    AgentState.READY_FOR_EXPORT -> "Ready for Export"
    AgentState.EXPORTING -> "Exporting Video"
    AgentState.COMPLETED -> "Completed"
    AgentState.STOPPED -> "Stopped"
    AgentState.FAILED -> "Failed"
}

fun AgentState.getDescription(): String = when (this) {
    AgentState.IDLE -> "Agent standing by for editing tasks"
    AgentState.INITIALIZING -> "Checking accessibility service and CapCut status"
    AgentState.WAITING_FOR_USER -> "Awaiting user confirmation or project selection"
    AgentState.ANALYZING_MEDIA -> "Scanning timeline layers, clips, and screen nodes"
    AgentState.BUILDING_EDIT_PLAN -> "Synthesizing editing macro and action sequence"
    AgentState.APPLYING_EDITS -> "Dispatching timeline gestures and tool clicks"
    AgentState.VERIFYING_EDITS -> "Verifying expected UI updates in active window"
    AgentState.RECOVERING -> "Attempting fallback gestures and popup dismissals"
    AgentState.READY_FOR_EXPORT -> "All edits verified, prepared for rendering"
    AgentState.EXPORTING -> "Setting resolution & frame rate, rendering video"
    AgentState.COMPLETED -> "Video editing session completed successfully"
    AgentState.STOPPED -> "Execution halted by user"
    AgentState.FAILED -> "Encountered unrecoverable error during execution"
}

