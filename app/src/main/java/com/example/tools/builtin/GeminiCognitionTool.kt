package com.example.tools.builtin

import com.example.cognition.llm.GeminiNeuralEngine
import com.example.security.model.PermissionLevel
import com.example.tools.contract.NousTool
import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.VerificationStatus

class GeminiCognitionTool(
    private val geminiEngine: GeminiNeuralEngine
) : NousTool {

    override val definition = ToolDefinition(
        id = "gemini_cognition_tool",
        name = "Gemini Neural Reasoning",
        description = "Invokes Gemini LLM for deep natural language understanding, question answering, creative analysis, and complex reasoning.",
        permissionLevel = PermissionLevel.L0_READ_ONLY
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        val query = parameters["query"]?.toString() ?: ""
        if (query.isBlank()) {
            return ToolExecutionResult(
                success = false,
                output = "Empty query passed to Gemini Neural Cognition Tool.",
                verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                verificationEvidence = "Parameter 'query' was blank"
            )
        }

        val result = geminiEngine.queryIntelligence(prompt = query)
        return if (result.isSuccess) {
            val responseText = result.getOrNull() ?: "Directive processed."
            ToolExecutionResult(
                success = true,
                output = responseText,
                verificationStatus = VerificationStatus.VERIFIED_VALID,
                verificationEvidence = "Gemini API successfully answered query."
            )
        } else {
            val errorMsg = result.exceptionOrNull()?.localizedMessage ?: "Gemini query failed."
            ToolExecutionResult(
                success = false,
                output = "Gemini Neural Engine Notice: $errorMsg",
                verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                verificationEvidence = errorMsg
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
