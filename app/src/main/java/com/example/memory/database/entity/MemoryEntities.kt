package com.example.memory.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "episodic_memory")
data class EpisodicRecordEntity(
    @PrimaryKey
    val recordId: String,
    val sessionId: String,
    val timestampEpochMs: Long,
    val userGoal: String,
    val planSummary: String,
    val stepsExecutedCount: Int,
    val finalStatus: String,
    val auditLog: String
)

@Entity(tableName = "semantic_facts")
data class SemanticFactEntity(
    @PrimaryKey
    val factKey: String,
    val factValue: String,
    val category: String,
    val confidence: String, // KNOWN, OBSERVED, INFERRED, etc.
    val source: String,
    val updatedAtEpochMs: Long
)

@Entity(tableName = "procedural_workflows")
data class ProceduralWorkflowEntity(
    @PrimaryKey
    val workflowId: String,
    val intentPattern: String,
    val description: String,
    val planJson: String,
    val executionCount: Int,
    val successRate: Float,
    val lastUsedEpochMs: Long
)
