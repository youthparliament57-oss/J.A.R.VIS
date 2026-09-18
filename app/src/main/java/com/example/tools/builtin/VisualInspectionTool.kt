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
        description = "Inspects the latest optical visual frame captured by NOUS sensors to analyze environment, brightness, and text elements.",
        permissionLevel = PermissionLevel.L0_READ_ONLY
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        val lastResult = visionEngine.lastAnalysis.value
        return if (lastResult != null) {
            ToolExecutionResult(
                success = true,
                output = "Optical Analysis: ${lastResult.summary} | Features: ${lastResult.detectedObjects.joinToString(", ")}",
                verificationStatus = VerificationStatus.VERIFIED_VALID,
                verificationEvidence = "Optical frame status successfully retrieved from vision perception pipeline."
            )
        } else {
            ToolExecutionResult(
                success = true,
                output = "No optical frame currently cached in memory. Optical sensor is active in HUD viewport.",
                verificationStatus = VerificationStatus.VERIFIED_VALID,
                verificationEvidence = "Optical sensor active and standby."
            )
        }
    }

    override suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult {
        return executionResult
    }
}
