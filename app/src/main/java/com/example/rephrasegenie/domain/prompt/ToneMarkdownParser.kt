package com.example.rephrasegenie.domain.prompt

import com.example.rephrasegenie.domain.model.Tone

/**
 * Reads a built-in tone from its markdown file.
 *
 * Format:
 * ```
 * # Polite
 *
 * ## Description
 * Courteous and respectful ...
 *
 * ## Prompt
 * A courteous and respectful tone ...
 *
 * ## Example Input
 * hey can you send me that file when you get a sec
 *
 * ## Example Output
 * Could you please send me that file when you get a chance? Thank you.
 *
 * ## Metadata
 * Category: General
 * Version: 1.0
 * Icon: hand
 * Color: #0ea5e9
 * ```
 *
 * `## Prompt` is the only required section.
 */
object ToneMarkdownParser {

    private const val TITLE_KEY = "__title__"

    class ParseException(message: String) : Exception(message)

    fun parse(markdown: String): Tone {
        val sections = splitSections(markdown)

        val name = sections[TITLE_KEY]?.trim().orEmpty().ifBlank { "Untitled" }
        val description = sections["description"]?.trim().orEmpty()
        val prompt = sections["prompt"]?.trim().orEmpty()

        if (prompt.isBlank()) {
            throw ParseException("Built-in tone '$name' has no ## Prompt section.")
        }

        val metadata = parseMetadata(sections["metadata"].orEmpty())

        return Tone(
            id = Tone.builtInId(name),
            name = name,
            description = description,
            prompt = prompt,
            rawInstruction = null,
            exampleInput = sections["example input"]?.trim()?.ifBlank { null },
            exampleOutput = sections["example output"]?.trim()?.ifBlank { null },
            iconKey = metadata["icon"],
            category = metadata["category"],
            color = metadata["color"],
            version = metadata["version"] ?: "1.0",
            isBuiltIn = true,
            usageCount = 0,
            lastUsedAt = null,
        )
    }

    /**
     * `# ` starts the title. `## ` starts a section, keyed in lower case. Anything else is body
     * text for the current section. Every body is trimmed.
     */
    private fun splitSections(markdown: String): Map<String, String> {
        val sections = mutableMapOf<String, String>()
        var currentKey: String? = null
        val body = StringBuilder()

        fun flush() {
            currentKey?.let { sections[it] = body.toString().trim() }
            body.setLength(0)
        }

        markdown.replace("\r\n", "\n").split("\n").forEach { line ->
            when {
                line.startsWith("# ") -> {
                    flush()
                    currentKey = TITLE_KEY
                    body.append(line.substring(2).trim())
                }

                line.startsWith("## ") -> {
                    flush()
                    currentKey = line.substring(3).trim().lowercase()
                }

                else -> body.append(line).append('\n')
            }
        }
        flush()

        return sections
    }

    /** Metadata lines split at the first `:`. Keys are lower-cased. */
    private fun parseMetadata(block: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        block.split("\n").forEach { line ->
            val index = line.indexOf(':')
            if (index <= 0) return@forEach
            val key = line.substring(0, index).trim().lowercase()
            val value = line.substring(index + 1).trim()
            if (key.isNotEmpty() && value.isNotEmpty()) result[key] = value
        }
        return result
    }
}
