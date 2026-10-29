package com.ai.capcutagent.core

import com.ai.capcutagent.automation.CapCutActionController
import com.ai.capcutagent.voice.HindiVoiceAgent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AIOrchestrator(
    private val actionController: CapCutActionController,
    private val voiceAgent: HindiVoiceAgent,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(AgentState.IDLE)
    val state: StateFlow<AgentState> = _state

    fun startSession() {
        _state.value = AgentState.INITIALIZING
        voiceAgent.speak("नमस्ते सर! चलिए एडिटिंग शुरू करते हैं। मैं प्रोजेक्ट समझकर प्लान बना रहा हूँ।")
        _state.value = AgentState.WAITING_FOR_USER
        voiceAgent.startListening()
    }

    fun handleVoiceCommand(command: String) {
        scope.launch(Dispatchers.Default) {
            when {
                command.contains("काटो") || command.contains("कट") || command.contains("split", ignoreCase = true) -> {
                    _state.value = AgentState.APPLYING_EDITS
                    voiceAgent.speak("कट लगा रहा हूँ।")
                    val success = actionController.executeSplit()
                    _state.value = if (success) AgentState.VERIFYING_EDITS else AgentState.RECOVERING
                    voiceAgent.speak(if (success) "कट पूरा हुआ।" else "कट नहीं लग सका, दोबारा प्रयास कर रहा हूँ।")
                }

                command.contains("स्पीड") || command.contains("फास्ट") || command.contains("स्लो") ||
                command.contains("speed", ignoreCase = true) -> {
                    _state.value = AgentState.APPLYING_EDITS
                    voiceAgent.speak("स्पीड मेन्यू खोल रहा हूँ।")
                    val success = actionController.openSpeedMenu()
                    _state.value = if (success) AgentState.VERIFYING_EDITS else AgentState.RECOVERING
                }

                command.contains("कैप्शन") || command.contains("सबटाइटल") || command.contains("caption", ignoreCase = true) -> {
                    _state.value = AgentState.APPLYING_EDITS
                    voiceAgent.speak("ऑटो कैप्शन बना रहा हूँ।")
                    val success = actionController.executeAutoCaptions()
                    _state.value = if (success) AgentState.VERIFYING_EDITS else AgentState.RECOVERING
                    voiceAgent.speak(if (success) "कैप्शन जनरेट हो गया है।" else "कैप्शन नहीं बन सका।")
                }

                command.contains("वाटरमार्क") || command.contains("लोगो") || command.contains("watermark", ignoreCase = true) -> {
                    _state.value = AgentState.APPLYING_EDITS
                    voiceAgent.speak("एंडिंग वाटरमार्क हटा रहा हूँ।")
                    val success = actionController.deleteEndingWatermark()
                    _state.value = if (success) AgentState.VERIFYING_EDITS else AgentState.RECOVERING
                    voiceAgent.speak(if (success) "वाटरमार्क हटा दिया गया है।" else "वाटरमार्क नहीं मिला।")
                }

                command.contains("एक्सपोर्ट") || command.contains("सेव") || command.contains("export", ignoreCase = true) -> {
                    _state.value = AgentState.READY_FOR_EXPORT
                    voiceAgent.speak("एक्सपोर्ट की तैयारी हो रही है।")
                    _state.value = AgentState.EXPORTING
                    val success = actionController.exportVideo()
                    _state.value = if (success) AgentState.COMPLETED else AgentState.FAILED
                    voiceAgent.speak(if (success) "वीडियो एक्सपोर्ट पूरा हुआ।" else "एक्सपोर्ट शुरू नहीं हो सका।")
                }

                command.contains("शॉर्ट्स") || command.contains("रील्स") || command.contains("9:16") -> {
                    _state.value = AgentState.APPLYING_EDITS
                    voiceAgent.speak("रेशियो 9:16 सेट कर रहा हूँ।")
                    val success = actionController.setAspectRatio("9:16")
                    _state.value = if (success) AgentState.VERIFYING_EDITS else AgentState.RECOVERING
                }

                command.contains("रुक") || command.contains("स्टॉप") || command.contains("stop", ignoreCase = true) -> {
                    stopSession()
                }

                else -> {
                    _state.value = AgentState.BUILDING_EDIT_PLAN
                    voiceAgent.speak("आपके निर्देश समझकर प्रोसेस कर रहा हूँ।")
                }
            }
        }
    }

    fun stopSession() {
        _state.value = AgentState.STOPPED
        voiceAgent.speak("एडिटिंग रोक दी गई है।")
    }
}
