package com.softdevcode.finflow_android.data.repository

import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.AuthResponse
import com.softdevcode.finflow_android.domain.model.LoginRequest
import com.softdevcode.finflow_android.domain.model.RegisterRequest
import com.softdevcode.finflow_android.domain.model.GoogleRegisterRequest
import com.softdevcode.finflow_android.domain.model.User
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import com.softdevcode.finflow_android.util.Result
import com.softdevcode.finflow_android.util.authErrorMessage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: FinFlowApi,
    private val tokenManager: TokenManager,
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> =
        runCatching {
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveAuth(
                token = response.token,
                name  = response.user.name,
                email = response.user.email,
                id    = response.user.id,
                refreshToken = response.refreshToken,
            )
            response
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = {
                if (it is CancellationException) throw it
                Result.Error(authErrorMessage(it, "No se pudo iniciar sesión. Inténtalo nuevamente."))
            }
        )

    suspend fun googleLogin(idToken: String): Result<AuthResponse> =
        runCatching {
            require(idToken.isNotBlank() && idToken.length <= 8192)
            val response = api.googleLogin(mapOf("idToken" to idToken))
            tokenManager.saveAuth(
                token = response.token, name = response.user.name, email = response.user.email,
                id = response.user.id, refreshToken = response.refreshToken,
            )
            response
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = {
                if (it is CancellationException) throw it
                Result.Error(authErrorMessage(it, "No se pudo verificar el acceso con Google."))
            }
        )

    suspend fun register(
        email: String, password: String, name: String, confirmPassword: String,
        privacyAccepted: Boolean?, privacyVersion: String?, marketingConsent: Boolean?,
    ): Result<AuthResponse> =
        runCatching {
            val response = api.register(RegisterRequest(
                email = email, password = password, name = name,
                confirmPassword = confirmPassword,
                privacyAccepted = privacyAccepted,
                privacyVersion = privacyVersion,
                marketingConsent = marketingConsent,
            ))
            tokenManager.saveAuth(
                token = response.token,
                name  = response.user.name,
                email = response.user.email,
                id    = response.user.id,
                refreshToken = response.refreshToken,
            )
            response
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = {
                if (it is CancellationException) throw it
                Result.Error(authErrorMessage(it, "No se pudo crear tu cuenta. Inténtalo nuevamente."))
            }
        )

    suspend fun googleRegister(
        idToken: String, privacyAccepted: Boolean?, privacyVersion: String?, marketingConsent: Boolean?,
    ): Result<AuthResponse> = runCatching {
        require(idToken.isNotBlank() && idToken.length <= 8192)
        val response = api.googleRegister(GoogleRegisterRequest(
            idToken = idToken, privacyAccepted = privacyAccepted,
            privacyVersion = privacyVersion, marketingConsent = marketingConsent,
        ))
        tokenManager.saveAuth(
            token = response.token, name = response.user.name, email = response.user.email,
            id = response.user.id, refreshToken = response.refreshToken,
        )
        response
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = {
            if (it is CancellationException) throw it
            Result.Error(authErrorMessage(it, "No se pudo crear tu cuenta con Google. Inténtalo nuevamente."))
        },
    )

    suspend fun logout() {
        val refresh = tokenManager.refreshToken.first()
        tokenManager.clearAuth()
        if (refresh != null) kotlinx.coroutines.withTimeoutOrNull(2000L) {
            try { api.logoutSession(mapOf("refreshToken" to refresh)) } catch (_: Exception) { }
        }
    }

    suspend fun isLoggedIn(): Boolean {
        val token = tokenManager.getTokenOnce() ?: return false
        return try {
            val decoded = com.auth0.android.jwt.JWT(token)
            !decoded.isExpired(0) || tokenManager.refreshToken.first() != null
        } catch (_: Exception) { false }
    }

    suspend fun getCurrentUser(): User? {
        val name  = tokenManager.userName.first() ?: return null
        val email = tokenManager.userEmail.first() ?: return null
        val id    = tokenManager.userId.first() ?: return null
        // Never infer ADMIN from persisted client data. Refresh role from authenticated
        // /auth/me; on network failure, use a least-privilege local display fallback.
        return try {
            // On slow/offline networks, show the least-privilege account view
            // instead of leaving the splash screen blocked for 40+ seconds.
            val verified = withTimeoutOrNull(12_000L) {
                api.getMe().takeIf { it.id == id && it.email.equals(email, true) }
            } ?: return User(id, email, name)
            if (tokenManager.refreshToken.first() == null) {
                val current = tokenManager.getTokenOnce()
                if (current != null) {
                    try {
                        withTimeoutOrNull(5_000L) {
                            tokenManager.replaceTokensIfCurrent(current, api.adoptSession())
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) { /* Keep the verified user on transient failures. */ }
                }
            }
            verified
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            User(id, email, name)
        }
    }
}
