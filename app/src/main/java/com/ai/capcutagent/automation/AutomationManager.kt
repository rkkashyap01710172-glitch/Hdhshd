package com.ai.capcutagent.automation

import android.content.Context
import com.ai.capcutagent.core.AIOrchestrator
import com.ai.capcutagent.voice.HindiVoiceAgent
import com.ai.capcutagent.voice.VoiceAction
import com.ai.capcutagent.voice.VoiceCommandParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object AutomationManager {
    var engine: AutomationEngine? = null
        private set

    var agentController: com.ai.capcutagent.core.AgentController? = null
        private set

    var actionController: CapCutActionController? = null
        private set

    var voiceAgent: HindiVoiceAgent? = null
        private set

    var aiOrchestrator: AIOrchestrator? = null
        private set

    var database: com.ai.capcutagent.data.AppDatabase? = null
        private set

    var logRepository: com.ai.capcutagent.data.AutomationLogRepository? = null
        private set

    var checkpointManager: com.ai.capcutagent.core.CheckpointManager? = null
        private set

    private val orchestratorScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _customWorkflows = MutableStateFlow<List<Workflow>>(emptyList())
    val customWorkflows: StateFlow<List<Workflow>> = _customWorkflows.asStateFlow()

    private val _lastVoiceResponse = MutableStateFlow<String?>(null)
    val lastVoiceResponse: StateFlow<String?> = _lastVoiceResponse.asStateFlow()

    fun initialize(context: Context) {
        if (engine == null) {
            engine = AutomationEngine(context.applicationContext)
        }
        if (agentController == null) {
            agentController = com.ai.capcutagent.core.AgentController(context.applicationContext)
        }
        if (actionController == null) {
            actionController = CapCutActionController(context.applicationContext) {
                engine?.isSimulationMode?.value ?: false
            }
        }
        if (voiceAgent == null) {
            voiceAgent = HindiVoiceAgent(context.applicationContext) { spokenText ->
                processVoiceCommand(spokenText)
            }
        }
        if (aiOrchestrator == null && actionController != null && voiceAgent != null) {
            aiOrchestrator = AIOrchestrator(actionController!!, voiceAgent!!, orchestratorScope)
        }
        if (database == null) {
            val db = com.ai.capcutagent.data.AppDatabase.getDatabase(context.applicationContext)
            database = db
            logRepository = com.ai.capcutagent.data.AutomationLogRepository(db.automationDao())
            checkpointManager = com.ai.capcutagent.core.CheckpointManager(db.automationDao())
        }
    }

    fun recordLog(
        actionName: String,
        isSuccess: Boolean,
        targetNodeText: String? = null,
        failureReason: String? = null
    ) {
        orchestratorScope.launch(Dispatchers.IO) {
            logRepository?.insertLog(
                com.ai.capcutagent.data.AutomationLog(
                    actionName = actionName,
                    isSuccess = isSuccess,
                    targetNodeText = targetNodeText,
                    failureReason = failureReason
                )
            )
        }
    }

    fun processVoiceCommand(spokenText: String) {
        // Forward to AIOrchestrator
        aiOrchestrator?.handleVoiceCommand(spokenText)

        val action = VoiceCommandParser.parse(spokenText)
        val feedback = when (action) {
            is VoiceAction.RunPreset -> {
                val preset = PresetWorkflows.getAllPresets().find { it.id == action.presetId }
                if (preset != null) {
                    val simMode = engine?.isSimulationMode?.value ?: false
                    agentController?.startAutonomousSession(preset, simMode)
                }
                action.hindiResponse
            }

            is VoiceAction.StopExecution -> {
                engine?.stopWorkflow()
                agentController?.stopSession()
                aiOrchestrator?.stopSession()
                action.hindiResponse
            }

            is VoiceAction.ScrollTimeline -> {
                val service = CapCutAutomationService.instance
                service?.swipeTimeline(0.65f, 0.65f, 0.35f, 0.65f, 300)
                action.hindiResponse
            }

            is VoiceAction.GenerateCustom -> {
                val generated = generateWorkflowFromPrompt(action.prompt)
                addCustomWorkflow(generated)
                val simMode = engine?.isSimulationMode?.value ?: false
                agentController?.startAutonomousSession(generated, simMode)
                action.hindiResponse
            }

            is VoiceAction.Unrecognized -> action.hindiResponse
        }

        _lastVoiceResponse.value = feedback
    }

    fun addCustomWorkflow(workflow: Workflow) {
        _customWorkflows.value = _customWorkflows.value + workflow
        CapCutAutomationService.log(
            LogLevel.SUCCESS,
            "Workflow",
            "Added custom workflow: '${workflow.title}' with ${workflow.steps.size} steps"
        )
    }

    fun deleteCustomWorkflow(id: String) {
        _customWorkflows.value = _customWorkflows.value.filter { it.id != id }
    }

    private fun generateWorkflowFromPrompt(prompt: String): Workflow {
        val lower = prompt.lowercase()
        val steps = mutableListOf<AutomationStep>()

        if (lower.contains("कट") || lower.contains("split") || lower.contains("काटो")) {
            steps.add(
                AutomationStep(
                    title = "Tap Playhead",
                    description = "Position playhead on clip",
                    action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.5f, yRatio = 0.65f),
                    delayAfterMs = 500
                )
            )
            steps.add(
                AutomationStep(
                    title = "Click 'Split'",
                    description = "Perform split at playhead",
                    action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Split"),
                    delayAfterMs = 600
                )
            )
        }

        if (lower.contains("कैप्शन") || lower.contains("caption")) {
            steps.add(
                AutomationStep(
                    title = "Select 'Text'",
                    description = "Open text panel",
                    action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Text"),
                    delayAfterMs = 800
                )
            )
            steps.add(
                AutomationStep(
                    title = "Tap 'Auto captions'",
                    description = "Open speech-to-text",
                    action = AutomationAction(type = ActionType.CLICK_TEXT, targetText = "Auto captions"),
                    delayAfterMs = 1200
                )
            )
        }

        if (steps.isEmpty()) {
            steps.add(
                AutomationStep(
                    title = "Tap Timeline",
                    description = "Focus active clip",
                    action = AutomationAction(type = ActionType.CLICK_COORDINATES, xRatio = 0.5f, yRatio = 0.65f),
                    delayAfterMs = 500
                )
            )
        }

        return Workflow(
            title = "Voice: ${prompt.take(24)}",
            description = "Generated from Hindi voice command: '$prompt'",
            category = "Custom",
            steps = steps,
            isPreset = false
        )
    }
}
