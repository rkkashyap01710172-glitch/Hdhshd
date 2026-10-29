package com.ai.capcutagent.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ai.capcutagent.automation.AutomationManager
import com.ai.capcutagent.automation.CapCutAutomationService
import com.ai.capcutagent.core.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AgentViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AgentUiState())
    val uiState: StateFlow<AgentUiState> = _uiState.asStateFlow()

    init {
        // Observe accessibility connection
        viewModelScope.launch {
            CapCutAutomationService.isServiceConnected.collect { connected ->
                _uiState.value = _uiState.value.copy(
                    isAccessibilityEnabled = connected
                )
            }
        }

        // Observe orchestrator state if available
        viewModelScope.launch {
            AutomationManager.aiOrchestrator?.state?.collect { state ->
                val message = getHindiStatusForState(state)
                _uiState.value = _uiState.value.copy(
                    agentState = state,
                    statusMessage = message
                )
            }
        }
    }

    fun setMode(mode: OperatingMode) {
        _uiState.value = _uiState.value.copy(mode = mode)
    }

    fun setStatusMessage(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
    }

    private fun getHindiStatusForState(state: AgentState): String = when (state) {
        AgentState.IDLE -> "तैयार हैं सर, एडिटिंग शुरू करें।"
        AgentState.INITIALIZING -> "सिस्टम तैयार हो रहा है..."
        AgentState.WAITING_FOR_USER -> "बोलिए सर, क्या एडिट करना है?"
        AgentState.ANALYZING_MEDIA -> "टाइमलाइन क्लिप्स और लेयर्स स्कैन हो रहे हैं..."
        AgentState.BUILDING_EDIT_PLAN -> "एडिटिंग प्लान तैयार किया जा रहा है..."
        AgentState.APPLYING_EDITS -> "एडिटिंग जेस्चर निष्पादित हो रहे हैं..."
        AgentState.VERIFYING_EDITS -> "स्क्रीन और कट जांची जा रही हैं..."
        AgentState.RECOVERING -> "सेल्फ-हीलिंग रिकवरी प्रयास जारी है..."
        AgentState.READY_FOR_EXPORT -> "वीडियो एक्सपोर्ट के लिए तैयार है।"
        AgentState.EXPORTING -> "वीडियो रेंडरिंग प्रक्रिया शुरू हो गई है..."
        AgentState.COMPLETED -> "एडिटिंग सफलतापूर्वक पूरी हुई!"
        AgentState.STOPPED -> "एडिटिंग रोक दी गई है।"
        AgentState.FAILED -> "एडिटिंग में रुकावट आई।"
    }
}
