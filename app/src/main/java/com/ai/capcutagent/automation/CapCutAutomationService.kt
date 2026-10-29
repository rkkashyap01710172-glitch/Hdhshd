package com.ai.capcutagent.automation

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.coroutines.resume

class CapCutAutomationService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    companion object {
        var instance: CapCutAutomationService? = null
            private set

        const val CAPCUT_PACKAGE = "com.lemon.lvoverseas"
        const val CAPCUT_PACKAGE_CN = "com.lemon.lv"

        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        private val _currentActivePackage = MutableStateFlow<String?>(null)
        val currentActivePackage: StateFlow<String?> = _currentActivePackage.asStateFlow()

        private val _serviceLogs = MutableStateFlow<List<AutomationLog>>(emptyList())
        val serviceLogs: StateFlow<List<AutomationLog>> = _serviceLogs.asStateFlow()

        private val _lastCapturedNodes = MutableStateFlow<List<String>>(emptyList())
        val lastCapturedNodes: StateFlow<List<String>> = _lastCapturedNodes.asStateFlow()

        fun log(level: LogLevel, tag: String, message: String) {
            val entry = AutomationLog(level = level, tag = tag, message = message)
            _serviceLogs.value = (_serviceLogs.value + entry).takeLast(200)
        }

        fun clearLogs() {
            _serviceLogs.value = emptyList()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true
        log(LogLevel.SUCCESS, "Service", "CapCut Automation Service connected and ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val pkg = event.packageName?.toString()
        if (pkg != null) {
            _currentActivePackage.value = pkg
            if (pkg == CAPCUT_PACKAGE || pkg == CAPCUT_PACKAGE_CN) {
                // Periodically capture interesting node labels when CapCut window changes
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                    refreshVisibleNodes()
                }
            }
        }
    }

    override fun onInterrupt() {
        log(LogLevel.WARNING, "Service", "CapCut Automation Service interrupted")
        _isServiceConnected.value = false
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        _isServiceConnected.value = false
        instance = null
        log(LogLevel.INFO, "Service", "CapCut Automation Service destroyed")
    }

    /**
     * टेक्स्ट या डिस्क्रिप्शन के आधार पर नोड खोजें और क्लिक करें
     */
    fun findAndClick(targetText: String, timeoutMs: Long = 5000L): Boolean {
        log(LogLevel.ACTION, "FindClick", "Searching node for: '$targetText'...")
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            log(LogLevel.WARNING, "FindClick", "Root window is null")
            return false
        }

        val matchedNodes = mutableListOf<AccessibilityNodeInfo>()

        // 1. टेक्स्ट सर्च
        matchedNodes.addAll(rootNode.findAccessibilityNodeInfosByText(targetText))

        // 2. फ़ॉलबैक: व्यू डिस्क्रिप्शन या लूज़ मैच
        if (matchedNodes.isEmpty()) {
            findNodesRecursively(rootNode, targetText, matchedNodes)
        }

        if (matchedNodes.isEmpty()) {
            log(LogLevel.WARNING, "FindClick", "No nodes matched '$targetText'")
            return false
        }

        for (node in matchedNodes) {
            val textFound = node.text?.toString() ?: node.contentDescription?.toString() ?: "Unnamed"
            if (clickNodeOrParent(node)) {
                log(LogLevel.SUCCESS, "FindClick", "Clicked node: '$textFound'")
                return true
            }
        }

