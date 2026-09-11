package com.example.rephrasegenie.domain.prompt

/**
 * Turns what the user typed in the Tone Builder into a style-only instruction.
 *
 * A custom tone must never be able to turn the app into a reply tool, so the raw wording is never
 * saved as the prompt and never sent to the AI as-is.
 */
object ToneInstructionCleaner {

    /**
     * Wording that reads like "write a reply" rather than "write in this style".
     *
     * This is **not** a blocklist — nothing is ever rejected because of it. It only decides whether
     * to add an extra nudge to the cleaning request below.
     */
    private val REPLY_SHAPED_KEYWORDS = listOf(
        "answer this",
        "answer the",
        "answer that",
        "give the correct answer",
        "give correct answer",
        "respond to",
        "reply to",
        "generate a response",
        "generate a reply",
        "tell them what to do",
        "tell him what to do",
        "tell her what to do",
    )

    fun containsReplyShapedLanguage(instruction: String): Boolean {
        val lowered = instruction.lowercase()
        return REPLY_SHAPED_KEYWORDS.any { lowered.contains(it) }
    }

    const val NORMALIZATION_SYSTEM_PROMPT: String =
        "You convert a user's informal style instruction into a structured tone definition " +
            "for a TEXT REWRITING tool (never a reply-generation tool). The tool only ever " +
            "rewords text the user already wrote, in a chosen style — it never answers, " +
            "responds to, or replies to anything, and never invents new facts.\n\n" +
            "Given the user's raw instruction, output ONLY a rewritten tone instruction, 1-3 " +
            "sentences, that: (1) describes the STYLE to apply (e.g. formal, warm, concise), " +
            "(2) never tells the model to answer, respond to, or reply to the text, (3) never " +
            "tells the model to add new information beyond what was already there. If the raw " +
            "instruction is itself reply-shaped (e.g. \"answer this question\", \"respond to the " +
            "customer\", \"give the correct answer\"), reinterpret it purely as a STYLE (e.g. " +
            "\"answer this professionally\" -> a professional, direct style) — never preserve any " +
            "request to answer or respond. Output only the tone instruction text itself, no " +
            "labels, headers, or quotation marks."

    private const val REPLY_SHAPED_NOTE =
        "\n\n(Note: this instruction sounds like it's asking to answer or respond to something " +
            "— reinterpret it purely as a writing style, per your instructions.)"

    fun buildUserMessage(rawInstruction: String): String =
        if (containsReplyShapedLanguage(rawInstruction)) {
            rawInstruction + REPLY_SHAPED_NOTE
        } else {
            rawInstruction
        }

    /**
     * Used when the network is down or the AI returns nothing. A tone should never end up
     * reply-shaped just because OpenAI was briefly unreachable.
     */
    fun localFallback(rawInstruction: String): String =
        "Rewrite the provided text clearly, preserving its original meaning and intent. " +
            "Style guidance: ${rawInstruction.trim()}. Do not answer, respond to, or add " +
            "information to the text — only reword it."
}
