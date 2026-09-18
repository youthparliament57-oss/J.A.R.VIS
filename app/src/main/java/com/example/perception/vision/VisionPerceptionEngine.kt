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

    suspend fun analyzeFrame(bitmap: Bitmap, prompt: String? = null): Result<VisionAnalysisResult>
}
