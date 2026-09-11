package com.example.rephrasegenie.domain.model

/**
 * The user's local identity. No password, no account, no server — just a display name and their
 * own OpenAI key, stored encrypted on the device.
 */
data class Identity(
    val username: String,
    val openAiApiKey: String,
)
