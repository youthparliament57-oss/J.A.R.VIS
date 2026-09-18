package com.example.cognition.planner

import com.example.core.model.ExecutionPlan
import com.example.core.model.PlanStep
import com.example.core.model.StepStatus
import com.example.tools.contract.ToolRegistry
import java.util.UUID

interface PlannerAgent {
    suspend fun createPlan(userGoal: String): ExecutionPlan
}

class RuleBasedPlanner(private val toolRegistry: ToolRegistry) : PlannerAgent {

    override suspend fun createPlan(userGoal: String): ExecutionPlan {
        val planId = UUID.randomUUID().toString()
        val lowerGoal = userGoal.lowercase().trim()
        val steps = mutableListOf<PlanStep>()

        when {
            // Check battery / system telemetry
            lowerGoal.contains("battery") || lowerGoal.contains("status") || lowerGoal.contains("system") || lowerGoal.contains("storage") || lowerGoal.contains("network") -> {
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Query local hardware sensors and connectivity state",
                        toolId = "device_status",
                        inputParameters = emptyMap(),
                        status = StepStatus.Pending
                    )
                )
            }

            // Math expressions
            lowerGoal.contains("calculate") || lowerGoal.contains("math") || lowerGoal.matches(Regex(".*[0-9]+[\\s]*[\\+\\-\\*/\\^][\\s]*[0-9]+.*")) -> {
                val expr = extractExpression(userGoal)
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Deterministically evaluate mathematical expression '$expr'",
                        toolId = "calculator_math",
                        inputParameters = mapOf("expression" to expr),
                        status = StepStatus.Pending
                    )
                )
            }

            // Flashlight / torch
            lowerGoal.contains("flashlight") || lowerGoal.contains("torch") -> {
                val turnOn = !lowerGoal.contains("off")
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = if (turnOn) "Enable system rear camera torch" else "Disable system rear camera torch",
                        toolId = "system_flashlight",
                        inputParameters = mapOf("enabled" to turnOn),
                        status = StepStatus.Pending
                    )
                )
            }

            // Remember / save note
            lowerGoal.startsWith("remember") || lowerGoal.contains("save note") || lowerGoal.contains("store fact") -> {
                val clean = userGoal.removePrefix("remember").removePrefix("save note").removePrefix("store fact").trim().removePrefix(":").removePrefix("that").trim()
                val parts = if (clean.contains("=")) clean.split("=", limit = 2) else listOf("note_${System.currentTimeMillis() % 10000}", clean)
                val key = parts[0].trim()
                val value = if (parts.size > 1) parts[1].trim() else clean

                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Store fact '$key' in persistent semantic memory (Requires L3 Authorization)",
                        toolId = "notes_memory",
                        inputParameters = mapOf(
                            "action" to "SAVE",
                            "key" to key,
                            "value" to value,
                            "category" to "USER_FACT"
                        ),
                        status = StepStatus.Pending
                    )
                )
            }

            // Delete note / memory
            lowerGoal.startsWith("forget") || lowerGoal.contains("delete note") -> {
                val key = userGoal.removePrefix("forget").removePrefix("delete note").trim()
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Delete fact '$key' from persistent semantic memory (Requires L3 Authorization)",
                        toolId = "notes_memory",
                        inputParameters = mapOf(
                            "action" to "DELETE",
                            "key" to key
                        ),
                        status = StepStatus.Pending
                    )
                )
            }

            // Visual inspection / camera / look / see
            lowerGoal.contains("inspect") || lowerGoal.contains("look") || lowerGoal.contains("camera") || lowerGoal.contains("see") || lowerGoal.contains("vision") -> {
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Sample and inspect optical viewport field from device camera",
                        toolId = "visual_inspection_tool",
                        inputParameters = mapOf("query" to userGoal),
                        status = StepStatus.Pending
                    )
                )
            }

            // Multi-step complex plan (Diagnostic & Calculation & Status check)
            lowerGoal.contains("diagnostic") || lowerGoal.contains("full check") -> {
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Query core system telemetry and battery level",
                        toolId = "device_status",
                        inputParameters = emptyMap(),
                        status = StepStatus.Pending
                    )
                )
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Perform sanity calculation on memory footprint (1024 * 64)",
                        toolId = "calculator_math",
                        inputParameters = mapOf("expression" to "1024 * 64"),
                        status = StepStatus.Pending
                    )
                )
            }

            // Fallback: general query / conversational / knowledge inquiry -> Gemini Neural LLM
            else -> {
                steps.add(
                    PlanStep(
                        stepId = UUID.randomUUID().toString(),
                        description = "Reason and formulate cognitive response via Gemini Neural Intelligence",
                        toolId = "gemini_cognition_tool",
                        inputParameters = mapOf("query" to userGoal),
                        status = StepStatus.Pending
                    )
                )
            }
        }

        return ExecutionPlan(
            planId = planId,
            userGoal = userGoal,
            steps = steps,
            createdAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun extractExpression(goal: String): String {
        val sanitized = goal.replace("calculate", "", ignoreCase = true)
            .replace("what is", "", ignoreCase = true)
            .replace("evaluate", "", ignoreCase = true)
            .replace("math", "", ignoreCase = true)
            .replace("?", "")
            .trim()
        return if (sanitized.isNotEmpty()) sanitized else "2 + 2"
    }
}
