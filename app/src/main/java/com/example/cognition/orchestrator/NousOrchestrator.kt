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
    val lastCognitiveResponse: StateFlow<String?>

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

    private val _lastCognitiveResponse = MutableStateFlow<String?>(null)
    override val lastCognitiveResponse: StateFlow<String?> = _lastCognitiveResponse.asStateFlow()

    private val _terminalLogs = MutableStateFlow<List<String>>(
        listOf(
            "[SYSTEM] NOUS Multimodal Architecture online.",
            "[PERCEPTION] Speech recognizer & Vision sensor initialized.",
            "[COGNITION] Gemini Neural Engine armed and standing by."
        )
    )
    override val terminalLogs: StateFlow<List<String>> = _terminalLogs.asStateFlow()

    override fun submitGoal(userGoal: String) {
        val trimmed = userGoal.trim()
        if (trimmed.isEmpty()) return

        activeExecutionJob?.cancel()
        _pendingApproval.value = null
        _lastCognitiveResponse.value = null

        activeExecutionJob = scope.launch {
            try {
                updateState(AgentState.PLANNING)
                appendLog("[DIRECTIVE] User: \"$trimmed\"")

                // Step 1: Cognition & Multi-Agent Planning
                val plan = plannerAgent.createPlan(trimmed)
                _activePlan.value = plan
                appendLog("[PLAN] Generated execution plan with ${plan.steps.size} step(s)")

                // Step 2: Autonomous Execution Loop
                updateState(AgentState.EXECUTING)
                executeCurrentPlan()
            } catch (e: Exception) {
                updateState(AgentState.ERROR)
                appendLog("[ERROR] Planning/Execution fault: ${e.localizedMessage}")
                voiceEngine?.speak("Execution encountered a fault: ${e.localizedMessage}")
            }
        }
    }

    private suspend fun executeCurrentPlan() {
        val currentPlan = _activePlan.value ?: return
        val updatedSteps = currentPlan.steps.toMutableList()
        var allSucceeded = true

        for (index in updatedSteps.indices) {
            val step = updatedSteps[index]
            if (step.status is StepStatus.Success || step.status is StepStatus.Skipped) {
                continue
            }

            // Mark InProgress and execute tool via ToolExecutor (which handles SecurityGatekeeper)
            updatedSteps[index] = step.copy(status = StepStatus.InProgress)
            _activePlan.value = currentPlan.copy(steps = updatedSteps)
            appendLog("[EXEC] Running step ${index + 1}: ${step.description}")

            val invocationResult = toolExecutor.executeTool(
                stepId = step.stepId,
                toolId = step.toolId,
                parameters = step.inputParameters
            )

            when (invocationResult) {
                is ToolInvocationResult.AwaitingApproval -> {
                    // Step requires explicit human authorization
                    _pendingApproval.value = invocationResult.request
                    updatedSteps[index] = step.copy(status = StepStatus.WaitingApproval)
                    _activePlan.value = currentPlan.copy(steps = updatedSteps)
                    appendLog("[INTERLOCK] Awaiting human approval for '${step.toolId}' (Risk: ${invocationResult.request.permissionLevel})")
                    return
                }
                is ToolInvocationResult.Blocked -> {
                    allSucceeded = false
                    val errorMsg = "Execution blocked: ${invocationResult.reason}"
                    updatedSteps[index] = step.copy(
                        status = StepStatus.Failure(errorMsg),
                        errorMessage = errorMsg
                    )
                    _activePlan.value = currentPlan.copy(steps = updatedSteps)
                    appendLog("[SECURITY] Step ${index + 1} blocked: $errorMsg")
                    updateState(AgentState.ERROR)
                    break
                }
                is ToolInvocationResult.Completed -> {
                    val result = invocationResult.result
                    if (result.success && result.verificationStatus == VerificationStatus.VERIFIED_VALID) {
                        updatedSteps[index] = step.copy(
                            status = StepStatus.Success(result.output),
                            outputResult = result.output,
                            verificationEvidence = result.verificationEvidence
                        )
                        _activePlan.value = currentPlan.copy(steps = updatedSteps)
                        _lastCognitiveResponse.value = result.output
                        appendLog("[VERIFIED] Step ${index + 1} output: ${result.output.take(120)}")
                    } else {
                        allSucceeded = false
                        val errorMsg = result.error ?: "Post-condition verification failed"
                        updatedSteps[index] = step.copy(
                            status = StepStatus.Failure(errorMsg),
                            outputResult = result.output,
                            errorMessage = errorMsg
                        )
                        _activePlan.value = currentPlan.copy(steps = updatedSteps)
                        _lastCognitiveResponse.value = result.output.ifBlank { errorMsg }
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
                    if (out.length > 200) out.take(200) + "..." else out
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
