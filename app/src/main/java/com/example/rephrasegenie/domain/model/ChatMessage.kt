package com.example.rephrasegenie.domain.model

enum class ChatRole { SYSTEM, USER, ASSISTANT }

data class ChatTurn(val role: ChatRole, val content: String)

data class ChatCompletion(
    val text: String,
    val model: String,
    val latencyMs: Int,
)

/** The finished result handed back to the UI and the overlay. */
data class RephraseResult(
    val text: String,
    val model: String,
    val latencyMs: Int,
)
