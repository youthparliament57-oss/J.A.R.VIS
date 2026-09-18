# NOUS Architecture: Core System Architecture & Module Boundaries

- **Document Version:** 1.0.0
- **Status:** SPECIFICATION COMPLETE
- **Scope:** Full-System Topography, Package Layout, Data Contracts & Interface Specifications

---

## 1. System Topography & Information Flow

The NOUS runtime is structured as a closed-loop perception-cognition-action-verification engine:

```
[ User Query / Voice / Sensor Event ]
                  │
                  ▼
         [ Perception Engine ]
     (STT / CameraX / Telemetry)
                  │
                  ▼
         [ Context Aggregator ] ◄─── [ Hierarchical Memory ]
       (Ephemeral World State)        (Room: Episodic/Semantic/Procedural)
                  │
                  ▼
          [ Cognition Core ]
          ┌──────────────────────────────────────────────┐
          │  1. Goal Deconstruction Engine (DAG)         │
          │  2. Multi-Agent Specialist Loop:             │
          │     - PlannerAgent: Step synthesis           │
          │     - Gatekeeper: Risk evaluation (L0 - L4)  │
          │     - ExecutorAgent: Tool dispatch           │
          │     - VerifierAgent: Output validation       │
          └──────────────────────────────────────────────┘
                  │
                  ├──[ Requires Approval >= L3 ]──► [ UI Approval Dialog ]
                  │                                         │
                  ▼                                         ▼
         [ Tool Execution Gateway ] ◄───────────────────────┘
     (Deterministic Invocation Sandbox)
                  │
                  ▼
          [ Verifier Agent ]
     (Pre/Post Condition Validation)
                  │
                  ▼
        [ Memory Consolidation ]
    (Episodic Recording & Fact Updates)
                  │
                  ▼
        [ HUD UI & Voice Output ]
   (Status Ring / Plan Tracker / TTS Audio)
```

---

## 2. Foundational Repository Package Structure

The codebase is organized into clean, isolated modules and packages that map directly to functional domains:

```
com.nous
├── core
│   ├── model
│   │   ├── AgentState.kt              // Agent lifecycle states
│   │   ├── EpistemicConfidence.kt     // Fact validity classifications
│   │   ├── ExecutionPlan.kt           // Plan, Step, StepStatus entities
│   │   └── NousResult.kt              // Sealed monadic result container
│   ├── event
│   │   ├── EventBus.kt                // Central SharedFlow event dispatcher
│   │   └── NousSystemEvent.kt         // Telemetry, step, and audit events
│   └── util
│       └── SystemClock.kt             // Monotonic timestamp provider
│
├── security
│   ├── model
│   │   ├── PermissionLevel.kt         // L0, L1, L2, L3, L4 definitions
│   │   ├── SecurityPolicy.kt          // Policy rules and permission ceilings
│   │   └── ApprovalRequest.kt         // Human-in-the-loop payload
│   └── gatekeeper
│       ├── SecurityGatekeeper.kt      // Intercepts tool calls and evaluates risk
│       └── SecurityGatekeeperImpl.kt  // Concrete policy implementation
│
├── memory
│   ├── database
│   │   ├── NousDatabase.kt            // Room database configuration
│   │   ├── Converters.kt              // Type converters for JSON/Dates
│   │   ├── dao
│   │   │   ├── EpisodicMemoryDao.kt   // Session and step logs
│   │   │   ├── SemanticFactDao.kt     // User facts and knowledge base
│   │   │   └── ProceduralWorkflowDao.kt // Reusable action workflows
│   │   └── entity
│   │       ├── EpisodicRecordEntity.kt
│   │       ├── SemanticFactEntity.kt
│   │       └── ProceduralWorkflowEntity.kt
│   └── repository
│       ├── MemoryRepository.kt        // Unified repository contract
│       └── MemoryRepositoryImpl.kt    // Room-backed implementation
│
├── tools
│   ├── model
│   │   ├── ToolDefinition.kt          // Schema, name, parameters, permission
│   │   ├── ToolParameter.kt           // Typed input argument specification
│   │   ├── ToolExecutionResult.kt     // Output container with verification status
│   │   └── VerificationStatus.kt      // VERIFIED_VALID, FAILED_VERIFICATION, etc.
│   ├── contract
│   │   ├── NousTool.kt                // Primary interface for all tools
│   │   ├── ToolRegistry.kt            // Tool discovery and registration service
│   │   └── ToolExecutor.kt            // Sandboxed runner with security gates
│   ├── registry
│   │   ├── ToolRegistryImpl.kt
│   │   └── ToolExecutorImpl.kt
│   └── builtin
│       ├── DeviceStatusTool.kt        // L0: Battery, memory, network stats
│       ├── CalculatorMathTool.kt      // L0: Deterministic expression parser
│       ├── SystemFlashlightTool.kt    // L1: Torch control via CameraManager
│       ├── TimerAlarmTool.kt          // L1: System clock intent timers
│       ├── NotesMemoryTool.kt         // L1/L3: Fact recording and search
│       ├── WebSearchQueryTool.kt      // L2: Remote HTTP query lookup
│       └── AppLauncherTool.kt         // L2: Package launch intents
│
├── cognition
│   ├── orchestrator
│   │   ├── NousOrchestrator.kt        // Master multi-agent coordinator
│   │   └── NousOrchestratorImpl.kt    // Concrete orchestration loop
│   ├── agent
│   │   ├── PlannerAgent.kt            // Goal decomposition into ExecutionPlan
│   │   ├── ExecutorAgent.kt           // Dispatches steps to ToolExecutor
│   │   └── VerifierAgent.kt           // Validates post-conditions & results
│   └── llm
│       ├── GeminiClient.kt            // Direct Gemini API communication
│       └── ModelResponseParser.kt     // JSON deserializer for plan schemas
│
├── perception
│   ├── voice
│   │   ├── VoiceEngine.kt             // SpeechRecognizer & TTS coordination
│   │   └── BargeInDetector.kt         // Audio interrupt processor
│   └── vision
│       ├── VisionEngine.kt            // CameraX frame analyzer
│       └── OfflineOcrProcessor.kt     // ML Kit text extractor
│
├── device
│   ├── DeviceTelemetryManager.kt      // Battery, Network, Storage sensors
│   └── AppLauncherManager.kt          // Intent launcher and resolver
│
└── ui
    ├── theme
    │   ├── Color.kt                   // Futuristic HUD palette (Neon Cyan, Deep Obsidian, Amber Alert)
    │   ├── Theme.kt                   // MaterialTheme dynamic color wrapper
    │   └── Type.kt                    // Monospace and Display typography
    ├── hud
    │   ├── NousHudScreen.kt           // Root composable container
    │   ├── StatusOrbRing.kt           // Animated state visualization
    │   ├── ExecutionPlanTracker.kt    // Interactive step list with status indicators
    │   ├── TerminalLogStream.kt       // Live system telemetry output
    │   ├── HudActionBar.kt            // Input box, microphone, emergency stop
    │   └── ApprovalModalDialog.kt     // L3/L4 confirmation dialog
    └── viewmodel
        └── NousHudViewModel.kt        // Main ViewModel bridging state to UI
```

