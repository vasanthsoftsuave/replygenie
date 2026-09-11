package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.model.ChatRole
import com.example.rephrasegenie.domain.model.ChatTurn
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.prompt.ToneInstructionCleaner
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns what the user typed in the Tone Builder into a style-only instruction.
 *
 * A custom tone must never be able to turn the app into a reply tool, so the raw wording is never
 * saved as the prompt and never sent to the AI as-is.
 *
 * Nothing is ever rejected. Wording that reads like "write a reply" only adds a nudge to the
 * cleaning request; and if OpenAI cannot be reached, a local wrapping is used instead. A tone
 * should never end up reply-shaped just because the network was briefly down.
 */
@Singleton
class CleanToneInstructionUseCase @Inject constructor(
    private val openAi: OpenAiRepository,
) {
    suspend operator fun invoke(rawInstruction: String): String {
        val raw = rawInstruction.trim()
        if (raw.isEmpty()) {
            throw RephraseError.Validation("Please say how the text should sound.")
        }

        val cleaned = try {
            openAi.chat(
                listOf(
                    ChatTurn(ChatRole.SYSTEM, ToneInstructionCleaner.NORMALIZATION_SYSTEM_PROMPT),
                    ChatTurn(ChatRole.USER, ToneInstructionCleaner.buildUserMessage(raw)),
                )
            ).text.trim()
        } catch (e: RephraseError) {
            ""
        }

        return cleaned.ifBlank { ToneInstructionCleaner.localFallback(raw) }
    }
}
