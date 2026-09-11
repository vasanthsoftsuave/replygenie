package com.example.rephrasegenie.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ToneDao {

    /** Built-in tones first, then custom ones, each group by name. */
    @Query("SELECT * FROM tones ORDER BY isBuiltIn DESC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<ToneEntity>>

    @Query("SELECT * FROM tones WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ToneEntity?

    @Query("SELECT COUNT(*) FROM tones WHERE isBuiltIn = 1")
    suspend fun countBuiltIns(): Int

    @Upsert
    suspend fun upsert(tone: ToneEntity)

    @Upsert
    suspend fun upsertAll(tones: List<ToneEntity>)

    @Query("DELETE FROM tones WHERE id = :id AND isBuiltIn = 0")
    suspend fun deleteCustom(id: String)

    @Query("UPDATE tones SET usageCount = usageCount + 1, lastUsedAt = :usedAt WHERE id = :id")
    suspend fun bumpUsage(id: String, usedAt: Long)
}

@Dao
interface GenerationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: GenerationEntity)

    @Query("SELECT * FROM generations ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<GenerationEntity>>

    @Delete
    suspend fun delete(record: GenerationEntity)
}
