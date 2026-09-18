package com.example.security.model

enum class PermissionLevel(val code: Int, val title: String, val requiresExplicitApproval: Boolean) {
    L0_READ_ONLY(0, "L0: Read-Only Sensor / Telemetry", false),
    L1_LOW_RISK(1, "L1: Local Low-Risk Operation", false),
    L2_EXTERNAL_COMM(2, "L2: External Network Query", false),
    L3_SENSITIVE_DATA(3, "L3: Sensitive Record Modification", true),
    L4_SYSTEM_DESTRUCTIVE(4, "L4: System / Administrative Action", true)
}

data class ApprovalRequest(
    val requestId: String,
    val stepId: String,
    val toolId: String,
    val toolName: String,
    val permissionLevel: PermissionLevel,
    val summary: String,
    val parameters: Map<String, Any>,
    val riskWarning: String
)

sealed class AuthorizationResult {
    object Granted : AuthorizationResult()
    data class RequiresApproval(val request: ApprovalRequest) : AuthorizationResult()
    data class Denied(val reason: String) : AuthorizationResult()
}
