package com.example.memory.repository

import com.example.core.model.EpistemicConfidence
import com.example.memory.database.NousDatabase
import com.example.memory.database.entity.EpisodicRecordEntity
import com.example.memory.database.entity.ProceduralWorkflowEntity
import com.example.memory.database.entity.SemanticFactEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface MemoryRepository {
    suspend fun recordEpisode(
        sessionId: String,
        userGoal: String,
        planSummary: String,
        stepsExecutedCount: Int,
        finalStatus: String,
        auditLog: String
    )
    fun observeRecentEpisodes(limit: Int = 10): Flow<List<EpisodicRecordEntity>>

    suspend fun storeFact(
        key: String,
        value: String,
        category: String,
        confidence: EpistemicConfidence = EpistemicConfidence.KNOWN,
        source: String = "USER_INPUT"
    )
    suspend fun getFact(key: String): SemanticFactEntity?
    fun observeAllFacts(): Flow<List<SemanticFactEntity>>
    suspend fun deleteFact(key: String)

    suspend fun saveWorkflow(
        intentPattern: String,
        description: String,
        planJson: String
    )
    suspend fun findMatchingWorkflow(prompt: String): ProceduralWorkflowEntity?
}

class MemoryRepositoryImpl(private val database: NousDatabase) : MemoryRepository {
    private val episodicDao = database.episodicMemoryDao()
    private val semanticDao = database.semanticFactDao()
    private val proceduralDao = database.proceduralWorkflowDao()

    override suspend fun recordEpisode(
        sessionId: String,
        userGoal: String,
        planSummary: String,
        stepsExecutedCount: Int,
        finalStatus: String,
        auditLog: String
    ) {
        val entity = EpisodicRecordEntity(
            recordId = UUID.randomUUID().toString(),
            sessionId = sessionId,
            timestampEpochMs = System.currentTimeMillis(),
            userGoal = userGoal,
            planSummary = planSummary,
            stepsExecutedCount = stepsExecutedCount,
            finalStatus = finalStatus,
            auditLog = auditLog
        )
        episodicDao.insertRecord(entity)
    }

    override fun observeRecentEpisodes(limit: Int): Flow<List<EpisodicRecordEntity>> {
        return episodicDao.observeRecentRecords(limit)
    }

    override suspend fun storeFact(
        key: String,
        value: String,
        category: String,
        confidence: EpistemicConfidence,
        source: String
    ) {
        val entity = SemanticFactEntity(
            factKey = key,
            factValue = value,
            category = category,
            confidence = confidence.name,
            source = source,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        semanticDao.insertFact(entity)
    }

    override suspend fun getFact(key: String): SemanticFactEntity? {
        return semanticDao.getFact(key)
    }

    override fun observeAllFacts(): Flow<List<SemanticFactEntity>> {
        return semanticDao.observeAllFacts()
    }

    override suspend fun deleteFact(key: String) {
        semanticDao.deleteFact(key)
    }

    override suspend fun saveWorkflow(
        intentPattern: String,
        description: String,
        planJson: String
    ) {
        val entity = ProceduralWorkflowEntity(
            workflowId = UUID.randomUUID().toString(),
            intentPattern = intentPattern,
            description = description,
            planJson = planJson,
            executionCount = 1,
            successRate = 1.0f,
            lastUsedEpochMs = System.currentTimeMillis()
        )
        proceduralDao.insertWorkflow(entity)
    }

    override suspend fun findMatchingWorkflow(prompt: String): ProceduralWorkflowEntity? {
        return proceduralDao.findMatchingWorkflow(prompt)
    }
}
