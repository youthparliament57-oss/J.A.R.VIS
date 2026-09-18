# NOUS: Android-First Engineering Roadmap

This document outlines the systematic, phased implementation strategy for building NOUS on Android before scaling to desktop and embedded nodes.

---

## Phase 1: Foundation, Security Engine, Memory & HUD [CURRENT FOCUS]
*Objective: Establish the unstoppable core kernel, memory database, tool execution engine, and futuristic HUD interface.*

### Milestones & Deliverables:
1. **Core Data Models & Enums:**
   - Security Permission Levels (`L0_READ_ONLY`, `L1_LOW_RISK`, `L2_EXTERNAL_COMM`, `L3_SENSITIVE_DATA`, `L4_SYSTEM_DESTRUCTIVE`).
   - Task & Plan Entities: `TaskGoal`, `ExecutionPlan`, `PlanStep`, `StepStatus` (`PENDING`, `IN_PROGRESS`, `SUCCESS`, `FAILED`, `WAITING_APPROVAL`).
   - Agent State Model: `IDLE`, `LISTENING`, `REASONING`, `PLANNING`, `EXECUTING`, `VERIFYING`, `AWAITING_USER_APPROVAL`, `ERROR`.

2. **Hierarchical Memory System (Room Database):**
   - `EpisodicMemoryDao` & Entity: Session-based interaction logs, executed steps, outcomes.
   - `SemanticFactDao` & Entity: Key-value user facts, world knowledge, categorized preferences.
   - `ProceduralWorkflowDao` & Entity: Reusable learned routines and command patterns.

3. **Tool Registry & Execution Sandbox:**
   - Interface `NousTool` with input JSON validation, permission level, execution logic, and self-verification hook.
   - Initial Built-in Tools:
     - `DeviceStatusTool` (Battery, Network, Storage, RAM, Uptime).
     - `TimerAlarmTool` (Create system alarms/timers).
     - `NotesMemoryTool` (Record and query semantic/episodic memory).
     - `AppLauncherTool` (Launch installed applications by name/package).
     - `SystemFlashlightTool` (Toggle torch with camera manager).
     - `WebSearchQueryTool` (External query retrieval).
     - `CalculatorMathTool` (Deterministic mathematical evaluator).

4. **Security & Approval Checkpoint Engine:**
   - Intercepts any tool execution request >= L2.
   - Dispatches a structured Approval Dialog to the UI for L3/L4 actions with risk explanation.

5. **Multi-Agent Orchestrator Loop:**
   - `NousOrchestrator`: Coordinates user prompt -> `PlannerAgent` (produces steps) -> `ExecutorAgent` (runs tools) -> `VerifierAgent` (checks output) -> User Feedback.

6. **NOUS Futuristic HUD (Material 3 Jetpack Compose):**
   - High-tech reactive interface:
     - Status Ring / Waveform visualization for agent state.
     - Live Step Execution Tracker (Active plan with checkmarks and real-time step status).
     - Real-Time Terminal / Activity Log stream (observability).
     - Quick Action Bar & Input Console (Voice/Text toggle, Emergency Stop button).
     - Memory & Capability Browser sheet.

---

## Phase 2: Perceptual Channels (Voice & Vision)
*Objective: Make NOUS see and hear without sluggish delays.*

### Milestones & Deliverables:
1. **Voice Perception Engine:**
   - Continuous & Push-to-Talk via Android `SpeechRecognizer`.
   - Local Voice Activity Detection (VAD).
   - Android `TextToSpeech` with natural pitch, stream pacing, and instantaneous "Stop" interrupt barge-in.
2. **Vision Perception Engine:**
   - CameraX integration with tap-to-inspect and continuous frame sampling.
   - Offline OCR via Google ML Kit Text Recognition.
   - Multimodal Gemini Vision pipeline for complex scene, diagram, and UI analysis.

---

## Phase 3: Android Device Mastery & Accessibility Automation
*Objective: Allow NOUS to operate the Android operating system within secure bounds.*

### Milestones & Deliverables:
1. **Accessibility Service Agent (`NousAccessibilityService`):**
   - Screen hierarchy reader (`AccessibilityNodeInfo`).
   - Click, swipe, scroll, text injection helpers.
2. **System Telemetry & Notifications:**
   - `NotificationListenerService` to process incoming messages and alerts.
   - Battery, Wi-Fi, Bluetooth, Audio mode broadcast receivers.
3. **Advanced Automation (Optional Shizuku / ADB connector):**
   - Package installer/uninstaller, granular permission toggles for power users.

---

## Phase 4: Proactive Intelligence & Autonomous Background Tasks
*Objective: Shift from reactive chatbot to proactive autonomous agent.*

### Milestones & Deliverables:
1. **Proactive Event Bus:**
   - Android `WorkManager` background recurring jobs (morning briefing, battery warning, scheduled workflows).
2. **Self-Monitoring & Circuit Breakers:**
   - Autonomous error diagnosis and retry limits (max 3 retries with dynamic strategy alteration).

---

## Phase 5: Distributed Multi-Node Connectivity
*Objective: Bridge Android NOUS with Desktop PC, Cloud, and Embedded Nodes.*

### Milestones & Deliverables:
1. **Local Node Protocol (mDNS / WebSocket):**
   - Discover desktop companion server on local Wi-Fi.
2. **Cross-Device Command Dispatch:**
   - Android as remote sensor & microphone; PC executes heavy compute, terminal scripts, and IDE actions.
