package com.ai.capcutagent.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.ai.capcutagent.automation.CapCutAutomationService
import com.ai.capcutagent.automation.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class HindiVoiceAgent(
    private val context: Context,
    private val onCommandReceived: (String) -> Unit
) : TextToSpeech.OnInitListener, RecognitionListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var recognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _lastRecognized = MutableStateFlow<String?>(null)
    val lastRecognized: StateFlow<String?> = _lastRecognized.asStateFlow()

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("hi", "IN"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Fallback to English (India) or default if Hindi voice data not preinstalled
                tts?.setLanguage(Locale("en", "IN"))
            }
            _isTtsReady.value = true
            CapCutAutomationService.log(LogLevel.SUCCESS, "VoiceAgent", "Hindi TTS engine initialized successfully")
        } else {
            CapCutAutomationService.log(LogLevel.WARNING, "VoiceAgent", "TTS initialization returned status $status")
        }
    }

    fun speak(text: String) {
        CapCutAutomationService.log(LogLevel.ACTION, "VoiceAgent", "Speaking (TTS): '$text'")
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "UTTERANCE_ID")
    }

    fun startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                recognizer?.destroy()
                recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@HindiVoiceAgent)
                }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, "hi-IN")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }
                _isListening.value = true
                recognizer?.startListening(intent)
                CapCutAutomationService.log(LogLevel.INFO, "VoiceAgent", "Speech recognition started (Listening in Hindi/Hinglish)...")
            } catch (e: Exception) {
                _isListening.value = false
                CapCutAutomationService.log(LogLevel.ERROR, "VoiceAgent", "Failed to start listening: ${e.message}")
            }
        } else {
            CapCutAutomationService.log(LogLevel.WARNING, "VoiceAgent", "SpeechRecognizer is not available on this device")
        }
    }

    fun stopListening() {
        try {
            recognizer?.stopListening()
            _isListening.value = false
        } catch (_: Exception) {}
    }

    override fun onResults(results: Bundle?) {
        _isListening.value = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val text = matches?.firstOrNull()
        if (!text.isNullOrBlank()) {
            _lastRecognized.value = text
            CapCutAutomationService.log(LogLevel.SUCCESS, "VoiceAgent", "Recognized voice command: '$text'")
            onCommandReceived(text)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()
        if (!partial.isNullOrBlank()) {
            _lastRecognized.value = partial
        }
    }

    override fun onRmsChanged(rmsdB: Float) {
        _rmsLevel.value = rmsdB
    }

    override fun onError(error: Int) {
        _isListening.value = false
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing RECORD_AUDIO permission"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            else -> "Speech recognition error code: $error"
        }
        CapCutAutomationService.log(LogLevel.WARNING, "VoiceAgent", errorMessage)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        _isListening.value = true
    }

    override fun onBeginningOfSpeech() {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _isListening.value = false
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        recognizer?.destroy()
        recognizer = null
        _isListening.value = false
        CapCutAutomationService.log(LogLevel.INFO, "VoiceAgent", "HindiVoiceAgent shut down")
    }
}
