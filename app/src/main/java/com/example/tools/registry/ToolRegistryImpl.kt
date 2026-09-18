package com.example.tools.registry

import com.example.tools.contract.NousTool
import com.example.tools.contract.ToolRegistry
import com.example.tools.model.ToolDefinition
import java.util.concurrent.ConcurrentHashMap

class ToolRegistryImpl : ToolRegistry {
    private val tools = ConcurrentHashMap<String, NousTool>()

    override fun registerTool(tool: NousTool) {
        tools[tool.definition.id] = tool
    }

    override fun getTool(toolId: String): NousTool? {
        return tools[toolId]
    }

    override fun getAllTools(): List<NousTool> {
        return tools.values.toList()
    }

    override fun getToolDefinitions(): List<ToolDefinition> {
        return tools.values.map { it.definition }
    }
}
