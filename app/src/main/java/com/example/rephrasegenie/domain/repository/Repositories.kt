package com.example.rephrasegenie.domain.repository

import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.model.ChatTurn
import com.example.rephrasegenie.domain.model.ChatCompletion
import com.example.rephrasegenie.domain.model.GenerationRecord
import com.example.rephrasegenie.domain.model.Identity
import com.example.rephrasegenie.domain.model.InstalledApp
import com.example.rephrasegenie.domain.model.Tone
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface ToneRepository {
    fun observeTones(): Flow<List<Tone>>
    suspend fun getTone(id: String): Tone?
    suspend fun saveCustomTone(tone: Tone)
    suspend fun deleteCustomTone(id: String)
    suspend fun bumpUsage(id: String)
    /** Reads the built-in markdown files into the database. Safe to call more than once. */
    suspend fun seedBuiltInsIfNeeded()
}

interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    fun observeSettings(): Flow<AppSettings>
    suspend fun update(transform: (AppSettings) -> AppSettings)
    /**
     * Read on every focus event, so it must never touch disk. Backed by an in-memory value kept
     * in step with the stored settings.
     */
    fun isAppBlocked(packageName: String): Boolean

    /** True once the first-run banking / password-manager defaults have been filled in. */
    suspend fun hasSeededBlockedApps(): Boolean
    suspend fun markBlockedAppsSeeded()
}

interface IdentityRepository {
    /** Null when no key is saved, or when the stored one could not be decrypted. */
    suspend fun getIdentity(): Identity?
    suspend fun saveIdentity(identity: Identity)
    suspend fun clear()
    /** Read by the auth interceptor on every request, so a new key takes effect at once. */
    fun currentApiKey(): String?
}

interface OpenAiRepository {
    suspend fun chat(messages: List<ChatTurn>): ChatCompletion
    /** Returns the flagged categories with their scores. Empty when nothing was flagged. */
    suspend fun moderate(text: String): List<Pair<String, Float>>
    /** Cheap check that a key works. Throws a [com.example.rephrasegenie.domain.model.RephraseError]. */
    suspend fun validateApiKey(apiKey: String)
}

interface HistoryRepository {
    suspend fun record(record: GenerationRecord)
    fun observeRecent(limit: Int = 20): Flow<List<GenerationRecord>>
}

/**
 * The apps the user can open, for the blocked-apps picker.
 *
 * Backed by queryIntentActivities for ACTION_MAIN + CATEGORY_LAUNCHER — deliberately not the
 * restricted QUERY_ALL_PACKAGES permission (§4.5).
 */
interface InstalledAppsRepository {
    /** Sorted by label, our own app left out. */
    suspend fun launchableApps(): List<InstalledApp>
    /** The stored name for a package, or the package name itself if it is no longer installed. */
    suspend fun labelFor(packageName: String): String
}
