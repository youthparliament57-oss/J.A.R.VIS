# AGENTS.md — NOUS Core Operating Directives

## 1. Project Identity & Vision
- **Project Name:** NOUS
- **Scope:** Advanced High-Tech Autonomous Multi-Agent Assistant & Autonomous Operating Intelligence (Jarvis Architecture).
- **Primary Deployment Target:** Android First -> Expanding to Desktop (PC/macOS/Linux) -> Distributed Nodes, Smart Devices & Embedded Microcontrollers.
- **Team Mode:** Active Engineering Partnership. We do not treat NOUS as a toy, todo app, or science-fiction illusion. We build grounded, production-grade systems with concrete architectures.

## 2. Core Operational Rules for AI Agent
1. **Zero Unsolicited Coding:**
   - **MANDATE:** DO NOT write or modify application source code (`.kt`, `.xml`, etc.) until explicitly instructed by the user.
   - We plan, specify, design schemas, and validate documents first.
2. **Precision & Task Distribution:**
   - Every feature must be decomposed into its architectural requirements, permissions, security risk level, and failure recovery before execution.
   - Work is distributed logically across specialized sub-agents (Orchestrator, Planner, Executor, Verifier, Memory, Vision, Voice, Tools).
3. **Reality-Grounded Engineering:**
   - Adhere to the engineering capability classification:
     - **AVAILABLE:** Realizable with established APIs and Android libraries today.
     - **ENGINEERING:** Technically feasible, requiring robust multi-layer implementation.
     - **RESEARCH:** Requires state-of-the-art multimodal reasoning models.
     - **HARDWARE-DEPENDENT:** Requires specific sensors, devices, or root/Shizuku access.
     - **NOT CURRENTLY REALISTIC:** Do not fake or hallucinate unsupported system magic.
4. **Security & Human-in-the-Loop:**
   - Security level hierarchy (L0 to L4) is strictly enforced. No high-risk autonomous action executes without explicit user authorization and verification check.

## 3. Project File Index
- `NOUS_BLUEPRINT.md`: Full 25-capability taxonomy, sub-feature specifications, and feasibility classification.
- `ANDROID_PHASE_ROADMAP.md`: Android-first development phased milestones, architecture breakdown, and implementation plan.
- `TOOL_REGISTRY_SPEC.md`: Schema, risk levels, permissions, and validation specifications for the NOUS Tool System.
