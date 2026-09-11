package com.example.rephrasegenie.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.InstalledApp
import com.example.rephrasegenie.domain.repository.InstalledAppsRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BlockedAppsUiState(
    val loading: Boolean = true,
    val query: String = "",
    val apps: List<InstalledApp> = emptyList(),
    val blocked: Set<String> = emptySet(),
) {
    /** Matched on both the app name and the package, so "gmail" and "com.google" both work. */
    val visibleApps: List<InstalledApp>
        get() {
            val needle = query.trim()
            if (needle.isEmpty()) return apps
            return apps.filter {
                it.label.contains(needle, ignoreCase = true) ||
                    it.packageName.contains(needle, ignoreCase = true)
            }
        }
}

/**
 * The searchable list of installed apps behind the "Add app" button.
 *
 * The user should never have to type a package name, which is why this lists real app names and
 * icons — the same change the Windows app made after starting out with a file browser.
 */
@HiltViewModel
class BlockedAppsViewModel @Inject constructor(
    private val installedApps: InstalledAppsRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BlockedAppsUiState())
    val state: StateFlow<BlockedAppsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = installedApps.launchableApps()
            _state.update { it.copy(loading = false, apps = apps) }
        }
        viewModelScope.launch {
            settings.observeSettings().collect { current ->
                _state.update { it.copy(blocked = current.blockedApps) }
            }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value) }
    }

    fun toggle(packageName: String) {
        viewModelScope.launch {
            settings.update { current ->
                val blocked = if (packageName in current.blockedApps) {
                    current.blockedApps - packageName
                } else {
                    current.blockedApps + packageName
                }
                current.copy(blockedApps = blocked)
            }
        }
    }
}
