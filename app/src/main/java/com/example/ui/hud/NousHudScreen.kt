package com.example.ui.hud

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cognition.orchestrator.NousOrchestrator
import com.example.core.config.ApiKeyPreferences
import com.example.memory.database.entity.SemanticFactEntity
import com.example.perception.vision.VisionPerceptionEngine
import com.example.perception.voice.VoiceEngineStatus
import com.example.perception.voice.VoicePerceptionEngine
import com.example.ui.theme.NousCyanGlow
import com.example.ui.theme.NousCyanNeon
import com.example.ui.theme.NousObsidianDark
import com.example.ui.theme.NousSurfaceDark
import com.example.ui.theme.NousTextSecondary

@Composable
fun NousHudScreen(
    orchestrator: NousOrchestrator,
    factsState: List<SemanticFactEntity>,
    voiceEngine: VoicePerceptionEngine,
    visionEngine: VisionPerceptionEngine,
    apiKeyPreferences: ApiKeyPreferences,
    onFrameCaptured: (Bitmap) -> Unit,
    modifier: Modifier = Modifier
) {
    val agentState by orchestrator.agentState.collectAsState()
    val activePlan by orchestrator.activePlan.collectAsState()
    val pendingApproval by orchestrator.pendingApproval.collectAsState()
    val terminalLogs by orchestrator.terminalLogs.collectAsState()
    val cognitiveResponse by orchestrator.lastCognitiveResponse.collectAsState()

    val voiceStatus by voiceEngine.engineStatus.collectAsState()
    val isSpeaking by voiceEngine.isTtsActive.collectAsState()

    val isAnalyzingVision by visionEngine.isAnalyzing.collectAsState()
    val lastVisionAnalysis by visionEngine.lastAnalysis.collectAsState()

    val activeApiKey by apiKeyPreferences.apiKeyFlow.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }

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
                // Header Bar with API Key Configuration Trigger
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Key Status Chip & Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NousSurfaceDark)
                                .border(
                                    1.dp,
                                    if (activeApiKey.isNotBlank()) NousCyanNeon else com.example.ui.theme.NousAmberWarning,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showApiKeyDialog = true }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("api_key_status_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = "Configure API Key",
                                    tint = if (activeApiKey.isNotBlank()) NousCyanNeon else com.example.ui.theme.NousAmberWarning,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = if (activeApiKey.isNotBlank()) "AI KEY OK" else "ADD KEY",
                                    color = if (activeApiKey.isNotBlank()) NousCyanNeon else com.example.ui.theme.NousAmberWarning,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "VOICE: ${voiceStatus.name}",
                            color = if (voiceStatus == VoiceEngineStatus.LISTENING) com.example.ui.theme.NousAmberWarning else NousCyanGlow,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Core Visualizer Orb & State Ring
                StatusOrbRing(
                    agentState = agentState,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Prominently Visible Cognitive Response Card
                NeuralResponseCard(
                    responseText = cognitiveResponse,
                    isSpeaking = isSpeaking
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

            // API Key Settings Modal
            if (showApiKeyDialog) {
                ApiKeyConfigDialog(
                    currentApiKey = activeApiKey,
                    onSaveKey = { newKey -> apiKeyPreferences.saveApiKey(newKey) },
                    onDismiss = { showApiKeyDialog = false }
                )
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
