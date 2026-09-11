package com.example.rephrasegenie.domain.model

import java.time.Instant

/**
 * A writing style the user can rephrase text into.
 *
 * Built-in tones ship as markdown files in assets and are read-only. Custom tones are made in the
 * Tone Builder and stored in the database.
 *
 * Only [prompt] is ever sent to the AI. [rawInstruction] is what the user typed and is kept so the
 * Tone Builder can show it again for editing. [exampleInput] and [exampleOutput] are shown in the
 * UI and never sent anywhere.
 */
data class Tone(
    val id: String,
    val name: String,
    val description: String,
    val prompt: String,
    val rawInstruction: String? = null,
    val exampleInput: String? = null,
    val exampleOutput: String? = null,
    val iconKey: String? = null,
    val category: String? = null,
    val color: String? = null,
    val version: String = "1.0",
    val isBuiltIn: Boolean = false,
    val createdAt: Instant = Instant.EPOCH,
    val updatedAt: Instant = Instant.EPOCH,
    val usageCount: Int = 0,
    val lastUsedAt: Instant? = null,
) {
    companion object {
        /** Built-in ids are readable and stable, e.g. "builtin:polite". */
        fun builtInId(name: String): String = "builtin:${name.lowercase().replace(" ", "-")}"
    }
}
