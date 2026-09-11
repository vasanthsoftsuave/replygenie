package com.example.rephrasegenie.ui.screen.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.Identity
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.repository.IdentityRepository
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ConnectionTest { NONE, TESTING, SUCCESS, FAILED }

data class SetupUiState(
    val username: String = "",
    val apiKey: String = "",
    val saving: Boolean = false,
    val connectionTest: ConnectionTest = ConnectionTest.NONE,
    val connectionMessage: String? = null,
    val error: String? = null,
    val saved: Boolean = false,
) {
    val canSave: Boolean get() = username.isNotBlank() && apiKey.isNotBlank() && !saving
}

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val openAi: OpenAiRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    fun onUsernameChange(value: String) {
        _state.update { it.copy(username = value, error = null) }
    }

    fun onApiKeyChange(value: String) {
        _state.update {
            it.copy(
                apiKey = value,
                error = null,
                connectionTest = ConnectionTest.NONE,
                connectionMessage = null,
            )
        }
    }

    fun testConnection() {
        val key = _state.value.apiKey
        if (key.isBlank()) {
            _state.update {
                it.copy(
                    connectionTest = ConnectionTest.FAILED,
                    connectionMessage = RephraseError.emptyApiKey().userMessage,
                )
            }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(connectionTest = ConnectionTest.TESTING, connectionMessage = null)
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

    /** The key is checked against OpenAI before it is stored. A key that fails is never saved. */
    fun save() {
        val current = _state.value
        if (current.username.isBlank()) {
            _state.update { it.copy(error = RephraseError.emptyUsername().userMessage) }
            return
        }
        if (current.apiKey.isBlank()) {
            _state.update { it.copy(error = RephraseError.emptyApiKey().userMessage) }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                openAi.validateApiKey(current.apiKey)
                identityRepository.saveIdentity(
                    Identity(current.username.trim(), current.apiKey.trim())
                )
                settings.update { it.copy(username = current.username.trim()) }
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: RephraseError) {
                _state.update { it.copy(saving = false, error = e.userMessage) }
            }
        }
    }
}
