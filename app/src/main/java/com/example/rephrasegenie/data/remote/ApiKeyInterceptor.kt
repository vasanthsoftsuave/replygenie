package com.example.rephrasegenie.data.remote

import com.example.rephrasegenie.data.local.secure.SecureIdentityStore
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds the user's key to every request.
 *
 * The key is read fresh each time rather than captured once, so changing it in Settings takes
 * effect immediately. The Windows app builds its client at startup and has to tell the user to
 * restart after changing the key.
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val identityStore: SecureIdentityStore,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // The key-validation call sets its own header, so leave it alone.
        if (request.header("Authorization") != null) {
            return chain.proceed(request)
        }

        val apiKey = identityStore.currentApiKey()
        val authorized = if (apiKey.isNullOrBlank()) {
            request
        } else {
            request.newBuilder()
                .header("Authorization", "Bearer $apiKey")
                .build()
        }
        return chain.proceed(authorized)
    }
}
