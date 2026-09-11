package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.local.apps.InstalledAppsProvider
import com.example.rephrasegenie.di.IoDispatcher
import com.example.rephrasegenie.domain.model.InstalledApp
import com.example.rephrasegenie.domain.repository.InstalledAppsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches the app list for the lifetime of the process.
 *
 * Reading it means loading and sorting a few hundred labels, which is slow enough to show on
 * screen, and the set barely changes while the app is open.
 */
@Singleton
class InstalledAppsRepositoryImpl @Inject constructor(
    private val provider: InstalledAppsProvider,
    @IoDispatcher private val io: CoroutineDispatcher,
) : InstalledAppsRepository {

    private var cached: List<InstalledApp>? = null

    override suspend fun launchableApps(): List<InstalledApp> = withContext(io) {
        cached ?: provider.launchableApps().also { cached = it }
    }

    override suspend fun labelFor(packageName: String): String = withContext(io) {
        cached?.firstOrNull { it.packageName == packageName }?.label
            ?: provider.labelFor(packageName)
    }
}
