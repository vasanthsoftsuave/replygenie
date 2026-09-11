package com.example.rephrasegenie.domain.model

import java.time.Instant

/**
 * One record of a rephrase.
 *
 * Privacy rule carried over from the Windows app: **never store the text itself.** Only lengths,
 * timing, the model used and whether it succeeded. The same rule applies to logs.
 */
data class GenerationRecord(
    val id: String,
    val toneId: String,
    val toneVersion: String,
    val timestamp: Instant,
    val inputLength: Int,
    val outputLength: Int,
    val model: String,
    val status: String,
    val durationMs: Int,
) {
    companion object {
        const val STATUS_SUCCESS = "success"
        const val STATUS_BLOCKED = "blocked"
    }
}
