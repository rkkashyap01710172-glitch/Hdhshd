package com.ai.capcutagent.automation

import android.content.Context
import kotlinx.coroutines.delay

class CapCutActionController(
    private val context: Context,
    private val isSimulationSupplier: () -> Boolean = { false }
) {

    private fun getService(): CapCutAutomationService? = CapCutAutomationService.instance

    /**
     * Executes split action on the current timeline playhead position
     */
    suspend fun executeSplit(): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Executing Split on active clip...")

        if (isSimulationSupplier() || getService() == null) {
            delay(400)
            CapCutAutomationService.log(LogLevel.SUCCESS, "ActionController", "Split executed in simulator")
            return true
        }

        val service = getService() ?: return false

        // 1. Ensure clip is active by tapping center playhead
        val metrics = context.resources.displayMetrics
        val cx = metrics.widthPixels * 0.5f
        val cy = metrics.heightPixels * 0.65f
        service.clickAtCoordinates(cx, cy)
        delay(350)

        // 2. Find and click "Split" or Hindi translation "काटें"
        var clicked = service.findAndClick("Split")
        if (!clicked) {
            clicked = service.findAndClick("काटें")
        }

        // 3. Fallback coordinates for bottom-left Split tool in CapCut
        if (!clicked) {
            val splitBtnX = metrics.widthPixels * 0.12f
            val splitBtnY = metrics.heightPixels * 0.94f
            clicked = service.clickAtCoordinates(splitBtnX, splitBtnY)
        }

        CapCutAutomationService.log(
            if (clicked) LogLevel.SUCCESS else LogLevel.WARNING,
            "ActionController",
            "Split execution result: $clicked"
        )
        AutomationManager.recordLog("SPLIT_CLIP", isSuccess = clicked, targetNodeText = "Split")
        return clicked
    }

    /**
     * Opens the video speed adjustment sub-menu
     */
    suspend fun openSpeedMenu(): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Opening Speed menu...")

        if (isSimulationSupplier() || getService() == null) {
            delay(350)
            CapCutAutomationService.log(LogLevel.SUCCESS, "ActionController", "Speed menu opened in simulator")
            AutomationManager.recordLog("SPEED_CHANGE", isSuccess = true, targetNodeText = "Speed (Simulated)")
            return true
        }

        val service = getService() ?: return false
        var clicked = service.findAndClick("Speed")
        if (!clicked) {
            clicked = service.findAndClick("गति")
        }

        if (!clicked) {
            val metrics = context.resources.displayMetrics
            val speedX = metrics.widthPixels * 0.25f
            val speedY = metrics.heightPixels * 0.94f
            clicked = service.clickAtCoordinates(speedX, speedY)
        }

        AutomationManager.recordLog("SPEED_CHANGE", isSuccess = clicked, targetNodeText = "Speed")
        return clicked
    }

    /**
     * Triggers speech-to-text auto captions
     */
    suspend fun executeAutoCaptions(): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Triggering Auto Captions...")
        if (isSimulationSupplier() || getService() == null) {
            delay(600)
            return true
        }

        val service = getService() ?: return false
        service.findAndClick("Text")
        delay(600)
        service.findAndClick("Auto captions")
        delay(800)
        return service.findAndClick("Generate")
    }

    /**
     * Removes the ending CapCut watermark outro clip
     */
    suspend fun deleteEndingWatermark(): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Removing ending watermark...")
        if (isSimulationSupplier() || getService() == null) {
            delay(500)
            return true
        }

        val service = getService() ?: return false
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        // Fast scrub to end
        service.swipeTimeline(screenW * 0.2f, screenH * 0.65f, screenW * 0.9f, screenH * 0.65f, 400)
        delay(600)

        // Select outro clip
        service.clickAtCoordinates(screenW * 0.78f, screenH * 0.65f)
        delay(500)

        return service.findAndClick("Delete") || service.findAndClick("हटाएं")
    }

    /**
     * Sets aspect ratio (e.g. 9:16 vertical shorts)
     */
    suspend fun setAspectRatio(ratio: String = "9:16"): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Setting aspect ratio to $ratio...")
        if (isSimulationSupplier() || getService() == null) {
            delay(400)
            return true
        }

        val service = getService() ?: return false
        service.findAndClick("Ratio")
        delay(600)
        return service.findAndClick(ratio)
    }

    /**
     * Triggers video export
     */
    suspend fun exportVideo(): Boolean {
        CapCutAutomationService.log(LogLevel.ACTION, "ActionController", "Triggering video export...")
        if (isSimulationSupplier() || getService() == null) {
            delay(500)
            return true
        }

        val service = getService() ?: return false
        return service.findAndClick("Export") || service.findAndClick("एक्सपोर्ट")
    }

    /**
     * Swipes timeline forward or backward
     */
    suspend fun seekTimeline(forward: Boolean, durationMs: Long = 300): Boolean {
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        val startX = if (forward) screenW * 0.65f else screenW * 0.35f
        val endX = if (forward) screenW * 0.35f else screenW * 0.65f
        val y = screenH * 0.65f

        if (isSimulationSupplier() || getService() == null) {
            delay(durationMs)
            return true
        }

        return getService()?.swipeTimeline(startX, y, endX, y, durationMs) ?: false
    }
}
