package com.example.rephrasegenie

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.repository.IdentityRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import com.example.rephrasegenie.domain.usecase.SeedBlockedAppsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val loading: Boolean = true,
    val hasIdentity: Boolean = false,
    val settings: AppSettings = AppSettings(),
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val identityRepository: IdentityRepository,
    private val settingsRepository: SettingsRepository,
    private val toneRepository: ToneRepository,
    private val seedBlockedApps: SeedBlockedAppsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(MainUiState())
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Reads the tone markdown files into the database on first run.
            toneRepository.seedBuiltInsIfNeeded()

            val identity = identityRepository.getIdentity()
            _state.value = _state.value.copy(loading = false, hasIdentity = identity != null)
        }
        viewModelScope.launch {
            // First run only: blocks whichever banking and password-manager apps are installed.
            seedBlockedApps()
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.value = _state.value.copy(settings = settings)
            }
        }
    }
}
