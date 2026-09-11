package com.example.rephrasegenie.data.repository

import com.example.rephrasegenie.data.remote.OpenAiApi
import com.example.rephrasegenie.data.remote.dto.ChatMessageDto
import com.example.rephrasegenie.data.remote.dto.ChatRequestDto
import com.example.rephrasegenie.data.remote.dto.ModerationRequestDto
import com.example.rephrasegenie.di.IoDispatcher
import com.example.rephrasegenie.domain.model.ChatCompletion
import com.example.rephrasegenie.domain.model.ChatRole
import com.example.rephrasegenie.domain.model.ChatTurn
import com.example.rephrasegenie.domain.model.RephraseError
import com.example.rephrasegenie.domain.repository.OpenAiRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiRepositoryImpl @Inject constructor(
    private val api: OpenAiApi,
    @IoDispatcher private val io: CoroutineDispatcher,
) : OpenAiRepository {

    override suspend fun chat(messages: List<ChatTurn>): ChatCompletion = withContext(io) {
        val startedAt = System.currentTimeMillis()

        val response = call {
            api.chat(
                ChatRequestDto(
                    model = OpenAiApi.CHAT_MODEL,
                    messages = messages.map { ChatMessageDto(it.role.wireName(), it.content) },
                )
            )
        }

        val latencyMs = (System.currentTimeMillis() - startedAt).toInt()
        ChatCompletion(
            text = response.choices.firstOrNull()?.message?.content.orEmpty(),
            model = response.model.ifBlank { OpenAiApi.CHAT_MODEL },
            latencyMs = latencyMs,
        )
    }

    override suspend fun moderate(text: String): List<Pair<String, Float>> = withContext(io) {
        val response = call {
            api.moderate(
                ModerationRequestDto(model = OpenAiApi.MODERATION_MODEL, input = text)
            )
        }

        val result = response.results.firstOrNull() ?: return@withContext emptyList()
        if (!result.flagged) return@withContext emptyList()

        result.categories
            .filterValues { it }
            .keys
            .map { category -> category to (result.categoryScores[category] ?: 0f) }
    }

    override suspend fun validateApiKey(apiKey: String) = withContext(io) {
        if (apiKey.isBlank()) throw RephraseError.emptyApiKey()
        call { api.listModels("Bearer ${apiKey.trim()}") }
        Unit
    }

    /**
     * Runs a call and turns every failure into a message the user is allowed to see.
     * Raw errors, stack traces and the API key never reach the UI.
     */
    private suspend fun <T : Any> call(block: suspend () -> Response<T>): T {
        val response = try {
            block()
        } catch (e: SocketTimeoutException) {
            throw RephraseError.timeout()
        } catch (e: IOException) {
            throw RephraseError.offline()
        } catch (e: RephraseError) {
            throw e
        } catch (e: Exception) {
            throw RephraseError.generic()
        }

        if (!response.isSuccessful) {
            throw RephraseError.forHttpStatus(response.code())
        }
        return response.body() ?: throw RephraseError.generic()
    }

    private fun ChatRole.wireName(): String = when (this) {
        ChatRole.SYSTEM -> "system"
        ChatRole.USER -> "user"
        ChatRole.ASSISTANT -> "assistant"
    }
}
