# NOUS: Tool Registry & Execution Specification

Every action NOUS performs must flow through a registered `NousTool`. NOUS strictly avoids uncontrolled side effects.

---

## 1. Tool Data Contract

```kotlin
data class ToolDefinition(
    val id: String,
    val name: String,
    val description: String,
    val permissionLevel: PermissionLevel,
    val inputParameters: List<ToolParameter>,
    val outputType: String
)

data class ToolParameter(
    val name: String,
    val type: String, // string, integer, boolean, object
    val description: String,
    val required: Boolean
)

data class ToolExecutionResult(
    val toolId: String,
    val isSuccess: Boolean,
    val outputData: String,
    val errorMessage: String? = null,
    val verificationStatus: VerificationStatus
)

enum class VerificationStatus {
    VERIFIED_VALID,
    OUTPUT_UNEXPECTED,
    FAILED_VERIFICATION,
    SKIPPED
}
```

---

## 2. Permission Levels & Gatekeeper Policies

| Level | Identifier | Description | Execution Policy |
|---|---|---|---|
| **L0** | `L0_READ_ONLY` | Sensory & read-only info (battery level, network status, current time, app version). | Autonomous instant execution. No prompt needed. |
| **L1** | `L1_LOW_RISK` | Low-impact device actions (set timer, turn on flashlight, save a note, copy to clipboard). | Autonomous execution with subtle UI toast/indicator. |
| **L2** | `L2_EXTERNAL_COMM` | External network communication (web query, send email draft, ping API). | Logged to HUD activity stream; notification displayed. |
| **L3** | `L3_SENSITIVE_DATA` | Modify/Delete user data (delete note, clear memory, modify file, schedule calendar event). | **MANDATORY**: Modal Approval Dialog with clear description of impact. |
| **L4** | `L4_SYSTEM_DESTRUCTIVE` | Destructive/Privileged actions (uninstall app, reboot device, wipe database, Shizuku root shell). | **MANDATORY**: Biometric or typed double-confirmation. Emergency Stop always available. |

---

## 3. Tool Verification Lifecycle

```
[Agent Selects Tool]
         │
         ▼
[Permission Check: L0 - L4]
         │
   ┌─────┴────────────────┐
   │ (>= L3)              │ (< L3)
   ▼                      ▼
[UI Approval Check]   [Direct Execution]
   │                      │
   └──────────┬───────────┘
              ▼
    [Execute Tool Logic]
              │
              ▼
    [Verification Handler] ──> Checks if result matches expectation
              │
              ▼
  [Update Working Memory]
              │
              ▼
 [Proceed to Next Plan Step]
```
