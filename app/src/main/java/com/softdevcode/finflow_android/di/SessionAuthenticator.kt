package com.softdevcode.finflow_android.di

import com.auth0.android.jwt.JWT
import com.google.gson.Gson
import com.softdevcode.finflow_android.BuildConfig
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.domain.model.AuthResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionAuthenticator @Inject constructor(private val tokens: TokenManager) : Authenticator {
    private val lock = Any()
    private val gson = Gson()
    private val publicClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.host != BuildConfig.BASE_URL.toHttpUrl().host ||
            response.request.url.encodedPath in setOf("/auth/login", "/auth/register", "/auth/refresh", "/auth/logout") ||
            response.priorResponse != null) return null
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        return synchronized(lock) {
            val current = runBlocking { tokens.getTokenOnce() } ?: return@synchronized null
            if (current != failed) {
                // Never replay an old account's request under another account's identity.
                val sameOwner = try { JWT(current).subject == JWT(failed).subject } catch (_: Exception) { false }
                return@synchronized if (sameOwner) response.request.newBuilder().header("Authorization", "Bearer $current").build() else null
            }
            val refresh = runBlocking { tokens.refreshToken.first() } ?: return@synchronized null
            val request = Request.Builder().url(BuildConfig.BASE_URL + "auth/refresh")
                .post(gson.toJson(mapOf("refreshToken" to refresh)).toRequestBody("application/json".toMediaType())).build()
            try {
                publicClient.newCall(request).execute().use { renewed ->
                    if (!renewed.isSuccessful) {
                        if (renewed.code == 401 || renewed.code == 403) runBlocking { tokens.clearIfTokenMatches(failed) }
                        return@synchronized null
                    }
                    val value = gson.fromJson(renewed.body?.string(), AuthResponse::class.java) ?: return@synchronized null
                    if (runBlocking { tokens.replaceTokensIfCurrent(failed, value) })
                        response.request.newBuilder().header("Authorization", "Bearer " + value.token).build()
                    else null
                }
            } catch (_: Exception) { null }
        }
    }
}
