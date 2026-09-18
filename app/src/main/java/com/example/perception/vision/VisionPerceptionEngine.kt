package com.example.perception.vision

import android.graphics.Bitmap
import kotlinx.coroutines.flow.StateFlow

data class VisionAnalysisResult(
    val summary: String,
    val detectedObjects: List<String> = emptyList(),
    val extractedText: String? = null,
    val visualRiskAssessment: String = "L0_SAFE",
    val latencyMs: Long = 0L
)

interface VisionPerceptionEngine {
    val isAnalyzing: StateFlow<Boolean>
    val lastAnalysis: StateFlow<VisionAnalysisResult?>
    val lastCapturedFrame: StateFlow<Bitmap?>

    fun updateCurrentFrame(bitmap: Bitmap)
    suspend fun analyzeFrame(bitmap: Bitmap? = null, prompt: String? = null): Result<VisionAnalysisResult>
}
