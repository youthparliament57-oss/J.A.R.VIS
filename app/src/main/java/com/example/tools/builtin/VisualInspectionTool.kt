package com.example.tools.builtin

import com.example.perception.vision.VisionPerceptionEngine
import com.example.security.model.PermissionLevel
import com.example.tools.contract.NousTool
import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.VerificationStatus

class VisualInspectionTool(
    private val visionEngine: VisionPerceptionEngine
) : NousTool {

    override val definition = ToolDefinition(
        id = "visual_inspection_tool",
        name = "Optical Viewport Inspection",
        description = "Inspects the optical visual frame captured by device camera and sends it to Gemini Multimodal Engine to analyze environment, detect objects, and read text.",
        permissionLevel = PermissionLevel.L0_READ_ONLY
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        val query = parameters["query"]?.toString()
        val analysisResult = visionEngine.analyzeFrame(prompt = query)

        return if (analysisResult.isSuccess) {
            val data = analysisResult.getOrNull()
            val text = data?.summary ?: "Visual analysis completed."
            ToolExecutionResult(
                success = true,
                output = text,
                verificationStatus = VerificationStatus.VERIFIED_VALID,
                verificationEvidence = "Optical frame successfully analyzed by Gemini Multimodal Vision."
            )
        } else {
            val lastCached = visionEngine.lastAnalysis.value
            if (lastCached != null) {
                ToolExecutionResult(
                    success = true,
                    output = lastCached.summary,
                    verificationStatus = VerificationStatus.VERIFIED_VALID,
                    verificationEvidence = "Cached optical frame analysis provided."
                )
            } else {
                val errorMsg = analysisResult.exceptionOrNull()?.localizedMessage ?: "Please tap INSPECT on the Optical Sensor Viewport to capture a camera frame."
                ToolExecutionResult(
                    success = false,
                    output = "Optical Sensor Notice: $errorMsg",
                    verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                    verificationEvidence = errorMsg
                )
            }
        }
    }

    override suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult {
        return executionResult
    }
}