        log(LogLevel.WARNING, "FindClick", "Matched ${matchedNodes.size} nodes, but could not trigger click")
        return false
    }

    private fun findNodesRecursively(
        node: AccessibilityNodeInfo?,
        query: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        if (node == null) return
        val text = node.text?.toString() ?: ""
        val desc = node.contentDescription?.toString() ?: ""

        if (text.contains(query, ignoreCase = true) || desc.contains(query, ignoreCase = true)) {
            results.add(node)
        }

        for (i in 0 until node.childCount) {
            findNodesRecursively(node.getChild(i), query, results)
        }
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo): Boolean {
        var current: AccessibilityNodeInfo? = node
        while (current != null) {
            if (current.isClickable) {
                val actionResult = current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                if (actionResult) return true
            }
            current = current.parent
        }

        // यदि नोड क्लिक करने योग्य नहीं है, तो उसके बाउंड्स पर टैप जेस्चर करें
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        if (bounds.width() > 0 && bounds.height() > 0) {
            val cx = bounds.centerX().toFloat()
            val cy = bounds.centerY().toFloat()
            log(LogLevel.INFO, "FindClick", "Fallback tap gesture at ($cx, $cy)")
            return clickAtCoordinates(cx, cy)
        }
        return false
    }

    /**
     * सटीक निर्देशांक (Coordinates) पर टैप जेस्चर
     */
    fun clickAtCoordinates(x: Float, y: Float): Boolean {
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, null, null)
        log(
            if (dispatched) LogLevel.ACTION else LogLevel.ERROR,
            "Gesture",
            "Tap at (${x.toInt()}, ${y.toInt()}): dispatched=$dispatched"
        )
        return dispatched
    }

    /**
     * Asynchronous tap at coordinates with callback suspension
     */
    suspend fun clickAtCoordinatesAsync(x: Float, y: Float): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply { moveTo(x, y) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                log(LogLevel.SUCCESS, "Gesture", "Tap completed at (${x.toInt()}, ${y.toInt()})")
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                log(LogLevel.WARNING, "Gesture", "Tap cancelled at (${x.toInt()}, ${y.toInt()})")
                if (continuation.isActive) continuation.resume(false)
            }
        }

        val dispatched = dispatchGesture(gesture, callback, null)
        if (!dispatched && continuation.isActive) {
            log(LogLevel.ERROR, "Gesture", "Failed to dispatch tap gesture")
            continuation.resume(false)
        }
    }

    /**
     * टाइमलाइन पर स्वाइप (स्क्रॉल) करने के लिए जेस्चर
     */
    fun swipeTimeline(startX: Float, startY: Float, endX: Float, endY: Float, durationMs: Long = 300): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val dispatched = dispatchGesture(gesture, null, null)
        log(
            if (dispatched) LogLevel.ACTION else LogLevel.ERROR,
            "TimelineSwipe",
            "Swiped from (${startX.toInt()}, ${startY.toInt()}) to (${endX.toInt()}, ${endY.toInt()}): dispatched=$dispatched"
        )
        return dispatched
    }

    /**
     * Asynchronous swipe along timeline with callback suspension
     */
    suspend fun swipeTimelineAsync(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        durationMs: Long = 300
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                log(LogLevel.SUCCESS, "TimelineSwipe", "Swipe gesture completed (${startX.toInt()} -> ${endX.toInt()})")
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                log(LogLevel.WARNING, "TimelineSwipe", "Swipe gesture cancelled")
                if (continuation.isActive) continuation.resume(false)
            }
        }

        val dispatched = dispatchGesture(gesture, callback, null)
        if (!dispatched && continuation.isActive) {
            log(LogLevel.ERROR, "TimelineSwipe", "Failed to dispatch swipe gesture")
            continuation.resume(false)
        }
    }

    /**
     * Pinch zoom gesture (simulate 2-finger timeline zoom in/out)
     */
    suspend fun pinchTimelineAsync(zoomIn: Boolean, durationMs: Long = 350): Boolean = suspendCancellableCoroutine { continuation ->
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.toFloat()
        val height = metrics.heightPixels.toFloat()
        val midY = height * 0.65f
        val midX = width * 0.5f

        val path1 = Path()
        val path2 = Path()

        if (zoomIn) {
            // Fingers move apart
            path1.moveTo(midX - 50, midY)
            path1.lineTo(midX - 220, midY)
            path2.moveTo(midX + 50, midY)
            path2.lineTo(midX + 220, midY)
        } else {
            // Fingers move together
            path1.moveTo(midX - 220, midY)
            path1.lineTo(midX - 50, midY)
            path2.moveTo(midX + 220, midY)
            path2.lineTo(midX + 50, midY)
        }

        val stroke1 = GestureDescription.StrokeDescription(path1, 0, durationMs)
        val stroke2 = GestureDescription.StrokeDescription(path2, 0, durationMs)
        val gesture = GestureDescription.Builder()
            .addStroke(stroke1)
            .addStroke(stroke2)
            .build()

        val callback = object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                log(LogLevel.SUCCESS, "PinchZoom", "Pinch gesture completed (zoomIn=$zoomIn)")
                if (continuation.isActive) continuation.resume(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                log(LogLevel.WARNING, "PinchZoom", "Pinch gesture cancelled")
                if (continuation.isActive) continuation.resume(false)
            }
        }

        val dispatched = dispatchGesture(gesture, callback, null)
        if (!dispatched && continuation.isActive) {
            log(LogLevel.ERROR, "PinchZoom", "Failed to dispatch pinch gesture")
            continuation.resume(false)
        }
    }

    /**
     * Inspect and dump currently visible nodes and button labels in the active window
     */
    fun refreshVisibleNodes(): List<String> {
        val root = rootInActiveWindow ?: return emptyList()
        val list = mutableListOf<String>()
        collectNodeLabels(root, list)
        _lastCapturedNodes.value = list
        return list
    }

    private fun collectNodeLabels(node: AccessibilityNodeInfo?, list: MutableList<String>) {
        if (node == null) return
        val text = node.text?.toString()?.trim()
        val desc = node.contentDescription?.toString()?.trim()
        val label = if (!text.isNullOrEmpty()) text else desc
        if (!label.isNullOrEmpty() && !list.contains(label)) {
            list.add(label)
        }
        for (i in 0 until node.childCount) {
            collectNodeLabels(node.getChild(i), list)
        }
    }

    fun isCapCutActive(): Boolean {
        val current = _currentActivePackage.value
        return current == CAPCUT_PACKAGE || current == CAPCUT_PACKAGE_CN
    }
}
