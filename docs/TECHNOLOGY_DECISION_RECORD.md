# NOUS Architecture: Technology Decision Record (TDR)

- **Document Version:** 1.0.0
- **Status:** APPROVED & ACTIVE
- **Scope:** Full-System Architecture & Engineering Stack Selection
- **Target Platform:** Android Native (Primary Kernel) -> Distributed Hybrid Expansion

---

## 1. Context & Architectural Principles

The NOUS system is an autonomous multi-agent operating intelligence built on top of Android, expanding outward to personal computing and ambient IoT nodes. Unlike conversational chatbots or basic prompt wrappers, NOUS functions as an active execution loop: observing environment state, decomposing goals into verifiable sub-steps, mediating safety through formal risk levels (L0–L4), and executing actions through a deterministic Tool Gateway.

To ensure stability, offline capability, security, and low-latency interaction, every layer of the technology stack must satisfy strict production-engineering criteria:
1. **Strict Determinism:** Critical control flow, permissions, database transactions, and safety interlocks must run on deterministic code, not probabilistic model guesses.
2. **Zero-Trust Tool Execution:** External side effects are isolated behind an explicit gatekeeper with verification hooks.
3. **Reactive State Flow:** Unidirectional data flow (UDF) guarantees synchronization across background services, multi-agent reasoning routines, and the UI.
4. **Offline-First Persistence:** Complete operational memory must exist in local structured storage before syncing or querying remote cloud models.

---

## 2. Technology Selection Matrix

### 2.1 Core Language & Concurrency
- **Selected:** **Kotlin 1.9+ with Coroutines & StateFlow / SharedFlow**
- **Rationale:**
  - First-class native Android support with full type safety and zero-overhead interop.
  - Coroutine structured concurrency (`SupervisorJob`, `CoroutineScope`, `Dispatchers.IO`, `Dispatchers.Default`) provides clean lifecycle-bound background execution without memory leaks or unhandled thread crashes.
  - Reactive streams via `StateFlow` and `SharedFlow` provide backpressure-aware event broadcasting between background agents, sensor loops, and Compose UI.
- **Alternatives Considered & Rejected:**
  - *Java:* Lacks modern coroutines, concise data classes, sealed interfaces, and native Jetpack Compose ergonomics.
  - *C++ / NDK:* Unnecessary complexity for the high-level orchestration kernel; reserved only if custom on-device neural tensor runtimes become bottlenecked.

### 2.2 User Interface & Visual Presentation
- **Selected:** **Jetpack Compose (Material Design 3 with Futuristic HUD Design Tokens)**
- **Rationale:**
  - Fully declarative, reactive UI framework tightly coupled with Compose Runtime state.
  - Effortless high-frequency telemetry updates (active plan steps, HUD waveforms, dynamic terminal logs) with minimal recomposition cost.
  - Seamless support for dynamic window size classes, tablet foldables, and accessibility standards (minimum 48dp touch targets, semantic content descriptions).
- **Alternatives Considered & Rejected:**
  - *Traditional XML Layouts:* High boilerplate, slow imperative view binding, difficult dynamic list state synchronization during multi-agent plan updates.
  - *Webview / Hybrid (Flutter/React Native):* Unacceptable latency overhead, disconnected from native Android OS system services (AccessibilityNodeInfo, CameraX, AudioRecord, MediaProjection).

### 2.3 Data Persistence & Memory Storage
- **Selected:** **Android Room 2.6+ (SQLite abstraction with KSP)**
- **Rationale:**
  - Compile-time SQL query verification preventing runtime crash bugs.
  - Native integration with Coroutines and Flow for reactive stream emission upon database mutations.
  - Clean separation of memory tiers:
    1. **Episodic Memory:** Immutable log of historical sessions, tasks, executed steps, tool inputs, outputs, and verifications.
    2. **Semantic Facts:** Key-value and entity-attribute-value (EAV) storage representing user preferences, domain knowledge, and world facts with confidence scores and timestamps.
    3. **Procedural Memory:** Reusable workflow templates, tool execution sequences, and successful plan macros.
    4. **Working Memory:** Fast in-memory state backed by active transaction caches for active goal execution.
- **Alternatives Considered & Rejected:**
  - *Raw SQLite:* Lacks compile-time safety, prone to schema mismatch bugs and manual cursor mapping overhead.
  - *DataStore / SharedPreferences:* Suitable only for simple key-value settings, completely inadequate for structured relational memory, indexed queries, and execution history logs.

