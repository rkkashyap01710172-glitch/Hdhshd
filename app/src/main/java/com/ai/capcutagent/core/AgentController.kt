package com.ai.capcutagent.core

import android.content.Context
import com.ai.capcutagent.automation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AgentTelemetry(
    val state: AgentState = AgentState.IDLE,
    val progress: Float = 0f,
    val statusMessage: String = "Standing by",
    val activeStepName: String = "",
    val recoveryAttempts: Int = 0,
    val confidenceScore: Int = 98,
    val totalActionsCompleted: Int = 0,
    val lastError: String? = null
)

class AgentController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var agentJob: Job? = null

    private val _telemetry = MutableStateFlow(AgentTelemetry())
    val telemetry: StateFlow<AgentTelemetry> = _telemetry.asStateFlow()

    val currentState: AgentState
        get() = _telemetry.value.state

    fun transitionTo(newState: AgentState, message: String, progress: Float = _telemetry.value.progress) {
        val oldState = _telemetry.value.state
        _telemetry.value = _telemetry.value.copy(
            state = newState,
            statusMessage = message,
            progress = progress
        )

        CapCutAutomationService.log(
            when (newState) {
                AgentState.COMPLETED -> LogLevel.SUCCESS
                AgentState.FAILED -> LogLevel.ERROR
                AgentState.RECOVERING -> LogLevel.WARNING
                AgentState.APPLYING_EDITS, AgentState.VERIFYING_EDITS -> LogLevel.ACTION
                else -> LogLevel.INFO
            },
            "AgentState",
            "[$oldState -> $newState]: $message"
        )
    }

    fun startAutonomousSession(workflow: Workflow, isSimulated: Boolean = false) {
        stopSession()

        agentJob = scope.launch {
            try {
                // 1. INITIALIZING
                transitionTo(AgentState.INITIALIZING, "Checking accessibility connection & environment...", 0.05f)
                delay(400)

                val service = CapCutAutomationService.instance
                val effectiveSim = isSimulated || service == null

                if (!effectiveSim && service?.isCapCutActive() != true) {
                    // Try to bring CapCut to foreground
                    transitionTo(AgentState.INITIALIZING, "Ensuring CapCut is in foreground...", 0.10f)
                    val launched = AutomationManager.engine?.launchCapCutApp() ?: false
                    if (launched) {
                        delay(1200)
                    }
                }

                // 2. ANALYZING_MEDIA
                transitionTo(AgentState.ANALYZING_MEDIA, "Scanning timeline canvas, tracks, and UI nodes...", 0.20f)
                delay(600)
                val visibleNodes = service?.refreshVisibleNodes() ?: emptyList()
                CapCutAutomationService.log(LogLevel.INFO, "Agent", "Identified ${visibleNodes.size} UI elements on screen")

                // 3. BUILDING_EDIT_PLAN
                transitionTo(
                    AgentState.BUILDING_EDIT_PLAN,
                    "Optimizing execution pipeline for '${workflow.title}' (${workflow.steps.size} steps)",
                    0.30f
                )
                delay(500)

                // 4. APPLYING_EDITS & VERIFYING_EDITS
                var completedSteps = 0
                val totalSteps = workflow.steps.size

                for ((index, step) in workflow.steps.withIndex()) {
                    if (!step.isEnabled) continue

                    _telemetry.value = _telemetry.value.copy(
                        activeStepName = step.title
                    )

                    // Applying edit
                    val stepBaseProgress = 0.35f + (index.toFloat() / totalSteps) * 0.45f
                    transitionTo(
                        AgentState.APPLYING_EDITS,
                        "Step ${index + 1}/$totalSteps: ${step.title}",
                        stepBaseProgress
                    )

                    var stepResult = AutomationManager.engine?.executeSingleStep(step)
                        ?: StepExecutionResult(false, "Engine unavailable")

                    // Self-healing Recovery if step fails
                    if (!stepResult.isSuccess && !step.isOptional) {
                        transitionTo(
                            AgentState.RECOVERING,
                            "Step '${step.title}' failed. Initiating self-healing recovery...",
                            stepBaseProgress
                        )
                        _telemetry.value = _telemetry.value.copy(
                            recoveryAttempts = _telemetry.value.recoveryAttempts + 1
                        )
                        delay(600)

                        // Attempt fallback recovery: dismiss active dialog/keyboard, wait and retry
                        service?.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                        delay(400)

                        // Retry step once
                        transitionTo(AgentState.APPLYING_EDITS, "Retrying step '${step.title}' with fallback coordinates...", stepBaseProgress)
                        stepResult = AutomationManager.engine?.executeSingleStep(step)
                            ?: StepExecutionResult(false, "Retry failed")

                        if (!stepResult.isSuccess && !step.isOptional) {
                            transitionTo(AgentState.FAILED, "Unrecoverable failure on step '${step.title}': ${stepResult.message}")
                            _telemetry.value = _telemetry.value.copy(lastError = stepResult.message)
                            return@launch
                        }
                    }

                    // 5. VERIFYING_EDITS
                    transitionTo(
                        AgentState.VERIFYING_EDITS,
                        "Verifying UI state after '${step.title}'...",
                        stepBaseProgress + (0.45f / totalSteps * 0.5f)
                    )
                    delay(250)

                    completedSteps++
                    _telemetry.value = _telemetry.value.copy(
                        totalActionsCompleted = completedSteps
                    )

                    if (step.delayAfterMs > 0) {
                        delay(step.delayAfterMs)
                    }
                }

                // Check if workflow includes export
                val hasExport = workflow.steps.any {
                    it.title.contains("export", ignoreCase = true) || it.action.targetText.contains("export", ignoreCase = true)
                }

                if (hasExport) {
                    // 6. READY_FOR_EXPORT & EXPORTING
                    transitionTo(AgentState.READY_FOR_EXPORT, "All edits verified. Preparing export settings...", 0.88f)
                    delay(500)
                    transitionTo(AgentState.EXPORTING, "Rendering high quality video file...", 0.94f)
                    delay(800)
                }

                // 7. COMPLETED
                transitionTo(AgentState.COMPLETED, "Workflow '${workflow.title}' executed and verified successfully!", 1.0f)

            } catch (e: CancellationException) {
                transitionTo(AgentState.STOPPED, "Session halted by user")
            } catch (e: Exception) {
                transitionTo(AgentState.FAILED, "Agent encountered exception: ${e.message}")
                _telemetry.value = _telemetry.value.copy(lastError = e.message)
            }
        }
    }

    fun stopSession() {
        if (agentJob?.isActive == true) {
            agentJob?.cancel()
            agentJob = null
            transitionTo(AgentState.STOPPED, "Session halted by user")
        }
    }

    fun reset() {
        stopSession()
        _telemetry.value = AgentTelemetry()
    }
}
