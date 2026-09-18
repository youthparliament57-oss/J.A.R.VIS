package com.example.security.gatekeeper

import com.example.security.model.ApprovalRequest
import com.example.security.model.AuthorizationResult
import com.example.security.model.PermissionLevel
import java.util.UUID

interface SecurityGatekeeper {
    suspend fun evaluateAuthorization(
        stepId: String,
        toolId: String,
        toolName: String,
        permissionLevel: PermissionLevel,
        parameters: Map<String, Any>
    ): AuthorizationResult

    fun recordApproval(requestId: String)
    fun recordRejection(requestId: String)
}

class SecurityGatekeeperImpl : SecurityGatekeeper {
    private val approvedRequests = mutableSetOf<String>()

    override suspend fun evaluateAuthorization(
        stepId: String,
        toolId: String,
        toolName: String,
        permissionLevel: PermissionLevel,
        parameters: Map<String, Any>
    ): AuthorizationResult {
        // If already approved in active session
        if (approvedRequests.contains(stepId)) {
            return AuthorizationResult.Granted
        }

        return if (permissionLevel.requiresExplicitApproval) {
            val req = ApprovalRequest(
                requestId = UUID.randomUUID().toString(),
                stepId = stepId,
                toolId = toolId,
                toolName = toolName,
                permissionLevel = permissionLevel,
                summary = "Execute '$toolName' with parameters: $parameters",
                parameters = parameters,
                riskWarning = when (permissionLevel) {
                    PermissionLevel.L3_SENSITIVE_DATA -> "Modifies persistent records or user memory. Requires human confirmation."
                    PermissionLevel.L4_SYSTEM_DESTRUCTIVE -> "System-level administrative command. Potential irreversible side effects."
                    else -> "Operation requires elevated user consent."
                }
            )
            AuthorizationResult.RequiresApproval(req)
        } else {
            AuthorizationResult.Granted
        }
    }

    override fun recordApproval(requestId: String) {
        approvedRequests.add(requestId)
    }

    override fun recordRejection(requestId: String) {
        approvedRequests.remove(requestId)
    }
}
