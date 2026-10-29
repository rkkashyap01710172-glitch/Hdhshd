package com.ai.capcutagent.ui

import com.ai.capcutagent.core.AgentState

enum class OperatingMode {
    ONLINE, OFFLINE, AUTO
}

data class AgentUiState(
    val agentState: AgentState = AgentState.IDLE,
    val mode: OperatingMode = OperatingMode.AUTO,
    val isAccessibilityEnabled: Boolean = false,
    val statusMessage: String = "तैयार हैं सर, एडिटिंग शुरू करें।"
)
