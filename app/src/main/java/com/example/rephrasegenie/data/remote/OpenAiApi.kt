package com.example.rephrasegenie.data.remote

import com.example.rephrasegenie.data.remote.dto.ChatRequestDto
import com.example.rephrasegenie.data.remote.dto.ChatResponseDto
import com.example.rephrasegenie.data.remote.dto.ModerationRequestDto
import com.example.rephrasegenie.data.remote.dto.ModerationResponseDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAiApi {

    @POST("v1/chat/completions")
    suspend fun chat(@Body request: ChatRequestDto): Response<ChatResponseDto>

    @POST("v1/moderations")
    suspend fun moderate(@Body request: ModerationRequestDto): Response<ModerationResponseDto>

    /**
     * Cheap way to confirm a key works — no tokens spent. Takes the key directly so the setup
     * screen can test a key before it is saved.
     */
    @GET("v1/models")
    suspend fun listModels(@Header("Authorization") bearer: String): Response<ResponseBody>

    companion object {
        const val BASE_URL = "https://api.openai.com/"
        const val CHAT_MODEL = "gpt-4o-mini"
        const val MODERATION_MODEL = "omni-moderation-latest"
    }
}
