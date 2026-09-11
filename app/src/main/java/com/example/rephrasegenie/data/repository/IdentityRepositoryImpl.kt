package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.local.secure.SecureIdentityStore
import com.example.rephrasegenie.di.IoDispatcher
import com.example.rephrasegenie.domain.model.Identity
import com.example.rephrasegenie.domain.repository.IdentityRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IdentityRepositoryImpl @Inject constructor(
    private val store: SecureIdentityStore,
    @IoDispatcher private val io: CoroutineDispatcher,
) : IdentityRepository {

    override suspend fun getIdentity(): Identity? = withContext(io) { store.load() }

    override suspend fun saveIdentity(identity: Identity) = withContext(io) {
        store.save(identity)
    }

    override suspend fun clear() = withContext(io) { store.clear() }

    override fun currentApiKey(): String? = store.currentApiKey()
}
