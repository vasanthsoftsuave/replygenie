package com.example.rephrasegenie.ui.screen.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.AppSettings
import com.example.rephrasegenie.domain.model.GenerationRecord
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.repository.HistoryRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import com.example.rephrasegenie.domain.usecase.RephraseStage
import com.example.rephrasegenie.domain.usecase.RephraseTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/** One row of the "Recent rephrases" list. Lengths only — the text itself is never stored. */
data class RecentRephrase(
    val id: String,
    val toneName: String,
    val toneColor: String?,
    val timestamp: Instant,
    val inputLength: Int,
    val outputLength: Int,
    val status: String,
    val durationMs: Int,
) {
    val succeeded: Boolean get() = status == GenerationRecord.STATUS_SUCCESS
}

data class HomeUiState(
    val inputText: String = "",
    val tones: List<Tone> = emptyList(),
    val selectedToneId: String? = null,
    val settings: AppSettings = AppSettings(),
    val stage: RephraseStage? = null,
    val result: String? = null,
    val originalText: String? = null,
    val error: String? = null,
    val recent: List<RecentRephrase> = emptyList(),
) {
    val isWorking: Boolean get() = stage != null
    val canRephrase: Boolean
        get() = inputText.isNotBlank() && selectedToneId != null && !isWorking

    /** Shown next to the spinner so the user knows why several calls are happening. */
    val stageLabel: String?
        get() = when (stage) {
            RephraseStage.REPHRASING -> "Rephrasing…"
            RephraseStage.CHECKING -> "Checking result…"
            null -> null
        }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rephraseText: RephraseTextUseCase,
    private val toneRepository: ToneRepository,
    private val settingsRepository: SettingsRepository,
    private val historyRepository: HistoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            toneRepository.seedBuiltInsIfNeeded()
        }
        observeRecent()
        viewModelScope.launch {
            toneRepository.observeTones().collect { tones ->
                _state.update { current ->
                    val selected = current.selectedToneId
                        ?: current.settings.defaultToneId
                        ?: tones.firstOrNull()?.id
                    current.copy(tones = tones, selectedToneId = selected)
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.observeSettings().collect { settings ->
                _state.update { current ->
                    current.copy(
                        settings = settings,
                        selectedToneId = current.selectedToneId
                            ?: settings.defaultToneId
                            ?: current.tones.firstOrNull()?.id,
                    )
                }
            }
        }
    }

    /**
     * Joined with the tone list so each row can show the tone name and colour, not a raw id.
     * A tone that has since been deleted falls back to a placeholder rather than vanishing.
     */
    private fun observeRecent() {
        viewModelScope.launch {
            combine(
                historyRepository.observeRecent(RECENT_LIMIT),
                toneRepository.observeTones(),
            ) { records, tones ->
                val byId = tones.associateBy { it.id }
                records.map { record ->
                    val tone = byId[record.toneId]
                    RecentRephrase(
                        id = record.id,
                        toneName = tone?.name ?: "Deleted tone",
                        toneColor = tone?.color,
                        timestamp = record.timestamp,
                        inputLength = record.inputLength,
                        outputLength = record.outputLength,
                        status = record.status,
                        durationMs = record.durationMs,
                    )
                }
            }.collect { rows -> _state.update { it.copy(recent = rows) } }
        }
    }

    fun onInputChange(value: String) {
        _state.update { it.copy(inputText = value, error = null) }
    }

    fun onToneSelected(toneId: String) {
        _state.update { it.copy(selectedToneId = toneId) }
    }

    fun setDefaultTone(toneId: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(defaultToneId = toneId) }
        }
    }

    fun rephrase() {
        val current = _state.value
        val tone = current.tones.firstOrNull { it.id == current.selectedToneId } ?: return
        val text = current.inputText

        job?.cancel()
        job = viewModelScope.launch {
            _state.update { it.copy(stage = RephraseStage.REPHRASING, error = null, result = null) }
            try {
                val result = rephraseText(tone, text) { stage ->
                    _state.update { it.copy(stage = stage) }
                }
                _state.update {
                    it.copy(stage = null, result = result.text, originalText = text)
                }
            } catch (e: RephraseError) {
                _state.update { it.copy(stage = null, error = e.userMessage) }
            }
        }
    }

    fun cancel() {
        job?.cancel()
        _state.update { it.copy(stage = null) }
    }

    /** Feeds the result back into the input so rephrases can be chained. */
    fun useResultAsInput() {
        _state.update { current ->
            current.copy(
                inputText = current.result.orEmpty(),
                result = null,
                originalText = null,
            )
        }
    }

    /** Puts the original text back, so a rephrase is never a one-way door. */
    fun undo() {
        _state.update { current ->
            current.copy(
                inputText = current.originalText ?: current.inputText,
                result = null,
                originalText = null,
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private companion object {
        const val RECENT_LIMIT = 10
    }
}
