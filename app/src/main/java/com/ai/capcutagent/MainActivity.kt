package com.ai.capcutagent

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.lifecycleScope
import com.ai.capcutagent.automation.AutomationManager
import com.ai.capcutagent.automation.CapCutActionController
import com.ai.capcutagent.automation.CapCutAutomationService
import com.ai.capcutagent.core.AIOrchestrator
import com.ai.capcutagent.core.AgentState
import com.ai.capcutagent.ui.*
import com.ai.capcutagent.voice.HindiVoiceAgent

class MainActivity : ComponentActivity() {

    private lateinit var voiceAgent: HindiVoiceAgent
    private var orchestrator: AIOrchestrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize shared automation engine & managers
        AutomationManager.initialize(this)

        val uiState = mutableStateOf(AgentUiState())

        voiceAgent = HindiVoiceAgent(this) { spokenCommand ->
            orchestrator?.handleVoiceCommand(spokenCommand)
        }

        setContent {
            val state by remember { uiState }

            AgentScreen(
                uiState = state,
                onStartStopClick = {
                    val service = CapCutAutomationService.instance
                    if (service == null) {
                        // यदि एक्सेसिबिलिटी चालू नहीं है तो सेटिंग्स खोलें
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        return@AgentScreen
                    }

                    if (orchestrator == null) {
                        val controller = CapCutActionController(service)
                        orchestrator = AIOrchestrator(controller, voiceAgent, lifecycleScope)
                    }

                    if (state.agentState == AgentState.IDLE || state.agentState == AgentState.STOPPED) {
                        orchestrator?.startSession()
                        uiState.value = state.copy(
                            agentState = AgentState.INITIALIZING,
                            statusMessage = "सत्र शुरू हो गया है। CapCut खोलें।"
                        )
                    } else {
                        orchestrator?.stopSession()
                        uiState.value = state.copy(
                            agentState = AgentState.STOPPED,
                            statusMessage = "एडिटिंग सुरक्षित रूप से रोक दी गई है।"
                        )
                    }
                },
                onModeChange = { newMode ->
                    uiState.value = state.copy(mode = newMode)
                },
                onTrainClick = {
                    uiState.value = state.copy(statusMessage = "ट्रेनिंग मोड जल्द शुरू होगा...")
                },
                onOpenSettings = {
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceAgent.shutdown()
    }
}
