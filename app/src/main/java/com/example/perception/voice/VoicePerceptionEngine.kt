package com.example.perception.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.cognition.llm.GeminiNeuralEngine
import com.example.core.event.EventBus
import com.example.core.event.NousSystemEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.Locale

enum class VoiceEngineStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

interface VoicePerceptionEngine {
    val engineStatus: StateFlow<VoiceEngineStatus>
    val lastRecognizedSpeech: StateFlow<String>
    val isTtsActive: StateFlow<Boolean>

    fun startListening(onResult: (String) -> Unit)
    fun stopListening()
    fun speak(text: String, onComplete: (() -> Unit)? = null)
    fun stopSpeaking()
    fun shutdown()
}

class AndroidVoicePerceptionEngine(
    private val context: Context,
    private val eventBus: EventBus,
    private val coroutineScope: CoroutineScope,
    private val geminiNeuralEngine: GeminiNeuralEngine? = null
) : VoicePerceptionEngine {

    private val _engineStatus = MutableStateFlow(VoiceEngineStatus.IDLE)
    override val engineStatus: StateFlow<VoiceEngineStatus> = _engineStatus.asStateFlow()

    private val _lastRecognizedSpeech = MutableStateFlow("")
    override val lastRecognizedSpeech: StateFlow<String> = _lastRecognizedSpeech.asStateFlow()

    private val _isTtsActive = MutableStateFlow(false)
    override val isTtsActive: StateFlow<Boolean> = _isTtsActive.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false
    private var pendingSpeechCompletion: (() -> Unit)? = null
    private var activeSpeechCallback: ((String) -> Unit)? = null

    private var activeAudioTrack: AudioTrack? = null
    private var neuralPlaybackJob: Job? = null

    init {
        initTts()
    }

    private fun initTts() {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.let { tts ->
                    val result = tts.setLanguage(Locale.US)
                    tts.setPitch(1.05f)
                    tts.setSpeechRate(1.05f)
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isTtsActive.value = true
                            _engineStatus.value = VoiceEngineStatus.SPEAKING
                        }

                        override fun onDone(utteranceId: String?) {
                            _isTtsActive.value = false
                            _engineStatus.value = VoiceEngineStatus.IDLE
                            pendingSpeechCompletion?.invoke()
                            pendingSpeechCompletion = null
                        }

                        override fun onError(utteranceId: String?) {
                            _isTtsActive.value = false
                            _engineStatus.value = VoiceEngineStatus.IDLE
                            pendingSpeechCompletion = null
                        }
                    })
                    isTtsInitialized = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
                    if (isTtsInitialized) {
                        coroutineScope.launch {
                            eventBus.emit(NousSystemEvent.LogEmitted(
                                tag = "VOICE",
                                message = "Standard Android TTS initialized as fallback."
                            ))
                        }
                    }
                }
            } else {
                _engineStatus.value = VoiceEngineStatus.ERROR
            }
        }
    }

    override fun startListening(onResult: (String) -> Unit) {
        stopSpeaking() // Barge-in: interrupt speech if user begins listening
        activeSpeechCallback = onResult

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _engineStatus.value = VoiceEngineStatus.ERROR
            coroutineScope.launch {
                eventBus.emit(NousSystemEvent.LogEmitted(
                    tag = "VOICE-ERR",
                    message = "SpeechRecognizer unavailable on hardware.",
                    level = "WARN"
                ))
            }
            return
        }

        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _engineStatus.value = VoiceEngineStatus.LISTENING
                            coroutineScope.launch {
                                eventBus.emit(NousSystemEvent.LogEmitted(
                                    tag = "VOICE",
                                    message = "Audio input buffer listening for user directive..."
                                ))
                            }
                        }

                        override fun onBeginningOfSpeech() {
                            _engineStatus.value = VoiceEngineStatus.LISTENING
                        }

                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            _engineStatus.value = VoiceEngineStatus.PROCESSING
                        }

                        override fun onError(error: Int) {
                            _engineStatus.value = VoiceEngineStatus.IDLE
                            val errorMsg = when (error) {
                                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized."
                                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Record Audio permission missing."
                                else -> "Speech recognition error code: $error"
                            }
                            coroutineScope.launch {
                                eventBus.emit(NousSystemEvent.LogEmitted(
                                    tag = "VOICE-WARN",
                                    message = errorMsg,
                                    level = "WARN"
                                ))
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _engineStatus.value = VoiceEngineStatus.IDLE
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognizedText = matches?.firstOrNull() ?: ""
                            if (recognizedText.isNotBlank()) {
                                _lastRecognizedSpeech.value = recognizedText
                                coroutineScope.launch {
                                    eventBus.emit(NousSystemEvent.LogEmitted(
                                        tag = "VOICE-HEARD",
                                        message = "\"$recognizedText\""
                                    ))
                                }
                                activeSpeechCallback?.invoke(recognizedText)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            matches?.firstOrNull()?.let {
                                _lastRecognizedSpeech.value = it
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                _engineStatus.value = VoiceEngineStatus.ERROR
                coroutineScope.launch {
                    eventBus.emit(NousSystemEvent.LogEmitted(
                        tag = "VOICE-EX",
                        message = e.localizedMessage ?: "Unknown speech error",
                        level = "ERROR"
                    ))
                }
            }
        }
    }

    override fun stopListening() {
        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer?.stopListening()
                _engineStatus.value = VoiceEngineStatus.IDLE
            } catch (e: Exception) {
                // Ignore safe cleanup
            }
        }
    }

    override fun speak(text: String, onComplete: (() -> Unit)?) {
        if (text.isBlank()) {
            onComplete?.invoke()
            return
        }

        stopSpeaking()
        pendingSpeechCompletion = onComplete

        // If Gemini Neural Engine is available, attempt Neural Voice synthesis first
        if (geminiNeuralEngine != null) {
            neuralPlaybackJob = coroutineScope.launch {
                _engineStatus.value = VoiceEngineStatus.PROCESSING
                eventBus.emit(NousSystemEvent.LogEmitted(
                    tag = "NEURAL-VOICE",
                    message = "Synthesizing Gemini Neural Vocal stream (Voice: Kore)..."
                ))

                val neuralResult = geminiNeuralEngine.synthesizeNeuralVoice(textToSpeak = text, voiceName = "Kore")
                if (neuralResult.isSuccess) {
                    val audioBytes = neuralResult.getOrNull()
                    if (audioBytes != null && audioBytes.isNotEmpty()) {
                        playRawAudio(audioBytes)
                        return@launch
                    }
                }

                // If neural synthesis is unavailable or fails, gracefully fallback to local TTS
                eventBus.emit(NousSystemEvent.LogEmitted(
                    tag = "NEURAL-VOICE",
                    message = "Fallback to high-pitch system TTS."
                ))
                withContext(Dispatchers.Main) {
                    playLocalTts(text)
                }
            }
        } else {
            playLocalTts(text)
        }
    }

    private fun playLocalTts(text: String) {
        if (!isTtsInitialized) {
            pendingSpeechCompletion?.invoke()
            pendingSpeechCompletion = null
            return
        }
        val utteranceId = "NOUS_VOICE_${System.currentTimeMillis()}"
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private suspend fun playRawAudio(audioBytes: ByteArray) = withContext(Dispatchers.IO) {
        try {
            _isTtsActive.value = true
            _engineStatus.value = VoiceEngineStatus.SPEAKING

            // Gemini audio response returns PCM 24000Hz or 16000Hz audio
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = maxOf(minBufferSize, audioBytes.size)

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            activeAudioTrack = track
            track.play()

            val inputStream = ByteArrayInputStream(audioBytes)
            val buffer = ByteArray(4096)
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                if (!_isTtsActive.value) break // Stop if user interrupted / barge-in
                track.write(buffer, 0, bytesRead)
            }

            try {
                track.stop()
            } catch (e: Exception) {}
            track.release()
            activeAudioTrack = null
        } catch (e: Exception) {
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "AUDIO-ERR",
                message = "AudioTrack playback error: ${e.message}",
                level = "WARN"
            ))
        } finally {
            _isTtsActive.value = false
            _engineStatus.value = VoiceEngineStatus.IDLE
            pendingSpeechCompletion?.invoke()
            pendingSpeechCompletion = null
        }
    }

    override fun stopSpeaking() {
        neuralPlaybackJob?.cancel()
        neuralPlaybackJob = null

        try {
            activeAudioTrack?.stop()
            activeAudioTrack?.release()
        } catch (e: Exception) {}
        activeAudioTrack = null

        if (_isTtsActive.value) {
            textToSpeech?.stop()
            _isTtsActive.value = false
            _engineStatus.value = VoiceEngineStatus.IDLE
            pendingSpeechCompletion?.invoke()
            pendingSpeechCompletion = null
        }
    }

    override fun shutdown() {
        stopSpeaking()
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.shutdown()
        textToSpeech = null
    }
}
