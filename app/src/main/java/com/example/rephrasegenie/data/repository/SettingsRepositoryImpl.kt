package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.local.datastore.SettingsDataStore
import com.example.rephrasegenie.di.ApplicationScope
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: SettingsDataStore,
    @ApplicationScope private val scope: CoroutineScope,
) : SettingsRepository {

    private val _settings = MutableStateFlow(AppSettings())

    /**
     * Kept hot in memory. The accessibility service reads [isAppBlocked] on every focus event, so
     * that path must never wait on disk.
     */
    override val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    init {
        scope.launch {
            dataStore.settings.collect { _settings.value = it }
        }
    }

    override fun observeSettings(): Flow<AppSettings> = dataStore.settings

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        dataStore.update(transform)
    }

    override fun isAppBlocked(packageName: String): Boolean =
        _settings.value.blockedApps.contains(packageName)

    override suspend fun hasSeededBlockedApps(): Boolean = dataStore.hasSeededBlockedApps()

    override suspend fun markBlockedAppsSeeded() = dataStore.markBlockedAppsSeeded()
}