---

## 3. Data Contracts & Concrete Interface Definitions

### 3.1 Core State & Epistemic Model

```kotlin
package com.nous.core.model

enum class AgentState {
    IDLE,
    LISTENING,
    REASONING,
    PLANNING,
    EXECUTING,
    VERIFYING,
    AWAITING_USER_APPROVAL,
    ERROR,
    EMERGENCY_STOPPED
}

enum class EpistemicConfidence {
    KNOWN,     // Deterministically verified ground truth (e.g. system battery level)
    OBSERVED,  // Directly captured via hardware sensor (e.g. OCR text, camera frame)
    INFERRED,  // Deductive conclusion produced by LLM reasoning (requires verification)
    STALE,     // Previously valid fact whose time-to-live (TTL) has expired
    UNKNOWN    // Missing required parameter or ambiguous intent
}

sealed class NousResult<out T> {
    data class Success<out T>(val data: T) : NousResult<T>()
    data class Failure(val error: String, val throwable: Throwable? = null) : NousResult<Nothing>()
}

data class PlanStep(
    val id: String,
    val description: String,
    val toolId: String,
    val inputParameters: Map<String, Any>,
    val status: StepStatus,
    val outputResult: String? = null,
    val errorMessage: String? = null
)

enum class StepStatus {
    PENDING,
    WAITING_APPROVAL,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    SKIPPED
}

data class ExecutionPlan(
    val planId: String,
    val goalId: String,
    val goalDescription: String,
    val steps: List<PlanStep>,
    val createdAtEpochMs: Long,
    val completedAtEpochMs: Long? = null
)
```

### 3.2 Security & Gatekeeper Contract

```kotlin
package com.nous.security.model

enum class PermissionLevel(val levelCode: Int, val description: String) {
    L0_READ_ONLY(0, "Read-only environmental or device telemetry. Autonomous."),
    L1_LOW_RISK(1, "Low-risk localized device action with minimal side effect. Autonomous with notification."),
    L2_EXTERNAL_COMM(2, "External network data transmission or draft generation. Logged to audit stream."),
    L3_SENSITIVE_DATA(3, "Modification or deletion of user records, notes, calendar, or files. Mandatory approval."),
    L4_SYSTEM_DESTRUCTIVE(4, "System administrative operations, package control, or bulk purge. Mandatory multi-confirm.")
}

data class ApprovalRequest(
    val requestId: String,
    val stepId: String,
    val toolId: String,
    val toolName: String,
    val requiredPermission: PermissionLevel,
    val actionSummary: String,
    val proposedParameters: Map<String, Any>,
    val riskExplanation: String
)

interface SecurityGatekeeper {
    suspend fun evaluateAuthorization(
        toolId: String,
        permissionLevel: PermissionLevel,
        parameters: Map<String, Any>
    ): AuthorizationDecision
}

sealed class AuthorizationDecision {
    object Granted : AuthorizationDecision()
    data class RequiresUserApproval(val request: ApprovalRequest) : AuthorizationDecision()
    data class Denied(val reason: String) : AuthorizationDecision()
}
```

