package com.example.rephrasegenie.domain.model

/**
 * What the user filled in on the Tone Builder, before any of it has been cleaned or saved.
 *
 * [rawInstruction] is what they typed. It is never saved as the tone's prompt and never sent to
 * the AI as-is — [com.example.rephrasegenie.domain.usecase.CleanToneInstructionUseCase] turns it
 * into a style-only description first.
 */
data class ToneDraft(
    /** Null for a new tone, the existing id when editing one. */
    val id: String? = null,
    val name: String = "",
    val description: String = "",
    val rawInstruction: String = "",
    val exampleInput: String = "",
    val exampleOutput: String = "",
    val color: String? = null,
) {
    companion object {
        fun from(tone: Tone) = ToneDraft(
            id = tone.id,
            name = tone.name,
            description = tone.description,
            rawInstruction = tone.rawInstruction ?: tone.prompt,
            exampleInput = tone.exampleInput.orEmpty(),
            exampleOutput = tone.exampleOutput.orEmpty(),
            color = tone.color,
        )
    }
}
