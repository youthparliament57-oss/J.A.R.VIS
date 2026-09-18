package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.cognition.orchestrator.NousOrchestrator
import com.example.cognition.orchestrator.NousOrchestratorImpl
import com.example.cognition.planner.RuleBasedPlanner
import com.example.core.event.EventBus
import com.example.memory.database.NousDatabase
import com.example.memory.repository.MemoryRepository
import com.example.memory.repository.MemoryRepositoryImpl
import com.example.perception.vision.LocalVisionEngine
import com.example.perception.vision.VisionPerceptionEngine
import com.example.perception.voice.AndroidVoicePerceptionEngine
import com.example.perception.voice.VoicePerceptionEngine
import com.example.security.gatekeeper.SecurityGatekeeperImpl
import com.example.tools.builtin.CalculatorMathTool
import com.example.tools.builtin.DeviceStatusTool
import com.example.tools.builtin.NotesMemoryTool
import com.example.tools.builtin.SystemFlashlightTool
import com.example.tools.builtin.VisualInspectionTool
import com.example.tools.executor.ToolExecutorImpl
import com.example.tools.registry.ToolRegistryImpl
import com.example.ui.hud.NousHudScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var voiceEngine: VoicePerceptionEngine
    private lateinit var visionEngine: VisionPerceptionEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Core Infrastructure, Event Bus & Memory
        val database = NousDatabase.getInstance(applicationContext)
        val memoryRepository: MemoryRepository = MemoryRepositoryImpl(database)
        val eventBus = EventBus()

        // 2. Perceptual Engines (Voice & Vision)
        voiceEngine = AndroidVoicePerceptionEngine(
            context = applicationContext,
            eventBus = eventBus,
            coroutineScope = lifecycleScope
        )
        visionEngine = LocalVisionEngine(eventBus = eventBus)

        // 3. Security Gatekeeper & Tool Registry
        val securityGatekeeper = SecurityGatekeeperImpl()
        val toolRegistry = ToolRegistryImpl()

        // Register Built-in Deterministic Tools
        toolRegistry.registerTool(DeviceStatusTool(applicationContext))
        toolRegistry.registerTool(CalculatorMathTool())
        toolRegistry.registerTool(SystemFlashlightTool(applicationContext))
        toolRegistry.registerTool(NotesMemoryTool(memoryRepository))
        toolRegistry.registerTool(VisualInspectionTool(visionEngine))

        // 4. Tool Executor & Multi-Agent Planner
        val toolExecutor = ToolExecutorImpl(
            toolRegistry = toolRegistry,
            securityGatekeeper = securityGatekeeper,
            eventBus = eventBus
        )
        val plannerAgent = RuleBasedPlanner(toolRegistry)

        // 5. Central Orchestrator
        val orchestrator: NousOrchestrator = NousOrchestratorImpl(
            plannerAgent = plannerAgent,
            toolExecutor = toolExecutor,
            securityGatekeeper = securityGatekeeper,
            memoryRepository = memoryRepository,
            eventBus = eventBus,
            voiceEngine = voiceEngine
        )

        setContent {
            MyApplicationTheme {
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions()
                ) { /* Permissions evaluated gracefully */ }

                LaunchedEffect(Unit) {
                    val permissionsToRequest = mutableListOf<String>()
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
                    }
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.CAMERA)
                    }
                    if (permissionsToRequest.isNotEmpty()) {
                        permissionLauncher.launch(permissionsToRequest.toTypedArray())
                    }
                }

                val facts by memoryRepository.observeAllFacts().collectAsState(initial = emptyList())
                NousHudScreen(
                    orchestrator = orchestrator,
                    factsState = facts,
                    voiceEngine = voiceEngine,
                    visionEngine = visionEngine,
                    onFrameCaptured = { bitmap ->
                        lifecycleScope.launch {
                            visionEngine.analyzeFrame(bitmap)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceEngine.shutdown()
    }
}
