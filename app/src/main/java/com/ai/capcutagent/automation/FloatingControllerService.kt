package com.ai.capcutagent.automation

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FloatingControllerService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    companion object {
        private val _isOverlayActive = MutableStateFlow(false)
        val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

        fun isOverlayPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(context)
            } else {
                true
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (!isOverlayPermissionGranted(this)) {
            stopSelf()
            return
        }

        try {
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 40
                y = 200
            }

            // Create simple dynamic overlay layout
            val context = this
            val container = android.widget.LinearLayout(context).apply {
                orientation = android.widget.LinearLayout.HORIZONTAL
                setPadding(24, 16, 24, 16)
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xEE0F172A.toInt())
                    cornerRadius = 32f
                    setStroke(2, 0xFF38BDF8.toInt())
                }
                elevation = 16f
            }

            val titleText = TextView(context).apply {
                text = "⚡ CapCut Agent"
                setTextColor(0xFF38BDF8.toInt())
                textSize = 13f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(8, 8, 16, 8)
            }

            val runBtn = Button(context).apply {
                text = "▶ Run"
                textSize = 12f
                setTextColor(0xFFFFFFFF.toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(0xFF0284C7.toInt())
                    cornerRadius = 20f
                }
                setPadding(16, 4, 16, 4)
                setOnClickListener {
                    CapCutAutomationService.log(LogLevel.ACTION, "Overlay", "Triggered from floating bubble")
                    val preset = PresetWorkflows.getAllPresets().firstOrNull()
                    if (preset != null) {
                        AutomationManager.engine?.startWorkflow(preset)
                    }
                }
            }

            val closeBtn = Button(context).apply {
                text = "✕"
                textSize = 12f
                setTextColor(0xFF94A3B8.toInt())
                background = null
                setOnClickListener {
                    stopSelf()
                }
            }

            container.addView(titleText)
            container.addView(runBtn)
            container.addView(closeBtn)

            // Make draggable
            container.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            params.x = initialX + (event.rawX - initialTouchX).toInt()
                            params.y = initialY + (event.rawY - initialTouchY).toInt()
                            try {
                                windowManager?.updateViewLayout(container, params)
                            } catch (_: Exception) {}
                            return true
                        }
                    }
                    return false
                }
            })

            windowManager?.addView(container, params)
            floatingView = container
            _isOverlayActive.value = true
            CapCutAutomationService.log(LogLevel.SUCCESS, "Overlay", "Floating Controller active on screen")

        } catch (e: Exception) {
            CapCutAutomationService.log(LogLevel.ERROR, "Overlay", "Failed to show overlay: ${e.message}")
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        floatingView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
        }
        floatingView = null
        _isOverlayActive.value = false
        CapCutAutomationService.log(LogLevel.INFO, "Overlay", "Floating Controller closed")
    }
}
