package com.example.cognition.llm

import android.graphics.Bitmap
import android.util.Base64
import com.example.core.config.ApiKeyPreferences
import com.example.core.event.EventBus
import com.example.core.event.NousSystemEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class GeminiNeuralEngine(
    private val apiKeyPreferences: ApiKeyPreferences,
    private val eventBus: EventBus
) {

    suspend fun queryIntelligence(
        prompt: String,
        systemInstruction: String = "You are NOUS (Neural Operating Universal System), an advanced autonomous AI operating intelligence inspired by JARVIS. Speak concisely, authoritatively, and strategically.",
        imageBitmap: Bitmap? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyPreferences.getStoredApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API Key provided. Please enter your Gemini API Key in the NOUS HUD settings dialog."))
        }

        try {
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "GEMINI-COGNITION",
                message = "Routing prompt to Gemini Neural Engine..."
            ))

            val parts = mutableListOf<GeminiPart>()
            parts.add(GeminiPart(text = prompt))

            if (imageBitmap != null) {
                val outputStream = ByteArrayOutputStream()
                imageBitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
                val base64Data = Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
                parts.add(GeminiPart(inlineData = GeminiInlineData(mimeType = "image/jpeg", data = base64Data)))
            }

            val request = GeminiRequest(
                contents = listOf(GeminiContent(parts = parts)),
                systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemInstruction))),
                generationConfig = GeminiGenerationConfig(
                    temperature = 0.4f,
                    topP = 0.9f
                )
            )

            val response = GeminiNetworkClient.service.generateContent(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val text = candidate?.content?.parts?.firstOrNull()?.text

            if (text != null && text.isNotBlank()) {
                eventBus.emit(NousSystemEvent.LogEmitted(
                    tag = "GEMINI-OK",
                    message = "Cognitive response received (${text.length} chars)"
                ))
                Result.success(text.trim())
            } else {
                Result.failure(IllegalStateException("Empty response returned from Gemini."))
            }
        } catch (e: Throwable) {
            val err = e.localizedMessage ?: "Gemini neural query failed"
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "GEMINI-ERR",
                message = err,
                level = "ERROR"
            ))
            Result.failure(e)
        }
    }

    /**
     * Synthesizes neural audio using Gemini's native audio modality with prebuilt voice (e.g. Kore or Puck).
     * Returns Base64-encoded audio or byte array.
     */
    suspend fun synthesizeNeuralVoice(
        textToSpeak: String,
        voiceName: String = "Kore"
    ): Result<ByteArray> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyPreferences.getStoredApiKey()
        if (apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("No Gemini API key available for neural voice."))
        }

        try {
            val request = GeminiRequest(
                contents = listOf(
                    GeminiContent(parts = listOf(GeminiPart(text = textToSpeak)))
                ),
                generationConfig = GeminiGenerationConfig(
                    responseModalities = listOf("AUDIO"),
                    speechConfig = GeminiSpeechConfig(
                        voiceConfig = GeminiVoiceConfig(
                            prebuiltVoiceConfig = GeminiPrebuiltVoiceConfig(voiceName = voiceName)
                        )
                    )
                )
            )

            val response = GeminiNetworkClient.service.generateSpeech(apiKey, request)
            val candidate = response.candidates?.firstOrNull()
            val audioPart = candidate?.content?.parts?.firstOrNull { it.inlineData != null }
            val base64Audio = audioPart?.inlineData?.data

            if (base64Audio != null) {
                val decodedBytes = Base64.decode(base64Audio, Base64.DEFAULT)
                Result.success(decodedBytes)
            } else {
                Result.failure(IllegalStateException("No audio stream received in Gemini response."))
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
