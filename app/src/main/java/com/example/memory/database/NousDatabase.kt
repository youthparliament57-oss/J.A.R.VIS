package com.example.memory.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.memory.database.dao.EpisodicMemoryDao
import com.example.memory.database.dao.ProceduralWorkflowDao
import com.example.memory.database.dao.SemanticFactDao
import com.example.memory.database.entity.EpisodicRecordEntity
import com.example.memory.database.entity.ProceduralWorkflowEntity
import com.example.memory.database.entity.SemanticFactEntity

@Database(
    entities = [
        EpisodicRecordEntity::class,
        SemanticFactEntity::class,
        ProceduralWorkflowEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class NousDatabase : RoomDatabase() {
    abstract fun episodicMemoryDao(): EpisodicMemoryDao
    abstract fun semanticFactDao(): SemanticFactDao
    abstract fun proceduralWorkflowDao(): ProceduralWorkflowDao

    companion object {
        @Volatile
        private var INSTANCE: NousDatabase? = null

        fun getInstance(context: Context): NousDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NousDatabase::class.java,
                    "nous_master_memory.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
