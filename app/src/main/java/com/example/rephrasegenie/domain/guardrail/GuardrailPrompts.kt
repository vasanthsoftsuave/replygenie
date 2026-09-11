package com.example.rephrasegenie.domain.guardrail

/**
 * The two checks that run on a result before it is written back.
 *
 * The conformance prompt below is long on purpose. Shorter and stricter wording wrongly rejected
 * about a third of perfectly good rewrites during the Windows app's testing, and the model needs
 * the worked leave-request example to tell "asking firmly" apart from "actually agreeing".
 * Do not shorten it.
 */
object GuardrailPrompts {

    const val CONFORMANCE_SYSTEM_PROMPT: String =
        "You are a QA checker for a rewrite tool. You will be shown a DRAFT message and a " +
            "REWRITE of it in a different tone. The rewrite's job is to reword the draft in the " +
            "new tone while preserving its core meaning and intent (a question stays a request for " +
            "the same thing, a statement stays the same statement) and its speaker/direction (the " +
            "same sender is still saying it to the same recipient - the rewrite must not switch to " +
            "sound like the recipient's response). Tone-appropriate phrasing - an apology, greeting, " +
            "endearment, politeness padding, urgency framing, or a firmer/more assertive or demanding " +
            "register (e.g. \"please consider\" becoming \"I expect\"/\"this is non-negotiable\") - is " +
            "EXPECTED and is NOT a violation on its own, even if it adds words or sounds more forceful, " +
            "as long as the sender is still just asking/stating the same thing more forcefully, not " +
            "actually granting, resolving, or answering it. For example: DRAFT \"I would be truly " +
            "grateful if you could grant me leave from 13/08 to 14/08\" -> REWRITE \"I expect you to " +
            "grant me leave from 13/08 to 14/08. Your approval is required\" is OK - the sender is " +
            "still demanding the SAME leave, it has not actually been granted. It would only be a " +
            "VIOLATION if the rewrite instead said something like \"Your leave from 13/08 to 14/08 is " +
            "approved\" - THAT is the recipient's decision, not the sender's request, however phrased. " +
            "This applies to full letters/emails too: if the DRAFT already has a greeting (\"Dear " +
            "...\") and a sign-off (\"Warm regards, ...\"), the REWRITE having the same greeting/" +
            "sign-off is just it mirroring the draft's own format, not evidence of a separate reply " +
            "message. Only respond VIOLATION if the rewrite actually replies to the draft, answers a " +
            "question it asked, fulfils/grants a request it made (from the recipient's perspective, " +
            "not the sender's), or invents new factual claims (names, dates, numbers, commitments) " +
            "that weren't in the draft. Otherwise respond OK. Respond with exactly one word: OK or " +
            "VIOLATION."

    /** Sent once when a check fails, before the single retry. */
    const val REINFORCEMENT_MESSAGE: String =
        "Your previous rewrite did not meet the requirements — it must ONLY reword the " +
            "draft in the target tone, without replying to it, answering it, or adding " +
            "information that wasn't in it. Try again."

    fun buildConformanceUserMessage(draft: String, rewritten: String): String =
        "DRAFT:\n$draft\n\nREWRITE:\n$rewritten"

    /**
     * The checker answers in one word. Anything that is not "OK" — including an empty answer —
     * counts as a failure.
     */
    fun passesConformance(answer: String): Boolean =
        answer.trim().startsWith("OK", ignoreCase = true)

    /** Reason strings stored with a blocked record. */
    const val REASON_CONFORMANCE = "rewrite_conformance"
    const val REASON_MODERATION_PREFIX = "moderation:"

    /**
     * Formats flagged moderation categories, highest score first, e.g.
     * "moderation:hate=92%,violence=61%".
     */
    fun formatModerationReason(categories: List<Pair<String, Float>>): String {
        val formatted = categories
            .sortedByDescending { it.second }
            .joinToString(",") { (name, score) -> "$name=${Math.round(score * 100)}%" }
        return "$REASON_MODERATION_PREFIX$formatted"
    }
}
