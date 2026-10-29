package com.ai.capcutagent.voice

sealed class VoiceAction {
    data class RunPreset(val presetId: String, val hindiResponse: String) : VoiceAction()
    data class GenerateCustom(val prompt: String, val hindiResponse: String) : VoiceAction()
    data class ScrollTimeline(val forward: Boolean, val hindiResponse: String) : VoiceAction()
    data class StopExecution(val hindiResponse: String) : VoiceAction()
    data class Unrecognized(val rawQuery: String, val hindiResponse: String) : VoiceAction()
}

object VoiceCommandParser {

    fun parse(rawText: String): VoiceAction {
        val text = rawText.lowercase().trim()

        // 1. Split / Cut commands
        if (text.contains("कट") || text.contains("स्प्लिट") || text.contains("काटो") ||
            text.contains("cut") || text.contains("split") || text.contains("tukde")
        ) {
            return VoiceAction.RunPreset(
                presetId = "preset_auto_split",
                hindiResponse = "वीडियो में कट लगा दिया गया है।"
            )
        }

        // 2. Auto Captions / Subtitles
        if (text.contains("कैप्शन") || text.contains("सबटाइटल") || text.contains("लिखो") ||
            text.contains("caption") || text.contains("subtitle") || text.contains("text lagao")
        ) {
            return VoiceAction.RunPreset(
                presetId = "preset_auto_captions",
                hindiResponse = "ऑटो कैप्शन जनरेटर शुरू कर दिया गया है।"
            )
        }

        // 3. Ending Watermark Remover
        if (text.contains("वाटरमार्क") || text.contains("लोगो") || text.contains("एंडिंग") ||
            text.contains("watermark") || text.contains("outro") || text.contains("ending delete")
        ) {
            return VoiceAction.RunPreset(
                presetId = "preset_watermark_remover",
                hindiResponse = "CapCut का एंडिंग वाटरमार्क हटा दिया गया है।"
            )
        }

        // 4. Aspect Ratio 9:16 (Shorts / Reels)
        if (text.contains("शॉर्ट्स") || text.contains("रील्स") || text.contains("9:16") || text.contains("9 16") ||
            text.contains("shorts") || text.contains("reels") || text.contains("ratio") || text.contains("format")
        ) {
            return VoiceAction.RunPreset(
                presetId = "preset_format_9_16",
                hindiResponse = "वीडियो का रेश्यो 9:16 वर्टिकल में बदल दिया गया है।"
            )
        }

        // 5. 4K Export
        if (text.contains("एक्सपोर्ट") || text.contains("सेव") || text.contains("4k") ||
            text.contains("export") || text.contains("save") || text.contains("render")
        ) {
            return VoiceAction.RunPreset(
                presetId = "preset_export_4k",
                hindiResponse = "4K 60FPS में वीडियो एक्सपोर्ट शुरू हो रहा है।"
            )
        }

        // 6. Stop / Pause
        if (text.contains("रुक") || text.contains("स्टॉप") || text.contains("बंद") ||
            text.contains("stop") || text.contains("pause") || text.contains("halt")
        ) {
            return VoiceAction.StopExecution(
                hindiResponse = "ऑटोमेशन रोक दिया गया है।"
            )
        }

        // 7. Timeline Scrubbing
        if (text.contains("आगे") || text.contains("forward") || text.contains("scroll")) {
            return VoiceAction.ScrollTimeline(
                forward = true,
                hindiResponse = "टाइमलाइन आगे बढ़ा दी गई है।"
            )
        }

        // 8. Custom AI prompt fallback
        return VoiceAction.GenerateCustom(
            prompt = rawText,
            hindiResponse = "आपके निर्देश '$rawText' के अनुसार नया एडिटिंग वर्कफ़्लो तैयार किया जा रहा है।"
        )
    }
}
