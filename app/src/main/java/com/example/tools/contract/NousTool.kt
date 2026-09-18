package com.example.tools.contract

import com.example.tools.model.ToolDefinition
import com.example.tools.model.ToolExecutionResult

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
    fun getToolDefinitions(): List<ToolDefinition>
}
