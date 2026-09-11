package com.example.rephrasegenie.domain.usecase

import com.example.rephrasegenie.domain.guardrail.GuardrailPrompts
import com.example.rephrasegenie.domain.model.ChatRole
import com.example.rephrasegenie.domain.model.ChatTurn
import com.example.rephrasegenie.domain.model.GenerationRecord
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.model.RephraseResult
import com.example.rephrasegenie.domain.model.Tone
import com.example.rephrasegenie.domain.prompt.PromptBuilder
import com.example.rephrasegenie.domain.repository.HistoryRepository
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import com.example.rephrasegenie.domain.repository.SettingsRepository
import com.example.rephrasegenie.domain.repository.ToneRepository
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Lets the caller show what is happening while several calls are in flight. */
enum class RephraseStage { REPHRASING, CHECKING }

/**
 * Rephrases the user's text in a tone.
 *
 * Rules taken from the Windows app, all load-bearing:
 *
 * - The rephrase check **always** runs, even when the safety setting is off. It is about whether
 *   the tool did its job, not about content safety.
 * - Moderation only runs when the user turned it on. It is off by default.
 * - **One retry only.** If a check fails twice, nothing is returned and nothing is written back.
 *
 * Worst case with moderation on is six API calls, which is why [onStage] exists.
 *
 * [record] is off for Tone Builder previews, where the tone may not be saved yet: a history row
 * against an id that does not exist would be meaningless and the usage bump would land nowhere.
 */
@Singleton
class RephraseTextUseCase @Inject constructor(
    private val openAi: OpenAiRepository,
    private val settings: SettingsRepository,
    private val tones: ToneRepository,
    private val history: HistoryRepository,
) {

    suspend operator fun invoke(
        tone: Tone,
        draftText: String,
        record: Boolean = true,
        onStage: (RephraseStage) -> Unit = {},
    ): RephraseResult {
        val draft = draftText.trim()
        if (draft.isEmpty()) {
            throw RephraseError.Validation("There is nothing to rephrase.")
        }

        val moderationEnabled = settings.settings.value.guardrailsEnabled
        val messages = PromptBuilder.buildMessages(tone.name, tone.prompt, draft)
        val startedAt = System.currentTimeMillis()

        onStage(RephraseStage.REPHRASING)
        var completion = openAi.chat(messages)

        onStage(RephraseStage.CHECKING)
        var verdict = runChecks(draft, completion.text, moderationEnabled)

        if (verdict != null) {
            // One retry. The nudge is added to the same list, so the model sees the original
            // instruction plus the correction.
            messages.add(ChatTurn(ChatRole.USER, GuardrailPrompts.REINFORCEMENT_MESSAGE))

            onStage(RephraseStage.REPHRASING)
            completion = openAi.chat(messages)

            onStage(RephraseStage.CHECKING)
            verdict = runChecks(draft, completion.text, moderationEnabled)

            if (verdict != null) {
                if (record) {
                    val durationMs = (System.currentTimeMillis() - startedAt).toInt()
                    history.record(
                        newRecord(
                            tone = tone,
                            inputLength = draft.length,
                            outputLength = 0,
                            model = completion.model,
                            status = GenerationRecord.STATUS_BLOCKED,
                            durationMs = durationMs,
                        )
                    )
                }
                throw RephraseError.Blocked(verdict)
            }
        }

        val durationMs = (System.currentTimeMillis() - startedAt).toInt()
        if (record) {
            history.record(
                newRecord(
                    tone = tone,
                    inputLength = draft.length,
                    outputLength = completion.text.length,
                    model = completion.model,
                    status = GenerationRecord.STATUS_SUCCESS,
                    durationMs = durationMs,
                )
            )
            tones.bumpUsage(tone.id)
        }

        return RephraseResult(completion.text, completion.model, durationMs)
    }

    /** Returns the reason it failed, or null when everything passed. */
    private suspend fun runChecks(
        draft: String,
        result: String,
        moderationEnabled: Boolean,
    ): String? {
        if (moderationEnabled) {
            val flagged = openAi.moderate(result)
            if (flagged.isNotEmpty()) {
                return GuardrailPrompts.formatModerationReason(flagged)
            }
        }

        val answer = openAi.chat(
            listOf(
                ChatTurn(ChatRole.SYSTEM, GuardrailPrompts.CONFORMANCE_SYSTEM_PROMPT),
                ChatTurn(ChatRole.USER, GuardrailPrompts.buildConformanceUserMessage(draft, result)),
            )
        )

        return if (GuardrailPrompts.passesConformance(answer.text)) {
            null
        } else {
            GuardrailPrompts.REASON_CONFORMANCE
        }
    }

    private fun newRecord(
        tone: Tone,
        inputLength: Int,
        outputLength: Int,
        model: String,
        status: String,
        durationMs: Int,
    ) = GenerationRecord(
        id = UUID.randomUUID().toString(),
        toneId = tone.id,
        toneVersion = tone.version,
        timestamp = Instant.now(),
        inputLength = inputLength,
        outputLength = outputLength,
        model = model,
        status = status,
        durationMs = durationMs,
    )
}
