package com.example.perception.vision

import android.graphics.Bitmap
import android.graphics.Color
import com.example.cognition.llm.GeminiNeuralEngine
import com.example.core.event.EventBus
import com.example.core.event.NousSystemEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class LocalVisionEngine(
    private val eventBus: EventBus,
    private val geminiNeuralEngine: GeminiNeuralEngine? = null
) : VisionPerceptionEngine {

    private val _isAnalyzing = MutableStateFlow(false)
    override val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _lastAnalysis = MutableStateFlow<VisionAnalysisResult?>(null)
    override val lastAnalysis: StateFlow<VisionAnalysisResult?> = _lastAnalysis.asStateFlow()

    override suspend fun analyzeFrame(bitmap: Bitmap, prompt: String?): Result<VisionAnalysisResult> = withContext(Dispatchers.Default) {
        _isAnalyzing.value = true
        val startTime = System.currentTimeMillis()

        try {
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "VISION",
                message = "Sampling optical frame ${bitmap.width}x${bitmap.height}..."
            ))

            // If prompt is specified or Gemini Neural Engine is available, run Multimodal Gemini Vision
            if (geminiNeuralEngine != null && !prompt.isNullOrBlank()) {
                val visionPrompt = "Analyze this camera frame in detail. $prompt"
                val geminiResult = geminiNeuralEngine.queryIntelligence(
                    prompt = visionPrompt,
                    systemInstruction = "You are NOUS Vision Engine. Identify objects, read any visible text, detect spatial anomalies, and describe the environment concisely.",
                    imageBitmap = bitmap
                )

                if (geminiResult.isSuccess) {
                    val analysisText = geminiResult.getOrNull() ?: "Visual field inspected."
                    val latency = System.currentTimeMillis() - startTime
                    val result = VisionAnalysisResult(
                        summary = analysisText,
                        detectedObjects = listOf("Multimodal Neural Scan", "Environment Objects"),
                        extractedText = null,
                        visualRiskAssessment = "L0_SAFE",
                        latencyMs = latency
                    )
                    _lastAnalysis.value = result
                    return@withContext Result.success(result)
                }
            }

            // High-speed Deterministic Optical Fallback
            val width = bitmap.width
            val height = bitmap.height
            val step = maxOf(1, width / 20)
            var totalLuminance = 0L
            var sampleCount = 0
            var highContrastPixels = 0

            var prevLum = -1
            for (y in 0 until height step step) {
                for (x in 0 until width step step) {
                    val pixel = bitmap.getPixel(x, y)
                    val r = Color.red(pixel)
                    val g = Color.green(pixel)
                    val b = Color.blue(pixel)
                    val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                    totalLuminance += lum
                    sampleCount++

                    if (prevLum != -1 && kotlin.math.abs(lum - prevLum) > 60) {
                        highContrastPixels++
                    }
                    prevLum = lum
                }
            }

            val avgLum = if (sampleCount > 0) (totalLuminance / sampleCount).toInt() else 128
            val contrastRatio = if (sampleCount > 0) (highContrastPixels.toFloat() / sampleCount.toFloat()) else 0f

            val detectedObjects = mutableListOf<String>()
            val illumination = when {
                avgLum > 190 -> {
                    detectedObjects.add("High-illumination environment")
                    "High brightness light field"
                }
                avgLum < 45 -> {
                    detectedObjects.add("Low-light ambient field")
                    "Dim / Low-light ambient"
                }
                else -> {
                    detectedObjects.add("Normal indoor spectrum")
                    "Balanced indoor lighting"
                }
            }

            if (contrastRatio > 0.35f) {
                detectedObjects.add("Structured edges / potential text elements")
            } else {
                detectedObjects.add("Homogeneous surface")
            }

            val latency = System.currentTimeMillis() - startTime
            val summary = "Visual inspection: $illumination, resolution ${width}x${height}, contrast index ${(contrastRatio * 100).toInt()}%."

            val result = VisionAnalysisResult(
                summary = summary,
                detectedObjects = detectedObjects,
                extractedText = if (contrastRatio > 0.35f) "High-contrast edge patterns detected in visual plane." else null,
                visualRiskAssessment = "L0_SAFE",
                latencyMs = latency
            )

            _lastAnalysis.value = result
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "VISION",
                message = "$summary (Latency: ${latency}ms)"
            ))

            Result.success(result)
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Visual frame processing failed"
            eventBus.emit(NousSystemEvent.LogEmitted(
                tag = "VISION-ERR",
                message = errorMsg,
                level = "ERROR"
            ))
            Result.failure(e)
        } finally {
            _isAnalyzing.value = false
        }
    }
}
