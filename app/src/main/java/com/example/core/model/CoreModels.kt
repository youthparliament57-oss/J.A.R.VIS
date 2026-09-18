package com.example.core.model

enum class AgentState(val label: String) {
    IDLE("IDLE - READY"),
    LISTENING("LISTENING..."),
    REASONING("REASONING"),
    PLANNING("SYNTHESIZING PLAN"),
    AWAITING_USER_APPROVAL("AWAITING L3/L4 APPROVAL"),
    EXECUTING("EXECUTING STEP"),
    VERIFYING("VERIFYING POST-CONDITIONS"),
    ERROR("ERROR DETECTED"),
    EMERGENCY_STOPPED("EMERGENCY STOPPED")
}

enum class EpistemicConfidence {
    KNOWN,      // Deterministically verified ground truth
    OBSERVED,   // Captured via hardware sensor or tool
    INFERRED,   // Model conclusion, requires validation
    STALE,      // Expired TTL
    UNKNOWN     // Missing or indeterminate state
}

sealed class StepStatus {
    object Pending : StepStatus()
    object WaitingApproval : StepStatus()
    object InProgress : StepStatus()
    data class Success(val observation: String) : StepStatus()
    data class Failure(val error: String) : StepStatus()
    object Skipped : StepStatus()
}

data class PlanStep(
    val stepId: String,
    val description: String,
    val toolId: String,
    val inputParameters: Map<String, Any> = emptyMap(),
    val status: StepStatus = StepStatus.Pending,
    val outputResult: String? = null,
    val verificationEvidence: String? = null,
    val errorMessage: String? = null
)

data class ExecutionPlan(
    val planId: String,
    val userGoal: String,
    val steps: List<PlanStep>,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val completedAtEpochMs: Long? = null
)

sealed class NousResult<out T> {
    data class Success<out T>(val data: T) : NousResult<T>()
    data class Failure(val error: String, val cause: Throwable? = null) : NousResult<Nothing>()
}
