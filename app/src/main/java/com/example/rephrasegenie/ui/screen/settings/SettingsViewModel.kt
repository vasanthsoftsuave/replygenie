package com.example.rephrasegenie.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.model.Identity
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.ThemeMode
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.repository.IdentityRepository
import com.example.rephrasegenie.domain.repository.InstalledAppsRepository
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import com.example.rephrasegenie.domain.usecase.DeleteCustomToneUseCase
import com.example.rephrasegenie.ui.screen.setup.ConnectionTest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One blocked app as the settings list shows it: package name plus a readable label. */
data class BlockedAppRow(
    val packageName: String,
    val label: String,
)

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val tones: List<Tone> = emptyList(),
    val blockedApps: List<BlockedAppRow> = emptyList(),
    /** Kept out of [settings] so an unsaved edit never reaches the stored username. */
    val usernameDraft: String = "",
    val apiKeyDraft: String = "",
    val hasStoredKey: Boolean = false,
    val connectionTest: ConnectionTest = ConnectionTest.NONE,
    val connectionMessage: String? = null,
    val savingProfile: Boolean = false,
    val message: String? = null,
    val error: String? = null,
) {
    val customTones: List<Tone> get() = tones.filterNot { it.isBuiltIn }
    val profileChanged: Boolean
        get() = apiKeyDraft.isNotBlank() || usernameDraft.trim() != settings.username
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val toneRepository: ToneRepository,
    private val identityRepository: IdentityRepository,
    private val installedApps: InstalledAppsRepository,
    private val openAi: OpenAiRepository,
    private val deleteCustomTone: DeleteCustomToneUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    /** True once the username field has been seeded, so collecting settings stops overwriting it. */
    private var usernameSeeded = false

    init {
        viewModelScope.launch {
            _state.update { it.copy(hasStoredKey = identityRepository.getIdentity() != null) }
        }
        viewModelScope.launch {
            toneRepository.observeTones().collect { tones ->
                _state.update { it.copy(tones = tones) }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = settings,
                        usernameDraft = if (usernameSeeded) {
                            current.usernameDraft
                        } else {
                            settings.username
                        },
                    )
                }
                usernameSeeded = true
                refreshBlockedAppLabels(settings.blockedApps)
            }
        }
    }

    /**
     * Resolves package names to app names.
     *
     * An app that has since been uninstalled falls back to its package name rather than
     * disappearing, so the user can still see it and remove it.
     */
    private fun refreshBlockedAppLabels(packages: Set<String>) {
        viewModelScope.launch {
            val rows = packages
                .map { BlockedAppRow(it, installedApps.labelFor(it)) }
                .sortedBy { it.label.lowercase() }
            _state.update { it.copy(blockedApps = rows) }
        }
    }

    // -- Profile & AI ------------------------------------------------------------------------

    fun onUsernameChange(value: String) {
        _state.update { it.copy(usernameDraft = value, error = null) }
    }

    fun onApiKeyChange(value: String) {
        _state.update {
            it.copy(
                apiKeyDraft = value,
                error = null,
                connectionTest = ConnectionTest.NONE,
                connectionMessage = null,
            )
        }
    }

    /** Tests the typed key, or the stored one when the field was left blank. */
    fun testConnection() {
        viewModelScope.launch {
            _state.update {
                it.copy(connectionTest = ConnectionTest.TESTING, connectionMessage = null)
            }

            val typed = _state.value.apiKeyDraft.trim()
            val key = typed.ifBlank { identityRepository.getIdentity()?.openAiApiKey.orEmpty() }

            if (key.isBlank()) {
                _state.update {
                    it.copy(
                        connectionTest = ConnectionTest.FAILED,
                        connectionMessage = RephraseError.emptyApiKey().userMessage,
                    )
                }
                return@launch
            }

            try {
                openAi.validateApiKey(key)
                _state.update {
                    it.copy(
                        connectionTest = ConnectionTest.SUCCESS,
                        connectionMessage = "Connected successfully.",
                    )
                }
            } catch (e: RephraseError) {
                _state.update {
                    it.copy(
                        connectionTest = ConnectionTest.FAILED,
                        connectionMessage = e.userMessage,
                    )
                }
            }
        }
    }

    /**
     * Saves the username, and the API key if one was typed.
     *
     * A blank key field means "keep the current one". A key that does not work is never stored,
     * which is the same rule the setup screen follows.
     */
    fun saveProfile() {
        val current = _state.value
        val username = current.usernameDraft.trim()
        if (username.isEmpty()) {
            _state.update { it.copy(error = RephraseError.emptyUsername().userMessage) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(savingProfile = true, error = null, message = null) }
            try {
                val newKey = current.apiKeyDraft.trim()
                if (newKey.isNotEmpty()) {
                    openAi.validateApiKey(newKey)
                    identityRepository.saveIdentity(Identity(username, newKey))
                } else {
                    val existing = identityRepository.getIdentity()
                        ?: throw RephraseError.emptyApiKey()
                    identityRepository.saveIdentity(existing.copy(username = username))
                }

                settingsRepository.update { it.copy(username = username) }
                _state.update {
                    it.copy(
                        savingProfile = false,
                        apiKeyDraft = "",
                        hasStoredKey = true,
                        message = "Saved.",
                        connectionTest = ConnectionTest.NONE,
                        connectionMessage = null,
                    )
                }
            } catch (e: RephraseError) {
                _state.update { it.copy(savingProfile = false, error = e.userMessage) }
            }
        }
    }

    // -- Appearance --------------------------------------------------------------------------

    fun setThemeMode(mode: ThemeMode) = updateSettings { it.copy(themeMode = mode) }

    fun setAccentColor(hex: String) = updateSettings { it.copy(accentColor = hex) }

    // -- Behaviour ---------------------------------------------------------------------------

    fun setDefaultTone(toneId: String?) = updateSettings { it.copy(defaultToneId = toneId) }

    fun setBubbleEnabled(enabled: Boolean) = updateSettings { it.copy(bubbleEnabled = enabled) }

    fun setGuardrailsEnabled(enabled: Boolean) =
        updateSettings { it.copy(guardrailsEnabled = enabled) }

    // -- Blocked apps ------------------------------------------------------------------------

    fun unblockApp(packageName: String) =
        updateSettings { it.copy(blockedApps = it.blockedApps - packageName) }

    // -- Custom tones ------------------------------------------------------------------------

    fun deleteTone(toneId: String) {
        viewModelScope.launch {
            try {
                deleteCustomTone(toneId)
                _state.update { it.copy(message = "Tone deleted.") }
            } catch (e: RephraseError) {
                _state.update { it.copy(error = e.userMessage) }
            }
        }
    }

    fun dismissMessage() {
        _state.update { it.copy(message = null, error = null) }
    }

    /** Every change saves as soon as it is made. There is no Save or Cancel button. */
    private fun updateSettings(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
