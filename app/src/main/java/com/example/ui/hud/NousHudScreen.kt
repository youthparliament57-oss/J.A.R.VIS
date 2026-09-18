package com.example.ui.hud

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognition.orchestrator.NousOrchestrator
import com.example.memory.database.entity.SemanticFactEntity
import com.example.perception.vision.VisionPerceptionEngine
import com.example.perception.voice.VoiceEngineStatus
import com.example.perception.voice.VoicePerceptionEngine
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousObsidianDark
import com.example.ui.theme.NousTextPrimary
import com.example.ui.theme.NousTextSecondary

@Composable
fun NousHudScreen(
    orchestrator: NousOrchestrator,
    factsState: List<SemanticFactEntity>,
    voiceEngine: VoicePerceptionEngine,
    visionEngine: VisionPerceptionEngine,
    onFrameCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val agentState by orchestrator.agentState.collectAsState()
    val activePlan by orchestrator.activePlan.collectAsState()
    val pendingApproval by orchestrator.pendingApproval.collectAsState()
    val terminalLogs by orchestrator.terminalLogs.collectAsState()

    val voiceStatus by voiceEngine.engineStatus.collectAsState()
    val isSpeaking by voiceEngine.isTtsActive.collectAsState()

    val isAnalyzingVision by visionEngine.isAnalyzing.collectAsState()
    val lastVisionAnalysis by visionEngine.lastAnalysis.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("nous_hud_screen"),
        containerColor = NousObsidianDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NOUS // JARVIS KERNEL",
                            color = NousCyanNeon,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "PERCEPTUAL MULTI-AGENT RUNTIME (PHASE 2)",
                            color = NousTextSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Text(
                        text = "VOICE: ${voiceStatus.name}",
                        color = if (voiceStatus == VoiceEngineStatus.LISTENING) com.example.ui.theme.NousAmberWarning else NousCyanGlow,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Core Visualizer Orb & State Ring
                StatusOrbRing(
                    agentState = agentState,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Optical Sensor Viewport (CameraX + Analysis)
                OpticalViewportCard(
                    isAnalyzing = isAnalyzingVision,
                    lastAnalysis = lastVisionAnalysis,
                    onFrameCaptured = onFrameCaptured
                )

                // Active Plan Tracker
                ExecutionPlanTracker(plan = activePlan)

                // Live Terminal Log Stream
                TerminalLogStream(logs = terminalLogs)

                // Persistent Memory Card
                MemoryViewerCard(facts = factsState)

                // Input & Command Action Bar with Voice Controls
                HudActionBar(
                    onCommandSubmit = { orchestrator.submitGoal(it) },
                    onEmergencyStop = { orchestrator.triggerEmergencyStop() },
                    voiceStatus = voiceStatus,
                    isSpeaking = isSpeaking,
                    onStartVoiceInput = {
                        voiceEngine.startListening { spokenDirective ->
                            orchestrator.submitGoal(spokenDirective)
                        }
                    },
                    onStopVoiceInput = { voiceEngine.stopListening() },
                    onStopSpeaking = { voiceEngine.stopSpeaking() }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Security Interlock Modal Dialog (Appears on L3/L4 events)
            pendingApproval?.let { approvalRequest ->
                ApprovalModalDialog(
                    request = approvalRequest,
                    onApprove = { orchestrator.approveRequest(approvalRequest.requestId) },
                    onReject = { orchestrator.rejectRequest(approvalRequest.requestId, "User declined action") }
                )
            }
        }
    }
}
