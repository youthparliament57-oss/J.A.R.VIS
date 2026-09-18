package com.example.tools.executor

import com.example.core.event.EventBus
import com.example.core.event.NousSystemEvent
import com.example.security.gatekeeper.SecurityGatekeeper
import com.example.security.model.AuthorizationResult
import com.example.tools.contract.ToolRegistry
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.VerificationStatus

sealed class ToolInvocationResult {
    data class Completed(val result: ToolExecutionResult) : ToolInvocationResult()
    data class AwaitingApproval(val request: com.example.security.model.ApprovalRequest) : ToolInvocationResult()
    data class Blocked(val reason: String) : ToolInvocationResult()
}

interface ToolExecutor {
    suspend fun executeTool(
        stepId: String,
        toolId: String,
        parameters: Map<String, Any>
    ): ToolInvocationResult
}

class ToolExecutorImpl(
    private val toolRegistry: ToolRegistry,
    private val securityGatekeeper: SecurityGatekeeper,
    private val eventBus: EventBus
) : ToolExecutor {

    override suspend fun executeTool(
        stepId: String,
        toolId: String,
        parameters: Map<String, Any>
    ): ToolInvocationResult {
        val tool = toolRegistry.getTool(toolId)
            ?: return ToolInvocationResult.Blocked("Tool '$toolId' is not registered in NOUS Tool Gateway.")

        // 1. Evaluate Security Gatekeeper (Risk Assessment)
        val authDecision = securityGatekeeper.evaluateAuthorization(
            stepId = stepId,
            toolId = toolId,
            toolName = tool.definition.name,
            permissionLevel = tool.definition.permissionLevel,
            parameters = parameters
        )

        when (authDecision) {
            is AuthorizationResult.RequiresApproval -> {
                eventBus.emit(
                    NousSystemEvent.SecurityAlert(
                        message = "Tool '${tool.definition.name}' (${tool.definition.permissionLevel}) paused for explicit human authorization.",
                        severity = "WARN"
                    )
                )
                return ToolInvocationResult.AwaitingApproval(authDecision.request)
            }
            is AuthorizationResult.Denied -> {
                eventBus.emit(
                    NousSystemEvent.SecurityAlert(
                        message = "Security Gatekeeper DENIED action for '$toolId': ${authDecision.reason}",
                        severity = "ERROR"
                    )
                )
                return ToolInvocationResult.Blocked(authDecision.reason)
            }
            is AuthorizationResult.Granted -> {
                // Proceed to execution
            }
        }

        // 2. Sandboxed Execution
        val startTime = System.currentTimeMillis()
        val rawResult = try {
            tool.execute(parameters)
        } catch (t: Throwable) {
            ToolExecutionResult(
                success = false,
                output = "Execution threw unhandled exception: ${t.message}",
                verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                error = t.localizedMessage
            )
        }

        // 3. Post-Condition Verification
        val finalVerifiedResult = if (rawResult.success) {
            try {
                tool.verify(parameters, rawResult)
            } catch (t: Throwable) {
                rawResult.copy(
                    verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                    verificationEvidence = "Post-condition verification exception: ${t.message}"
                )
            }
        } else {
            rawResult
        }

        val elapsed = System.currentTimeMillis() - startTime
        eventBus.emit(
            NousSystemEvent.ToolExecuted(
                toolId = toolId,
                success = finalVerifiedResult.success,
                executionTimeMs = elapsed
            )
        )

        return ToolInvocationResult.Completed(finalVerifiedResult)
    }
}
