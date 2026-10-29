package com.example

import com.ai.capcutagent.automation.ActionType
import com.ai.capcutagent.automation.PresetWorkflows
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AutomationWorkflowTest {

    @Test
    fun testPresetWorkflowsAreValid() {
        val presets = PresetWorkflows.getAllPresets()
        assertFalse("Presets should not be empty", presets.isEmpty())

        for (preset in presets) {
            assertNotNull(preset.id)
            assertNotNull(preset.title)
            assertFalse("Preset ${preset.title} has empty steps", preset.steps.isEmpty())

            for (step in preset.steps) {
                assertNotNull(step.title)
                assertTrue("Step delay should be >= 0", step.delayAfterMs >= 0)

                if (step.action.type == ActionType.CLICK_COORDINATES) {
                    assertTrue(step.action.xRatio in 0f..1f)
                    assertTrue(step.action.yRatio in 0f..1f)
                }
            }
        }
    }

    @Test
    fun testAgentStatesAreDefined() {
        val states = com.ai.capcutagent.core.AgentState.values()
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.IDLE))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.INITIALIZING))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.ANALYZING_MEDIA))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.BUILDING_EDIT_PLAN))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.APPLYING_EDITS))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.VERIFYING_EDITS))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.RECOVERING))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.READY_FOR_EXPORT))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.EXPORTING))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.COMPLETED))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.STOPPED))
        assertTrue(states.contains(com.ai.capcutagent.core.AgentState.FAILED))
    }

    @Test
    fun testVoiceCommandParserHindi() {
        val splitAction = com.ai.capcutagent.voice.VoiceCommandParser.parse("कट लगाओ")
        assertTrue(splitAction is com.ai.capcutagent.voice.VoiceAction.RunPreset)
        org.junit.Assert.assertEquals("preset_auto_split", (splitAction as com.ai.capcutagent.voice.VoiceAction.RunPreset).presetId)

        val captionAction = com.ai.capcutagent.voice.VoiceCommandParser.parse("ऑटो कैप्शन बनाओ")
        assertTrue(captionAction is com.ai.capcutagent.voice.VoiceAction.RunPreset)
        org.junit.Assert.assertEquals("preset_auto_captions", (captionAction as com.ai.capcutagent.voice.VoiceAction.RunPreset).presetId)

        val watermarkAction = com.ai.capcutagent.voice.VoiceCommandParser.parse("वाटरमार्क हटाओ")
        assertTrue(watermarkAction is com.ai.capcutagent.voice.VoiceAction.RunPreset)
        org.junit.Assert.assertEquals("preset_watermark_remover", (watermarkAction as com.ai.capcutagent.voice.VoiceAction.RunPreset).presetId)

        val stopAction = com.ai.capcutagent.voice.VoiceCommandParser.parse("स्टॉप करो")
        assertTrue(stopAction is com.ai.capcutagent.voice.VoiceAction.StopExecution)
    }

    @Test
    fun testAgentUiStateAndOperatingMode() {
        val defaultState = com.ai.capcutagent.ui.AgentUiState()
        org.junit.Assert.assertEquals(com.ai.capcutagent.core.AgentState.IDLE, defaultState.agentState)
        org.junit.Assert.assertEquals(com.ai.capcutagent.ui.OperatingMode.AUTO, defaultState.mode)
        assertFalse(defaultState.isAccessibilityEnabled)
        org.junit.Assert.assertEquals("तैयार हैं सर, एडिटिंग शुरू करें।", defaultState.statusMessage)

        val modes = com.ai.capcutagent.ui.OperatingMode.values()
        assertTrue(modes.contains(com.ai.capcutagent.ui.OperatingMode.ONLINE))
        assertTrue(modes.contains(com.ai.capcutagent.ui.OperatingMode.OFFLINE))
        assertTrue(modes.contains(com.ai.capcutagent.ui.OperatingMode.AUTO))
    }

    @Test
    fun testAutomationLogEntity() {
        val log = com.ai.capcutagent.data.AutomationLog(
            actionName = "SPLIT_CLIP",
            isSuccess = true,
            targetNodeText = "Split"
        )
        org.junit.Assert.assertEquals("SPLIT_CLIP", log.actionName)
        assertTrue(log.isSuccess)
        org.junit.Assert.assertEquals("Split", log.targetNodeText)
        org.junit.Assert.assertNull(log.failureReason)
        assertTrue(log.timestamp > 0)
    }

    @Test
    fun testAutomationDaoInterface() {
        val methods = com.ai.capcutagent.data.AutomationDao::class.java.methods
        val methodNames = methods.map { it.name }
        assertTrue(methodNames.contains("insertLog"))
        assertTrue(methodNames.contains("getRecentLogs"))
        assertTrue(methodNames.contains("getSuccessCount"))
        assertTrue(methodNames.contains("clearLogs"))
    }

    @Test
    fun testCheckpointManagerClass() {
        val methods = com.ai.capcutagent.core.CheckpointManager::class.java.methods
        val methodNames = methods.map { it.name }
        assertTrue(methodNames.contains("recordAction"))
        assertTrue(methodNames.contains("getLastVerifiedStep"))
    }
}
