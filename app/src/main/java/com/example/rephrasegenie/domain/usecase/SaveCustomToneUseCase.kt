package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.model.ToneColors
import com.example.rephrasegenie.domain.model.ToneDraft
import com.example.rephrasegenie.domain.repository.ToneRepository
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates, cleans and stores a custom tone.
 *
 * The colour rule is checked here rather than only in the UI, so it holds however the tone was
 * created. Two tones sharing a colour would make the bubble and the tone pills ambiguous, which is
 * the one thing the colour is for.
 */
@Singleton
class SaveCustomToneUseCase @Inject constructor(
    private val tones: ToneRepository,
    private val cleanInstruction: CleanToneInstructionUseCase,
) {

    suspend operator fun invoke(draft: ToneDraft): Tone {
        val name = draft.name.trim()
        if (name.isEmpty()) {
            throw RephraseError.Validation("Please enter a tone name.")
        }

        val color = ToneColors.normalize(draft.color)
            ?: throw RephraseError.Validation("Please pick a colour for this tone.")

        val existing = tones.observeTones().first()

        val clash = existing.firstOrNull { other ->
            other.id != draft.id && ToneColors.normalize(other.color) == color
        }
        if (clash != null) {
            throw RephraseError.Validation(
                "The color $color is already used by '${clash.name}'. Please pick a different one."
            )
        }

        val nameClash = existing.firstOrNull { other ->
            other.id != draft.id && other.name.equals(name, ignoreCase = true)
        }
        if (nameClash != null) {
            throw RephraseError.Validation("A tone called '$name' already exists.")
        }

        val previous = draft.id?.let { tones.getTone(it) }
        if (previous?.isBuiltIn == true) {
            throw RephraseError.Validation("Built-in tones cannot be edited.")
        }

        // Never store what the user typed as the prompt. Only the cleaned, style-only version is
        // ever sent to the AI; the raw wording is kept so the builder can show it again.
        val prompt = cleanInstruction(draft.rawInstruction)

        val now = Instant.now()
        val tone = Tone(
            id = draft.id ?: "custom:${UUID.randomUUID()}",
            name = name,
            description = draft.description.trim().ifEmpty { name },
            prompt = prompt,
            rawInstruction = draft.rawInstruction.trim(),
            exampleInput = draft.exampleInput.trim().ifEmpty { null },
            exampleOutput = draft.exampleOutput.trim().ifEmpty { null },
            iconKey = previous?.iconKey,
            category = previous?.category ?: "Custom",
            color = color,
            version = previous?.version ?: "1.0",
            isBuiltIn = false,
            createdAt = previous?.createdAt ?: now,
            updatedAt = now,
            usageCount = previous?.usageCount ?: 0,
            lastUsedAt = previous?.lastUsedAt,
        )

        tones.saveCustomTone(tone)
        return tone
    }
}
