package com.example.tools.builtin

import com.example.core.model.EpistemicConfidence
import com.example.memory.repository.MemoryRepository
import com.example.security.model.PermissionLevel
import com.example.tools.contract.NousTool
import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.ToolParameter
import com.example.tools.model.VerificationStatus

class NotesMemoryTool(private val memoryRepository: MemoryRepository) : NousTool {
    override val definition = ToolDefinition(
        id = "notes_memory",
        name = "Semantic Fact & Notes Storage",
        description = "Persists or modifies user facts, notes, and preferences in Room storage. Classified as L3 Sensitive Data requiring confirmation for writes.",
        permissionLevel = PermissionLevel.L3_SENSITIVE_DATA,
        parameters = listOf(
            ToolParameter(name = "action", description = "Operation type: 'SAVE' or 'DELETE'", type = "string", required = true),
            ToolParameter(name = "key", description = "Unique identifier or subject key for the fact", type = "string", required = true),
            ToolParameter(name = "value", description = "Content of the fact/note (required for SAVE)", type = "string", required = false),
            ToolParameter(name = "category", description = "Category, e.g. 'USER_PREFERENCE', 'PERSONAL', 'TASK'", type = "string", required = false)
        )
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        val action = parameters["action"]?.toString()?.uppercase() ?: "SAVE"
        val key = parameters["key"]?.toString()
            ?: return ToolExecutionResult(false, "Parameter 'key' is required", VerificationStatus.FAILED_VERIFICATION)

        return when (action) {
            "SAVE" -> {
                val value = parameters["value"]?.toString() ?: ""
                val category = parameters["category"]?.toString() ?: "GENERAL"
                memoryRepository.storeFact(
                    key = key,
                    value = value,
                    category = category,
                    confidence = EpistemicConfidence.KNOWN,
                    source = "USER_REQUEST"
                )
                ToolExecutionResult(
                    success = true,
                    output = "Saved semantic fact '$key' under category '$category'.",
                    verificationStatus = VerificationStatus.VERIFIED_VALID,
                    verificationEvidence = "Fact '$key' confirmed written to Room database"
                )
            }
            "DELETE" -> {
                memoryRepository.deleteFact(key)
                ToolExecutionResult(
                    success = true,
                    output = "Deleted semantic fact '$key' from persistent storage.",
                    verificationStatus = VerificationStatus.VERIFIED_VALID,
                    verificationEvidence = "Fact '$key' deletion confirmed from Room database"
                )
            }
            else -> {
                ToolExecutionResult(
                    success = false,
                    output = "Unknown action '$action'. Expected 'SAVE' or 'DELETE'",
                    verificationStatus = VerificationStatus.FAILED_VERIFICATION
                )
            }
        }
    }

    override suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult {
        val key = parameters["key"]?.toString() ?: return executionResult
        val action = parameters["action"]?.toString()?.uppercase() ?: "SAVE"
        if (action == "SAVE") {
            val queried = memoryRepository.getFact(key)
            return if (queried != null) {
                executionResult.copy(
                    verificationStatus = VerificationStatus.VERIFIED_VALID,
                    verificationEvidence = "Queried database: factKey=${queried.factKey}, value=${queried.factValue}"
                )
            } else {
                executionResult.copy(
                    success = false,
                    verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                    error = "Database post-check failed: fact '$key' was not found"
                )
            }
        }
        return executionResult
    }
}
