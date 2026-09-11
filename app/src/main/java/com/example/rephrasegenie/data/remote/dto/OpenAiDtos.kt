package com.example.rephrasegenie.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Deliberately minimal.
 *
 * The Windows app sets no temperature, no max_tokens, no top_p — it relies on the API defaults, and
 * adding any of them would change the results. So this request body carries only the model and the
 * messages, and nothing else.
 */
@Serializable
data class ChatRequestDto(
    val model: String,
    val messages: List<ChatMessageDto>,
)

@Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
)

@Serializable
data class ChatResponseDto(
    val model: String = "",
    val choices: List<ChoiceDto> = emptyList(),
)

@Serializable
data class ChoiceDto(
    val message: ChatMessageDto? = null,
)

@Serializable
data class ModerationRequestDto(
    val model: String,
    val input: String,
)

@Serializable
data class ModerationResponseDto(
    val results: List<ModerationResultDto> = emptyList(),
)

@Serializable
data class ModerationResultDto(
    val flagged: Boolean = false,
    val categories: Map<String, Boolean> = emptyMap(),
    @SerialName("category_scores") val categoryScores: Map<String, Float> = emptyMap(),
)
