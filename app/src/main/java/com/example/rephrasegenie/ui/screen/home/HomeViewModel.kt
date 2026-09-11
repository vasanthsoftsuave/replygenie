package com.example.rephrasegenie.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val settings: AppSettings = AppSettings(),
)

/**
 * The Home screen only reports on the bubble and switches it on and off. Rephrasing itself
 * happens in the overlay, in whatever app the user is typing in.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { it.copy(settings = settings) }
            }
        }
    }

    /**
     * Read on every focus event by the accessibility service, so turning it off takes effect at
     * the next tap into a text field.
     */
    fun setBubbleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(bubbleEnabled = enabled) }
        }
    }
}
