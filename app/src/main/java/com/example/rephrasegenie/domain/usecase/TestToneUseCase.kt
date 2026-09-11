package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.model.ToneDraft
import javax.inject.Inject
import javax.inject.Singleton

/** What a Tone Builder test produced, plus the cleaned prompt that was actually used. */
data class ToneTestResult(
    val cleanedPrompt: String,
    val output: String,
)

/**
 * Runs a tone the user is still writing against some sample text.
 *
 * It cleans the instruction first, exactly as saving would, so what the user sees here is what
 * they will get once the tone is saved. Testing the raw wording would show a different — and
 * usually better — result than the saved tone can ever produce.
 *
 * Nothing is recorded: the tone may not exist yet, so a history row against its id would be
 * meaningless and a usage bump would land nowhere.
 */
@Singleton
class TestToneUseCase @Inject constructor(
    private val cleanInstruction: CleanToneInstructionUseCase,
    private val rephraseText: RephraseTextUseCase,
) {
    suspend operator fun invoke(
        draft: ToneDraft,
        sampleText: String,
        onStage: (RephraseStage) -> Unit = {},
    ): ToneTestResult {
        val sample = sampleText.trim()
        if (sample.isEmpty()) {
            throw RephraseError.Validation("Type some sample text to test with.")
        }

        val cleanedPrompt = cleanInstruction(draft.rawInstruction)

        val previewTone = Tone(
            id = draft.id ?: "custom:preview",
            name = draft.name.trim().ifEmpty { "Custom" },
            description = draft.description.trim(),
            prompt = cleanedPrompt,
            color = draft.color,
            isBuiltIn = false,
        )

        val result = rephraseText(previewTone, sample, record = false, onStage = onStage)
        return ToneTestResult(cleanedPrompt = cleanedPrompt, output = result.text)
    }
}
