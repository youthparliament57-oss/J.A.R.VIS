package com.example.core.event

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class NousSystemEvent {
    data class LogEmitted(
        val tag: String,
        val message: String,
        val level: String = "INFO",
        val timestampMs: Long = System.currentTimeMillis()
    ) : NousSystemEvent()

    data class StateChanged(
        val oldState: com.example.core.model.AgentState,
        val newState: com.example.core.model.AgentState
    ) : NousSystemEvent()

    data class ToolExecuted(
        val toolId: String,
        val success: Boolean,
        val executionTimeMs: Long
    ) : NousSystemEvent()

    data class SecurityAlert(
        val message: String,
        val severity: String
    ) : NousSystemEvent()
}

class EventBus {
    private val _events = MutableSharedFlow<NousSystemEvent>(replay = 50, extraBufferCapacity = 100)
    val events: SharedFlow<NousSystemEvent> = _events.asSharedFlow()

    suspend fun emit(event: NousSystemEvent) {
        _events.emit(event)
    }

    fun tryEmit(event: NousSystemEvent): Boolean {
        return _events.tryEmit(event)
    }
}
