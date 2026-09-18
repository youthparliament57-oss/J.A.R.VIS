package com.example.memory.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.memory.database.entity.EpisodicRecordEntity
import com.example.memory.database.entity.ProceduralWorkflowEntity
import com.example.memory.database.entity.SemanticFactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EpisodicMemoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: EpisodicRecordEntity)

    @Query("SELECT * FROM episodic_memory ORDER BY timestampEpochMs DESC LIMIT :limit")
    fun observeRecentRecords(limit: Int): Flow<List<EpisodicRecordEntity>>

    @Query("SELECT * FROM episodic_memory ORDER BY timestampEpochMs DESC LIMIT :limit")
    suspend fun getRecentRecords(limit: Int): List<EpisodicRecordEntity>
}

@Dao
interface SemanticFactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: SemanticFactEntity)

    @Query("SELECT * FROM semantic_facts WHERE factKey = :key LIMIT 1")
    suspend fun getFact(key: String): SemanticFactEntity?

    @Query("SELECT * FROM semantic_facts WHERE category = :category ORDER BY updatedAtEpochMs DESC")
    fun observeFactsByCategory(category: String): Flow<List<SemanticFactEntity>>

    @Query("SELECT * FROM semantic_facts ORDER BY updatedAtEpochMs DESC")
    fun observeAllFacts(): Flow<List<SemanticFactEntity>>

    @Query("DELETE FROM semantic_facts WHERE factKey = :key")
    suspend fun deleteFact(key: String)
}

@Dao
interface ProceduralWorkflowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkflow(workflow: ProceduralWorkflowEntity)

    @Query("SELECT * FROM procedural_workflows WHERE intentPattern LIKE '%' || :query || '%' LIMIT 1")
    suspend fun findMatchingWorkflow(query: String): ProceduralWorkflowEntity?

    @Query("SELECT * FROM procedural_workflows ORDER BY executionCount DESC")
    fun observeWorkflows(): Flow<List<ProceduralWorkflowEntity>>
}
