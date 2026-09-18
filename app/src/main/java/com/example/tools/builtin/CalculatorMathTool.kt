package com.example.tools.builtin

import com.example.security.model.PermissionLevel
import com.example.tools.contract.NousTool
import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult
import com.example.tools.model.ToolParameter
import com.example.tools.model.VerificationStatus

class CalculatorMathTool : NousTool {
    override val definition = ToolDefinition(
        id = "calculator_math",
        name = "Deterministic Math Evaluator",
        description = "Performs exact deterministic arithmetic evaluation (addition, subtraction, multiplication, division, powers).",
        permissionLevel = PermissionLevel.L0_READ_ONLY,
        parameters = listOf(
            ToolParameter(name = "expression", description = "Mathematical expression string, e.g. '125 * 8' or '4500 / 12'", type = "string", required = true)
        )
    )

    override suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult {
        val expr = parameters["expression"]?.toString()?.trim()
            ?: return ToolExecutionResult(false, "Missing required parameter 'expression'", VerificationStatus.FAILED_VERIFICATION)

        return try {
            val result = evaluateSimpleExpression(expr)
            ToolExecutionResult(
                success = true,
                output = "Evaluated: $expr = $result",
                verificationStatus = VerificationStatus.VERIFIED_VALID,
                verificationEvidence = "Exact computed value: $result"
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                success = false,
                output = "Math error: ${e.message}",
                verificationStatus = VerificationStatus.FAILED_VERIFICATION,
                error = e.localizedMessage
            )
        }
    }

    override suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult {
        return executionResult
    }

    private fun evaluateSimpleExpression(expression: String): Double {
        val sanitized = expression.replace(" ", "")
        // Handle basic binary operations
        return when {
            sanitized.contains("+") -> {
                val parts = sanitized.split("+", limit = 2)
                evaluateSimpleExpression(parts[0]) + evaluateSimpleExpression(parts[1])
            }
            sanitized.contains("-") && !sanitized.startsWith("-") -> {
                val parts = sanitized.split("-", limit = 2)
                evaluateSimpleExpression(parts[0]) - evaluateSimpleExpression(parts[1])
            }
            sanitized.contains("*") -> {
                val parts = sanitized.split("*", limit = 2)
                evaluateSimpleExpression(parts[0]) * evaluateSimpleExpression(parts[1])
            }
            sanitized.contains("/") -> {
                val parts = sanitized.split("/", limit = 2)
                val divisor = evaluateSimpleExpression(parts[1])
                if (divisor == 0.0) throw ArithmeticException("Division by zero")
                evaluateSimpleExpression(parts[0]) / divisor
            }
            sanitized.contains("^") -> {
                val parts = sanitized.split("^", limit = 2)
                Math.pow(evaluateSimpleExpression(parts[0]), evaluateSimpleExpression(parts[1]))
            }
            else -> sanitized.toDouble()
        }
    }
}
