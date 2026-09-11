package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deletes a custom tone, and clears the default tone setting if it pointed at this one.
 *
 * Without that second step the bubble would keep a default tone id that no longer resolves, and
 * tapping it would silently do nothing.
 */
@Singleton
class DeleteCustomToneUseCase @Inject constructor(
    private val tones: ToneRepository,
    private val settings: SettingsRepository,
) {
    suspend operator fun invoke(toneId: String) {
        val tone = tones.getTone(toneId)
            ?: throw RephraseError.Validation("That tone no longer exists.")

        if (tone.isBuiltIn) {
            throw RephraseError.Validation("Built-in tones cannot be deleted.")
        }

        tones.deleteCustomTone(toneId)

        if (settings.settings.value.defaultToneId == toneId) {
            settings.update { it.copy(defaultToneId = null) }
        }
    }
}
