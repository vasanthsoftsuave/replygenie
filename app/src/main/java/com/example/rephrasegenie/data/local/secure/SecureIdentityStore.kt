package com.example.rephrasegenie.data.local.secure

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.rephrasegenie.domain.model.Identity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores the username and the user's OpenAI key, encrypted with a Keystore-backed key.
 *
 * This is the Android equivalent of the Windows app's DPAPI-encrypted identity file, and it keeps
 * the same failure behaviour: if the stored data cannot be read back, treat it as "no key saved"
 * and send the user to setup rather than crashing. That happens for real when the Keystore key is
 * invalidated, for example after the device lock is changed.
 */
@Singleton
class SecureIdentityStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private companion object {
        const val TAG = "SecureIdentityStore"
        const val FILE_NAME = "identity"
        const val KEY_USERNAME = "username"
        const val KEY_API_KEY = "openai_api_key"
    }

    /** Kept in memory so the auth interceptor never blocks on disk. */
    @Volatile
    private var cachedApiKey: String? = null

    private val prefs: SharedPreferences? by lazy {
        runCatching { createPrefs() }
            .onFailure { Log.w(TAG, "Could not open encrypted storage; treating as no identity.") }
            .getOrNull()
    }

    private fun createPrefs(): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun load(): Identity? {
        val store = prefs ?: return null
        return runCatching {
            val username = store.getString(KEY_USERNAME, null)
            val apiKey = store.getString(KEY_API_KEY, null)
            if (username.isNullOrBlank() || apiKey.isNullOrBlank()) {
                null
            } else {
                cachedApiKey = apiKey
                Identity(username, apiKey)
            }
        }.getOrElse {
            // Corrupt or undecryptable — behave as if nothing was ever saved.
            Log.w(TAG, "Stored identity could not be decrypted; treating as no identity.")
            null
        }
    }

    fun save(identity: Identity) {
        val store = prefs ?: return
        store.edit()
            .putString(KEY_USERNAME, identity.username)
            .putString(KEY_API_KEY, identity.openAiApiKey)
            .apply()
        cachedApiKey = identity.openAiApiKey
    }

    fun clear() {
        prefs?.edit()?.clear()?.apply()
        cachedApiKey = null
    }

    /**
     * Read on every network call, so a newly saved key takes effect at once with no restart.
     * The Windows app builds its client once at startup and has to ask the user to restart.
     */
    fun currentApiKey(): String? = cachedApiKey ?: load()?.openAiApiKey
}
