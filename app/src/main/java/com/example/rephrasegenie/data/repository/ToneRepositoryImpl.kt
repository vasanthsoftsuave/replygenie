package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.local.tone.BuiltInToneLoader
import com.example.rephrasegenie.data.local.db.ToneDao
import com.example.rephrasegenie.data.mapper.toDomain
import com.example.rephrasegenie.data.mapper.toEntity
import com.example.rephrasegenie.di.IoDispatcher
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.repository.ToneRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToneRepositoryImpl @Inject constructor(
    private val toneDao: ToneDao,
    private val builtInLoader: BuiltInToneLoader,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ToneRepository {

    override fun observeTones(): Flow<List<Tone>> =
        toneDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getTone(id: String): Tone? = withContext(io) {
        toneDao.getById(id)?.toDomain()
    }

    override suspend fun saveCustomTone(tone: Tone) = withContext(io) {
        require(!tone.isBuiltIn) { "Built-in tones are read-only." }
        toneDao.upsert(tone.toEntity())
    }

    override suspend fun deleteCustomTone(id: String) = withContext(io) {
        toneDao.deleteCustom(id)
    }

    override suspend fun bumpUsage(id: String) = withContext(io) {
        toneDao.bumpUsage(id, Instant.now().toEpochMilli())
    }

    /**
     * Reads the markdown files into the database on first run.
     *
     * Uses upsert, so re-running it refreshes the built-in wording after an app update while
     * leaving custom tones untouched.
     */
    override suspend fun seedBuiltInsIfNeeded() = withContext(io) {
        val builtIns = builtInLoader.load()
        if (builtIns.isEmpty()) return@withContext

        val existing = toneDao.countBuiltIns()
        if (existing == builtIns.size) return@withContext

        toneDao.upsertAll(builtIns.map { it.toEntity() })
    }
}