### 3.3 Tool Gateway & Sandbox Contract

```kotlin
package com.nous.tools.contract

import com.nous.security.model.PermissionLevel
import com.nous.tools.model.ToolDefinition
import com.nous.tools.model.ToolExecutionResult

interface NousTool {
    val definition: ToolDefinition
    
    suspend fun execute(parameters: Map<String, Any>): ToolExecutionResult
    
    suspend fun verify(
        parameters: Map<String, Any>,
        executionResult: ToolExecutionResult
    ): ToolExecutionResult
}

interface ToolRegistry {
    fun registerTool(tool: NousTool)
    fun getTool(toolId: String): NousTool?
    fun getAllTools(): List<NousTool>
    fun getAvailableToolDefinitions(): List<ToolDefinition>
}

interface ToolExecutor {
    suspend fun executeTool(
        toolId: String,
        parameters: Map<String, Any>
    ): ToolExecutionResult
}
```

### 3.4 Hierarchical Memory Contract

```kotlin
package com.nous.memory.repository

import com.nous.core.model.EpistemicConfidence
import kotlinx.coroutines.flow.Flow

data class EpisodicEntry(
    val id: String,
    val sessionId: String,
    val timestampEpochMs: Long,
    val userPrompt: String,
    val planId: String?,
    val executionSummary: String,
    val success: Boolean
)

data class SemanticFact(
    val key: String,
    val value: String,
    val category: String,
    val confidence: EpistemicConfidence,
    val updatedAtEpochMs: Long
)

data class ProceduralWorkflow(
    val workflowId: String,
    val triggerPattern: String,
    val planTemplateJson: String,
    val executionCount: Int,
    val successRate: Float
)

interface MemoryRepository {
    suspend fun recordEpisodicMemory(entry: EpisodicEntry)
    fun observeRecentEpisodes(limit: Int): Flow<List<EpisodicEntry>>
    
    suspend fun storeFact(fact: SemanticFact)
    suspend fun queryFact(key: String): SemanticFact?
    fun observeFactsByCategory(category: String): Flow<List<SemanticFact>>
    suspend fun deleteFact(key: String)
    
    suspend fun saveWorkflow(workflow: ProceduralWorkflow)
    suspend fun findMatchingWorkflow(prompt: String): ProceduralWorkflow?
}
```

### 3.5 Cognition & Multi-Agent Orchestrator Contract

```kotlin
package com.nous.cognition.orchestrator

import com.nous.core.model.AgentState
import com.nous.core.model.ExecutionPlan
import com.nous.security.model.ApprovalRequest
import kotlinx.coroutines.flow.StateFlow

interface NousOrchestrator {
    val agentState: StateFlow<AgentState>
    val activePlan: StateFlow<ExecutionPlan?>
    val pendingApproval: StateFlow<ApprovalRequest?>
    val terminalLogs: StateFlow<List<String>>

    suspend fun submitUserGoal(goalText: String)
    suspend fun approvePendingAction(requestId: String)
    suspend fun rejectPendingAction(requestId: String, userReason: String)
    suspend fun emergencyStop()
    suspend fun clearHistory()
}
```

---

## 4. State Machine Transition Rules

The NOUS agent runtime moves through discrete lifecycle states with guaranteed transition boundaries:

1. **IDLE ➔ LISTENING:** Triggered by user tapping microphone or voice wake event.
2. **LISTENING ➔ REASONING:** Speech input stream completes and transcription is emitted.
3. **REASONING ➔ PLANNING:** Goal parsed and context aggregated from Room memory.
4. **PLANNING ➔ EXECUTING:** Valid ExecutionPlan synthesized by PlannerAgent.
5. **EXECUTING ➔ AWAITING_USER_APPROVAL:** Executor encounters a step with PermissionLevel >= L3.
6. **AWAITING_USER_APPROVAL ➔ EXECUTING:** User approves the action via HUD modal dialog.
7. **AWAITING_USER_APPROVAL ➔ PLANNING:** User rejects the action; dynamic replan triggered with rejection context.
8. **EXECUTING ➔ VERIFYING:** Tool returns output; VerifierAgent audits post-condition match.
9. **VERIFYING ➔ EXECUTING:** Current step verified; proceed to next step in plan DAG.
10. **VERIFYING ➔ IDLE:** All steps verified and completed successfully. Response synthesized.
11. **ANY_STATE ➔ EMERGENCY_STOPPED:** Triggered by user hitting the Emergency Stop UI button or voice "STOP". All running coroutines cancelled instantly; hardware held in safe state.
12. **ANY_STATE ➔ ERROR:** Unhandled exception or repeated verification failure (> 3 attempts); graceful error explanation presented.

---

## 5. Summary & Phase 0 Deliverable Sign-Off

Phase 0 establishes the unambiguous foundational contracts and technical blueprints for the entire NOUS system. With all architectural decisions codified in `docs/TECHNOLOGY_DECISION_RECORD.md` and `docs/SYSTEM_ARCHITECTURE.md`, the platform is ready for Phase 1 milestone execution.
