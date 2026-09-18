package com.example.cognition.orchestrator

import com.example.cognition.planner.PlannerAgent
import com.example.core.event.EventBus
import com.example.core.event.NousSystemEvent
import com.example.core.model.AgentState
import com.example.core.model.ExecutionPlan
import com.example.core.model.StepStatus
import com.example.memory.repository.MemoryRepository
import com.example.security.gatekeeper.SecurityGatekeeper
import com.example.security.model.ApprovalRequest
import com.example.tools.executor.ToolExecutor
import com.example.tools.executor.ToolInvocationResult
import com.example.tools.model.VerificationStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

interface NousOrchestrator {
    val agentState: StateFlow<AgentState>
    val activePlan: StateFlow<ExecutionPlan?>
    val pendingApproval: StateFlow<ApprovalRequest?>
    val terminalLogs: StateFlow<List<String>>

    fun submitGoal(userGoal: String)
    fun approveRequest(requestId: String)
    fun rejectRequest(requestId: String, reason: String)
    fun triggerEmergencyStop()
    fun clearLogs()
}

class NousOrchestratorImpl(
    private val plannerAgent: PlannerAgent,
    private val toolExecutor: ToolExecutor,
    private val securityGatekeeper: SecurityGatekeeper,
    private val memoryRepository: MemoryRepository,
    private val eventBus: EventBus,
    private val voiceEngine: com.example.perception.voice.VoicePerceptionEngine? = null
) : NousOrchestrator {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var activeExecutionJob: Job? = null

    private val _agentState = MutableStateFlow(AgentState.IDLE)
    override val agentState: StateFlow<AgentState> = _agentState.asStateFlow()

    private val _activePlan = MutableStateFlow<ExecutionPlan?>(null)
    override val activePlan: StateFlow<ExecutionPlan?> = _activePlan.asStateFlow()

    private val _pendingApproval = MutableStateFlow<ApprovalRequest?>(null)
    override val pendingApproval: StateFlow<ApprovalRequest?> = _pendingApproval.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "[SYSTEM KERNEL] NOUS Master Intelligence initialized.",
            "[GATEWAY] Tool sandbox active. Security policy: L0-L4 hierarchy.",
            "[READY] Standing by for telemetry and voice/text commands."
        )
    )
    override val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    init {
        // Collect event bus messages for terminal telemetry
        scope.launch {
            eventBus.events.collect { event ->
                when (event) {
                    is NousSystemEvent.LogEmitted -> appendLog("[${event.level}] ${event.tag}: ${event.message}")
                    is NousSystemEvent.StateChanged -> appendLog("[STATE] ${event.oldState.name} -> ${event.newState.name}")
                    is NousSystemEvent.ToolExecuted -> appendLog("[TOOL] Executed '${event.toolId}' in ${event.executionTimeMs}ms (Success: ${event.success})")
                    is NousSystemEvent.SecurityAlert -> appendLog("[SECURITY] ${event.message}")
                }
            }
        }
    }

    override fun submitGoal(userGoal: String) {
        if (userGoal.isBlank()) return

        activeExecutionJob?.cancel()
        activeExecutionJob = scope.launch {
            appendLog("[GOAL] Received user intent: \"$userGoal\"")
            updateState(AgentState.PLANNING)

            val plan = plannerAgent.createPlan(userGoal)
            _activePlan.value = plan
            appendLog("[PLANNER] Synthesized plan with ${plan.steps.size} executable step(s).")

            executeCurrentPlan()
        }
    }

    private suspend fun executeCurrentPlan() {
        val currentPlan = _activePlan.value ?: return
        updateState(AgentState.EXECUTING)

        val updatedSteps = currentPlan.steps.toMutableList()
        var allSucceeded = true

        for (index in updatedSteps.indices) {
            val step = updatedSteps[index]
            appendLog("[EXEC] Step ${index + 1}/${updatedSteps.size}: \"${step.description}\"")

            // Mark step InProgress
            updatedSteps[index] = step.copy(status = StepStatus.InProgress)
            _activePlan.value = currentPlan.copy(steps = updatedSteps)

            // Invoke Tool
            val invocation = toolExecutor.executeTool(
                stepId = step.stepId,
                toolId = step.toolId,
                parameters = step.inputParameters
            )

            when (invocation) {
                is ToolInvocationResult.AwaitingApproval -> {
                    updateState(AgentState.AWAITING_USER_APPROVAL)
                    _pendingApproval.value = invocation.request
                    updatedSteps[index] = step.copy(status = StepStatus.WaitingApproval)
                    _activePlan.value = currentPlan.copy(steps = updatedSteps)
                    appendLog("[INTERLOCK] Execution paused at step ${index + 1}. Awaiting explicit user confirmation.")
                    return // Wait for approval callback
                }
                is ToolInvocationResult.Blocked -> {
                    allSucceeded = false
                    val err = "Blocked by Security: ${invocation.reason}"
                    updatedSteps[index] = step.copy(status = StepStatus.Failure(err), errorMessage = err)
                    _activePlan.value = currentPlan.copy(steps = updatedSteps)
                    appendLog("[ERROR] Step ${index + 1} blocked: ${invocation.reason}")
                    updateState(AgentState.ERROR)
                    break
                }
                is ToolInvocationResult.Completed -> {
                    val result = invocation.result
                    if (result.success && result.verificationStatus == VerificationStatus.VERIFIED_VALID) {
                        updatedSteps[index] = step.copy(
                            status = StepStatus.Success(result.output),
                            outputResult = result.output,
                            verificationEvidence = result.verificationEvidence
                        )
                        _activePlan.value = currentPlan.copy(steps = updatedSteps)
                        appendLog("[VERIFIED] Step ${index + 1} confirmed: ${result.verificationEvidence ?: "OK"}")
                    } else {
                        allSucceeded = false
                        val errorMsg = result.error ?: "Post-condition verification failed"
                        updatedSteps[index] = step.copy(
                            status = StepStatus.Failure(errorMsg),
                            outputResult = result.output,
                            errorMessage = errorMsg
                        )
                        _activePlan.value = currentPlan.copy(steps = updatedSteps)
                        appendLog("[FAIL] Step ${index + 1} failed verification: $errorMsg")
                        updateState(AgentState.ERROR)
                        break
                    }
                }
            }
        }

        if (allSucceeded) {
            updateState(AgentState.IDLE)
            appendLog("[COMPLETE] All plan steps executed and verified.")

            // Natural Vocal Feedback
            val spokenSummary = when {
                updatedSteps.size == 1 && updatedSteps.first().outputResult != null -> {
                    val out = updatedSteps.first().outputResult ?: ""
                    if (out.length > 120) out.take(120) + "..." else out
                }
                else -> "Directive executed successfully across ${updatedSteps.size} operational steps."
            }
            voiceEngine?.speak(spokenSummary)

            // Record episode in Room database
            memoryRepository.recordEpisode(
                sessionId = UUID.randomUUID().toString(),
                userGoal = currentPlan.userGoal,
                planSummary = "${updatedSteps.size} steps completed successfully",
                stepsExecutedCount = updatedSteps.size,
                finalStatus = "SUCCESS",
                auditLog = _terminalLogs.value.takeLast(10).joinToString("\n")
            )
        }
    }

    override fun approveRequest(requestId: String) {
        val request = _pendingApproval.value ?: return
        if (request.requestId == requestId) {
            securityGatekeeper.recordApproval(request.stepId)
            _pendingApproval.value = null
            appendLog("[AUTHORIZED] User approved step '${request.toolName}' (${request.permissionLevel}). Resuming...")
            scope.launch {
                executeCurrentPlan()
            }
        }
    }

    override fun rejectRequest(requestId: String, reason: String) {
        val request = _pendingApproval.value ?: return
        if (request.requestId == requestId) {
            securityGatekeeper.recordRejection(request.stepId)
            _pendingApproval.value = null
            appendLog("[REJECTED] User aborted step '${request.toolName}': $reason")
            updateState(AgentState.IDLE)

            val currentPlan = _activePlan.value
            if (currentPlan != null) {
                val updated = currentPlan.steps.map {
                    if (it.stepId == request.stepId) it.copy(status = StepStatus.Failure("Rejected by user")) else it
                }
                _activePlan.value = currentPlan.copy(steps = updated)
            }
        }
    }

    override fun triggerEmergencyStop() {
        activeExecutionJob?.cancel()
        _pendingApproval.value = null
        updateState(AgentState.EMERGENCY_STOPPED)
        appendLog("[EMERGENCY STOP] All active processes, subroutines, and tool executions terminated immediately.")
    }

    override fun clearLogs() {
        _terminalLogs.value = listOf("[SYSTEM] Telemetry buffer reset.")
    }

    private fun updateState(newState: AgentState) {
        val old = _agentState.value
        _agentState.value = newState
        eventBus.tryEmit(NousSystemEvent.StateChanged(old, newState))
    }

    private fun appendLog(log: String) {
        val current = _terminalLogs.value
        _terminalLogs.value = (current + log).takeLast(100)
    }
}