### 2.4 Multimodal Reasoning & Foundation Intelligence
- **Selected:** **Google Gemini Multimodal Models (Gemini 1.5 Flash & Gemini 1.5 Pro)**
- **Rationale:**
  - Native multimodal context windows capable of processing high-resolution visual frames, complex system prompts, structured tool definitions, and historical transcripts simultaneously.
  - Strict JSON schema enforcement and native function calling protocol matching the NOUS Tool Gateway specification.
  - Dual-tier utilization: Gemini 1.5 Flash for rapid low-latency step planning, entity extraction, and tool selection; Gemini 1.5 Pro for deep multi-step DAG planning, complex code generation, and failure diagnosis.
- **Alternatives Considered & Rejected:**
  - *Pure On-Device Nano Models:* Insufficient reasoning depth for complex multi-step tool decomposition and dynamic replanning on consumer mobile chipsets at current maturity.
  - *Generic Chatbot APIs:* Lack native multimodal frame processing, fine-grained safety settings, and structured output guarantees.

### 2.5 Perceptual Channels: Voice & Audio
- **Selected:** **Hybrid Speech Pipeline (Android SpeechRecognizer + TextToSpeech + Barge-In Interrupter)**
- **Rationale:**
  - Low-latency local processing for speech capture via `android.speech.SpeechRecognizer` with streaming partial results.
  - Local `android.speech.tts.TextToSpeech` provides instant voice synthesis without cloud transmission latency.
  - Real-time Voice Activity Detection (VAD) coupled with immediate AudioTrack flushing enables "Barge-in" (instant voice interruption when the user speaks while NOUS is responding).
- **Alternatives Considered & Rejected:**
  - *Cloud-Only Audio Streaming:* High latency (500ms - 1500ms roundtrip) disrupts natural conversational pace and fails when offline or on unstable cellular connections.

### 2.6 Perceptual Channels: Vision & Document Analysis
- **Selected:** **CameraX + Google ML Kit (Text Recognition / Barcode) + Gemini Multimodal Vision**
- **Rationale:**
  - CameraX manages camera lifecycles across diverse Android hardware configurations with zero boilerplate.
  - On-device Google ML Kit delivers instant offline OCR and QR/Barcode extraction (< 40ms) without cloud transmission or API token costs.
  - Gemini Multimodal Vision handles semantic scene interpretation, UI screenshot analysis, and physical object reasoning when on-device OCR is insufficient.

### 2.7 Device Automation & Operating System Access
- **Selected:** **Android AccessibilityService + NotificationListenerService + System Intent Gateway**
- **Rationale:**
  - `AccessibilityService` allows programmatic inspection of UI node hierarchies (`AccessibilityNodeInfo`) and dispatch of synthetic gestures (`dispatchGesture`) without rooting or voiding device security.
  - `NotificationListenerService` grants real-time telemetry on incoming messages, alerts, and system notifications.
  - Standard Android Intents facilitate zero-risk execution for common operations (opening apps, setting timers, composing emails).
  - Optional Shizuku/ADB bridge isolated behind Level 4 (L4) security checkpoints for advanced administrative actions.

### 2.8 Security Architecture & Permission Gatekeeper
- **Selected:** **5-Tier Tiered Permission Model (L0–L4) with Intercepting Proxy**
- **Rationale:**
  - Guarantees human-in-the-loop for any action that affects external services or modifies persistent user data.
  - Explicit classification:
    - **L0 (Read-Only):** Instant telemetry queries (battery, network, sensors).
    - **L1 (Low-Risk):** Local device settings and transient actions (flashlight, local timer, clipboard).
    - **L2 (External Comm):** Network requests, draft dispatches, read external web endpoints.
    - **L3 (Sensitive Data):** Modify or delete contacts, calendar events, memory entries, or files. Mandatory UI Approval Dialog.
    - **L4 (System Destructive):** Privileged system configurations, root/Shizuku actions, bulk data wiping. Mandatory dual-factor or biometric authorization.

---

## 3. Engineering Quality Standard & Compilation Mandate

All subsequent code implementations in Phase 1 through Phase 5 must adhere to this document. No placeholder implementations, stubbed mocks, or missing parameters are permitted in the production codebase. Every component must compile and run on the Android target platform.
