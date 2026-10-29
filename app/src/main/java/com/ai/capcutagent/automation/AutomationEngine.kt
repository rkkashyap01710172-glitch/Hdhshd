package com.ai.capcutagent.automation

import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AutomationEngine(private val context: Context) {

    private val engineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var runningJob: Job? = null

    private val _executionState = MutableStateFlow<ExecutionState>(ExecutionState.Idle)
    val executionState: StateFlow<ExecutionState> = _executionState.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(false)
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    private val _activeWorkflow = MutableStateFlow<Workflow?>(null)
    val activeWorkflow: StateFlow<Workflow?> = _activeWorkflow.asStateFlow()

    private val _currentStepIndex = MutableStateFlow(-1)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // Callback for simulator visualizer
    var onSimulatorStepAction: ((AutomationStep) -> Unit)? = null

    fun setSimulationMode(enabled: Boolean) {
        _isSimulationMode.value = enabled
        CapCutAutomationService.log(
            LogLevel.INFO,
            "Engine",
            if (enabled) "Simulation mode ENABLED (virtual testing)" else "Real device execution mode ENABLED"
        )
    }

    fun startWorkflow(workflow: Workflow) {
        stopWorkflow()
        _activeWorkflow.value = workflow
        _executionState.value = ExecutionState.Running(0, workflow.steps.size, workflow.steps.firstOrNull()?.title ?: "")

        runningJob = engineScope.launch {
            val startTime = System.currentTimeMillis()
            var stepIndex = 0

            CapCutAutomationService.log(
                LogLevel.INFO,
                "Engine",
                "Starting workflow: '${workflow.title}' with ${workflow.steps.size} steps"
            )

            for ((index, step) in workflow.steps.withIndex()) {
                if (!step.isEnabled) {
                    CapCutAutomationService.log(LogLevel.INFO, "Engine", "Skipping disabled step: ${step.title}")
                    continue
                }

                stepIndex = index
                _currentStepIndex.value = index
                _executionState.value = ExecutionState.Running(index, workflow.steps.size, step.title)

                vibrateBriefly()

                val result = executeSingleStep(step)

                if (!result.isSuccess && !step.isOptional) {
                    CapCutAutomationService.log(
                        LogLevel.ERROR,
                        "Engine",
                        "Step failed: '${step.title}' - ${result.message}"
                    )
                    _executionState.value = ExecutionState.Failed(index, step.title, result.message)
                    _currentStepIndex.value = -1
                    return@launch
                }

                // Wait step delay
                if (step.delayAfterMs > 0) {
                    delay(step.delayAfterMs)
                }
            }

            val elapsed = System.currentTimeMillis() - startTime
            CapCutAutomationService.log(
                LogLevel.SUCCESS,
                "Engine",
                "Workflow '${workflow.title}' completed successfully in ${elapsed}ms"
            )
            _executionState.value = ExecutionState.Completed(workflow.steps.size, elapsed)
            _currentStepIndex.value = -1
        }
    }

    suspend fun executeSingleStep(step: AutomationStep): StepExecutionResult {
        CapCutAutomationService.log(
            LogLevel.ACTION,
            "Step",
            "Executing [${step.title}]: ${step.description}"
        )

        // Notify simulator UI if listening
        onSimulatorStepAction?.invoke(step)

        val service = CapCutAutomationService.instance
        val simMode = _isSimulationMode.value || service == null

        if (simMode) {
            // High fidelity simulation
            delay(350L)
            CapCutAutomationService.log(
                LogLevel.SUCCESS,
                "Simulator",
                "Simulated: ${step.action.type} (${step.title})"
            )
            return StepExecutionResult(true, "Simulated successfully")
        }

        // Real execution via Accessibility Service
        val action = step.action
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        return when (action.type) {
            ActionType.CLICK_TEXT -> {
                val success = service.findAndClick(action.targetText, action.timeoutMs)
                if (success) {
                    StepExecutionResult(true, "Clicked text '${action.targetText}'")
                } else {
                    StepExecutionResult(false, "Could not find or click '${action.targetText}'")
                }
            }

            ActionType.CLICK_COORDINATES -> {
                val absX = action.xRatio * screenW
                val absY = action.yRatio * screenH
                val success = service.clickAtCoordinatesAsync(absX, absY)
                StepExecutionResult(success, "Tap at ratio (${action.xRatio}, ${action.yRatio})")
            }

            ActionType.SWIPE_TIMELINE -> {
                val startX = action.startXRatio * screenW
                val startY = action.startYRatio * screenH
                val endX = action.endXRatio * screenW
                val endY = action.endYRatio * screenH
                val success = service.swipeTimelineAsync(startX, startY, endX, endY, action.durationMs)
                StepExecutionResult(success, "Timeline swipe dispatched")
            }

            ActionType.PINCH_ZOOM -> {
                val success = service.pinchTimelineAsync(zoomIn = true, durationMs = action.durationMs)
                StepExecutionResult(success, "Pinch zoom dispatched")
            }

            ActionType.WAIT -> {
                delay(action.durationMs)
                StepExecutionResult(true, "Waited ${action.durationMs}ms")
            }

            ActionType.PRESS_BACK -> {
                val success = service.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
                StepExecutionResult(success, "Pressed Back button")
            }

            ActionType.LAUNCH_CAPCUT -> {
                val launched = launchCapCutApp()
                StepExecutionResult(launched, if (launched) "CapCut launched" else "CapCut not installed")
            }
        }
    }

    fun stopWorkflow() {
        runningJob?.cancel()
        runningJob = null
        _executionState.value = ExecutionState.Idle
        _currentStepIndex.value = -1
        CapCutAutomationService.log(LogLevel.WARNING, "Engine", "Workflow execution halted")
    }

    fun launchCapCutApp(): Boolean {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(CapCutAutomationService.CAPCUT_PACKAGE)
            ?: pm.getLaunchIntentForPackage(CapCutAutomationService.CAPCUT_PACKAGE_CN)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            CapCutAutomationService.log(LogLevel.SUCCESS, "Launcher", "Launched CapCut successfully")
            true
        } else {
            CapCutAutomationService.log(LogLevel.WARNING, "Launcher", "CapCut package not installed on device")
            false
        }
    }

    fun isCapCutInstalled(): Boolean {
        val pm = context.packageManager
        return try {
            pm.getPackageInfo(CapCutAutomationService.CAPCUT_PACKAGE, 0)
            true
        } catch (_: Exception) {
            try {
                pm.getPackageInfo(CapCutAutomationService.CAPCUT_PACKAGE_CN, 0)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun vibrateBriefly() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
        } catch (_: Exception) {}
    }
}
