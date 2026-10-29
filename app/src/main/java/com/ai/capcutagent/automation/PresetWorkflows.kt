package com.ai.capcutagent.automation

object PresetWorkflows {

    fun getAllPresets(): List<Workflow> = listOf(
        Workflow(
            id = "preset_auto_split",
            title = "Smart Beat Cut / Auto Split",
            description = "Automatically scrubs the timeline at intervals and splits clips for fast-paced edits and jump cuts.",
            category = "Cutting",
            iconKey = "scissors",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Select Timeline Clip",
                    description = "Tap on the video clip at the center of the timeline",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.5f,
                        yRatio = 0.65f
                    ),
                    delayAfterMs = 500
                ),
                AutomationStep(
                    title = "Tap 'Split'",
                    description = "Click the Split tool button on the bottom toolbar",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Split"
                    ),
                    delayAfterMs = 600
                ),
                AutomationStep(
                    title = "Scrub Forward 2 Seconds",
                    description = "Swipe the timeline smoothly forward",
                    action = AutomationAction(
                        type = ActionType.SWIPE_TIMELINE,
                        startXRatio = 0.65f,
                        startYRatio = 0.65f,
                        endXRatio = 0.40f,
                        endYRatio = 0.65f,
                        durationMs = 300
                    ),
                    delayAfterMs = 500
                ),
                AutomationStep(
                    title = "Tap 'Split' Second Cut",
                    description = "Perform the second cut on the active clip",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Split"
                    ),
                    delayAfterMs = 600
                ),
                AutomationStep(
                    title = "Scrub Forward 2 Seconds",
                    description = "Advance the playhead for the third cut",
                    action = AutomationAction(
                        type = ActionType.SWIPE_TIMELINE,
                        startXRatio = 0.65f,
                        startYRatio = 0.65f,
                        endXRatio = 0.40f,
                        endYRatio = 0.65f,
                        durationMs = 300
                    ),
                    delayAfterMs = 500
                ),
                AutomationStep(
                    title = "Tap 'Split' Third Cut",
                    description = "Split the third segment",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Split"
                    ),
                    delayAfterMs = 500
                )
            )
        ),

        Workflow(
            id = "preset_auto_captions",
            title = "1-Tap Auto Captions Generator",
            description = "Navigates to Text, opens Auto captions, and triggers speech recognition transcription.",
            category = "Captions",
            iconKey = "subtitles",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Tap 'Text' Menu",
                    description = "Select the Text category in the bottom bar",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Text"
                    ),
                    delayAfterMs = 800
                ),
                AutomationStep(
                    title = "Tap 'Auto captions'",
                    description = "Open the AI speech-to-text captions wizard",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Auto captions"
                    ),
                    delayAfterMs = 1200
                ),
                AutomationStep(
                    title = "Click 'Generate' or 'Start'",
                    description = "Trigger the automated subtitle generation",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Generate"
                    ),
                    delayAfterMs = 2500
                )
            )
        ),

        Workflow(
            id = "preset_watermark_remover",
            title = "Auto Remove Ending Watermark",
            description = "Swipes to the end of the timeline, highlights CapCut's default ending outro logo, and deletes it.",
            category = "Cleanup",
            iconKey = "delete",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Fast Scrub to Timeline End",
                    description = "Swipe aggressively to scroll to the very end of the video timeline",
                    action = AutomationAction(
                        type = ActionType.SWIPE_TIMELINE,
                        startXRatio = 0.25f,
                        startYRatio = 0.65f,
                        endXRatio = 0.90f,
                        endYRatio = 0.65f,
                        durationMs = 450
                    ),
                    delayAfterMs = 800
                ),
                AutomationStep(
                    title = "Tap Ending Watermark Clip",
                    description = "Click on the outro logo segment at the far right",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.78f,
                        yRatio = 0.65f
                    ),
                    delayAfterMs = 600
                ),
                AutomationStep(
                    title = "Tap 'Delete'",
                    description = "Click the trash/delete action to purge the ending logo",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Delete"
                    ),
                    delayAfterMs = 500
                )
            )
        ),

        Workflow(
            id = "preset_format_9_16",
            title = "Convert to 9:16 Shorts/Reels",
            description = "Switches video canvas to vertical 9:16 ratio optimized for TikTok, Instagram Reels, and YouTube Shorts.",
            category = "Formatting",
            iconKey = "aspect_ratio",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Tap 'Ratio'",
                    description = "Open the aspect ratio selection bar",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Ratio"
                    ),
                    delayAfterMs = 800
                ),
                AutomationStep(
                    title = "Select '9:16'",
                    description = "Choose 9:16 vertical standard",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "9:16"
                    ),
                    delayAfterMs = 800
                ),
                AutomationStep(
                    title = "Confirm Aspect Ratio",
                    description = "Tap checkmark or done to apply format",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.92f,
                        yRatio = 0.93f
                    ),
                    delayAfterMs = 500
                )
            )
        ),

        Workflow(
            id = "preset_export_4k",
            title = "1-Tap 4K 60FPS Video Export",
            description = "Opens the resolution drop-down at top bar, selects 4K / 2K, 60fps frame rate, and triggers export.",
            category = "Export",
            iconKey = "cloud_upload",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Tap Resolution Settings",
                    description = "Click resolution selector at the top right of editor header",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.80f,
                        yRatio = 0.06f
                    ),
                    delayAfterMs = 700
                ),
                AutomationStep(
                    title = "Select '4K' / '2K'",
                    description = "Tap 4K / 2K resolution tick mark",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.88f,
                        yRatio = 0.16f
                    ),
                    delayAfterMs = 600
                ),
                AutomationStep(
                    title = "Select '60' FPS",
                    description = "Set frame rate slider to 60fps",
                    action = AutomationAction(
                        type = ActionType.CLICK_COORDINATES,
                        xRatio = 0.88f,
                        yRatio = 0.23f
                    ),
                    delayAfterMs = 600
                ),
                AutomationStep(
                    title = "Tap 'Export'",
                    description = "Click the export arrow or button at the top right",
                    action = AutomationAction(
                        type = ActionType.CLICK_TEXT,
                        targetText = "Export"
                    ),
                    delayAfterMs = 1000
                )
            )
        ),

        Workflow(
            id = "preset_zoom_timeline",
            title = "Pinch Zoom Timeline Detail",
            description = "Simulates two-finger spread gesture to zoom into timeline for frame-accurate cut inspections.",
            category = "Navigation",
            iconKey = "zoom_in",
            isPreset = true,
            steps = listOf(
                AutomationStep(
                    title = "Pinch Spread Zoom In",
                    description = "Dispatch two-finger outward gesture on timeline track",
                    action = AutomationAction(
                        type = ActionType.PINCH_ZOOM,
                        durationMs = 350
                    ),
                    delayAfterMs = 500
                )
            )
        )
    )
}
