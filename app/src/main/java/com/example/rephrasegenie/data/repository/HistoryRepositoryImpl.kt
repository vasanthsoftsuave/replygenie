package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.local.db.GenerationDao
import com.example.rephrasegenie.data.mapper.toDomain
import com.example.rephrasegenie.data.mapper.toEntity
import com.example.rephrasegenie.di.IoDispatcher
import com.example.rephrasegenie.domain.model.GenerationRecord
import com.example.rephrasegenie.domain.repository.HistoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val dao: GenerationDao,
    @IoDispatcher private val io: CoroutineDispatcher,
) : HistoryRepository {

    override suspend fun record(record: GenerationRecord) = withContext(io) {
        dao.insert(record.toEntity())
    }

    override fun observeRecent(limit: Int): Flow<List<GenerationRecord>> =
        dao.observeRecent(limit).map { list -> list.map { it.toDomain() } }
}
