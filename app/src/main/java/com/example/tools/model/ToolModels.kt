package com.example.tools.model

import com.example.security.model.PermissionLevel

data class ToolParameter(
    val name: String,
    val description: String,
    val type: String, // "string", "number", "boolean"
    val required: Boolean = true
)

data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val permissionLevel: PermissionLevel,
    val parameters: List<ToolParameter> = emptyList()
)

enum class VerificationStatus {
    VERIFIED_VALID,
    FAILED_VERIFICATION,
    UNVERIFIABLE
}

data class ToolExecutionResult(
    val success: Boolean,
    val output: String,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED_VALID,
    val verificationEvidence: String? = null,
    val error: String? = null
)
