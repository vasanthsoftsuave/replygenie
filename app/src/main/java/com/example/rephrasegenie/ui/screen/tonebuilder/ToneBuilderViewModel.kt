package com.example.rephrasegenie.ui.screen.tonebuilder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.model.ToneColors
import com.example.rephrasegenie.domain.model.ToneDraft
import com.example.rephrasegenie.domain.repository.ToneRepository
import com.example.rephrasegenie.domain.usecase.RephraseStage
import com.example.rephrasegenie.domain.usecase.SaveCustomToneUseCase
import com.example.rephrasegenie.domain.usecase.TestToneUseCase
import com.example.rephrasegenie.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ToneBuilderUiState(
    val draft: ToneDraft = ToneDraft(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val allTones: List<Tone> = emptyList(),
    val sampleText: String = DEFAULT_SAMPLE,
    val testStage: RephraseStage? = null,
    val testResult: String? = null,
    val cleanedPrompt: String? = null,
    val error: String? = null,
) {
    /** Colour to tone name, for every colour already spoken for by a different tone. */
    val takenColors: Map<String, String>
        get() = allTones
            .filter { it.id != draft.id }
            .mapNotNull { tone -> ToneColors.normalize(tone.color)?.let { hex -> hex to tone.name } }
            .toMap()

    val isEditing: Boolean get() = draft.id != null
    val isTesting: Boolean get() = testStage != null
    val canSave: Boolean
        get() = draft.name.isNotBlank() &&
            draft.rawInstruction.isNotBlank() &&
            draft.color != null &&
            !saving

    val testStageLabel: String?
        get() = when (testStage) {
            RephraseStage.REPHRASING -> "Rephrasing…"
            RephraseStage.CHECKING -> "Checking result…"
            null -> null
        }

    companion object {
        const val DEFAULT_SAMPLE = "hey can you send me that file when you get a sec"
    }
}

/**
 * Builds or edits a custom tone.
 *
 * The colour rule is enforced in [SaveCustomToneUseCase], not here — this only dims the swatches
 * so the user can see the rule before they hit it.
 */
@HiltViewModel
class ToneBuilderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tones: ToneRepository,
    private val saveCustomTone: SaveCustomToneUseCase,
    private val testTone: TestToneUseCase,
) : ViewModel() {

    private val toneId: String? = savedStateHandle.get<String>(Routes.ARG_TONE_ID)
        ?.takeIf { it.isNotBlank() && it != Routes.NEW_TONE }

    private val _state = MutableStateFlow(ToneBuilderUiState())
    val state: StateFlow<ToneBuilderUiState> = _state.asStateFlow()

    private var testJob: Job? = null

    init {
        viewModelScope.launch {
            val existing = toneId?.let { tones.getTone(it) }
            _state.update { current ->
                current.copy(
                    loading = false,
                    draft = existing?.let(ToneDraft::from) ?: ToneDraft(),
                )
            }
        }
        viewModelScope.launch {
            tones.observeTones().collect { all -> _state.update { it.copy(allTones = all) } }
        }
    }

    fun onNameChange(value: String) = editDraft { it.copy(name = value) }

    fun onDescriptionChange(value: String) = editDraft { it.copy(description = value) }

    fun onInstructionChange(value: String) = editDraft { it.copy(rawInstruction = value) }

    fun onExampleInputChange(value: String) = editDraft { it.copy(exampleInput = value) }

    fun onExampleOutputChange(value: String) = editDraft { it.copy(exampleOutput = value) }

    fun onColorSelected(hex: String) = editDraft { it.copy(color = hex) }

    fun onSampleTextChange(value: String) {
        _state.update { it.copy(sampleText = value) }
    }

    /**
     * Runs the tone as it will really behave: the instruction is cleaned first, exactly as saving
     * would clean it, so the preview is not better than the saved tone.
     */
    fun test() {
        val current = _state.value
        testJob?.cancel()
        testJob = viewModelScope.launch {
            _state.update {
                it.copy(testStage = RephraseStage.REPHRASING, testResult = null, error = null)
            }
            try {
                val result = testTone(current.draft, current.sampleText) { stage ->
                    _state.update { it.copy(testStage = stage) }
                }
                _state.update {
                    it.copy(
                        testStage = null,
                        testResult = result.output,
                        cleanedPrompt = result.cleanedPrompt,
                    )
                }
            } catch (e: RephraseError) {
                _state.update { it.copy(testStage = null, error = e.userMessage) }
            }
        }
    }

    fun cancelTest() {
        testJob?.cancel()
        _state.update { it.copy(testStage = null) }
    }

    fun save() {
        val draft = _state.value.draft
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            try {
                saveCustomTone(draft)
                _state.update { it.copy(saving = false, saved = true) }
            } catch (e: RephraseError) {
                _state.update { it.copy(saving = false, error = e.userMessage) }
            }
        }
    }

    private fun editDraft(transform: (ToneDraft) -> ToneDraft) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }
}
