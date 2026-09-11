package com.example.rephrasegenie.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private object Keys {
        val USERNAME = stringPreferencesKey("username")
        val DEFAULT_TONE_ID = stringPreferencesKey("default_tone_id")
        val BUBBLE_ENABLED = booleanPreferencesKey("bubble_enabled")
        val GUARDRAILS_ENABLED = booleanPreferencesKey("guardrails_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val BLOCKED_APPS = stringSetPreferencesKey("blocked_apps")

        /** Not a user setting: it only stops the first-run defaults being filled in twice. */
        val BLOCKED_APPS_SEEDED = booleanPreferencesKey("blocked_apps_seeded")
    }

    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { throwable ->
            // A corrupt file should send the user to defaults, not crash the app.
            if (throwable is IOException) emit(emptyPreferences()) else throw throwable
        }
        .map { prefs ->
            AppSettings(
                username = prefs[Keys.USERNAME].orEmpty(),
                defaultToneId = prefs[Keys.DEFAULT_TONE_ID],
                bubbleEnabled = prefs[Keys.BUBBLE_ENABLED] ?: true,
                guardrailsEnabled = prefs[Keys.GUARDRAILS_ENABLED] ?: false,
                themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
                accentColor = prefs[Keys.ACCENT_COLOR] ?: AppSettings.DEFAULT_ACCENT,
                blockedApps = prefs[Keys.BLOCKED_APPS] ?: emptySet(),
            )
        }

    suspend fun hasSeededBlockedApps(): Boolean =
        context.dataStore.data.first()[Keys.BLOCKED_APPS_SEEDED] ?: false

    suspend fun markBlockedAppsSeeded() {
        context.dataStore.edit { prefs -> prefs[Keys.BLOCKED_APPS_SEEDED] = true }
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                username = prefs[Keys.USERNAME].orEmpty(),
                defaultToneId = prefs[Keys.DEFAULT_TONE_ID],
                bubbleEnabled = prefs[Keys.BUBBLE_ENABLED] ?: true,
                guardrailsEnabled = prefs[Keys.GUARDRAILS_ENABLED] ?: false,
                themeMode = ThemeMode.fromName(prefs[Keys.THEME_MODE]),
                accentColor = prefs[Keys.ACCENT_COLOR] ?: AppSettings.DEFAULT_ACCENT,
                blockedApps = prefs[Keys.BLOCKED_APPS] ?: emptySet(),
            )
            val updated = transform(current)

            prefs[Keys.USERNAME] = updated.username
            updated.defaultToneId
                ?.let { prefs[Keys.DEFAULT_TONE_ID] = it }
                ?: prefs.remove(Keys.DEFAULT_TONE_ID)
            prefs[Keys.BUBBLE_ENABLED] = updated.bubbleEnabled
            prefs[Keys.GUARDRAILS_ENABLED] = updated.guardrailsEnabled
            prefs[Keys.THEME_MODE] = updated.themeMode.name
            prefs[Keys.ACCENT_COLOR] = updated.accentColor
            prefs[Keys.BLOCKED_APPS] = updated.blockedApps
        }
    }
}
