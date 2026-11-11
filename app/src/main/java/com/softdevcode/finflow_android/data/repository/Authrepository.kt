package com.softdevcode.finflow_android.data.repository

import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.AuthResponse
import com.softdevcode.finflow_android.domain.model.LoginRequest
import com.softdevcode.finflow_android.domain.model.RegisterRequest
import com.softdevcode.finflow_android.domain.model.User
import kotlinx.coroutines.flow.first
import com.softdevcode.finflow_android.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: FinFlowApi,
    private val tokenManager: TokenManager,
) {
    suspend fun login(email: String, password: String): Result<AuthResponse> =
        runCatching {
            // Clear any old token first
            tokenManager.clearAuth()
            val response = api.login(LoginRequest(email, password))
            tokenManager.saveAuth(
                token = response.token,
                name  = response.user.name,
                email = response.user.email,
                id    = response.user.id,
            )
            response
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Error al iniciar sesión") }
        )

    suspend fun register(email: String, password: String, name: String): Result<AuthResponse> =
        runCatching {
            val response = api.register(RegisterRequest(email, password, name))
            tokenManager.saveAuth(
                token = response.token,
                name  = response.user.name,
                email = response.user.email,
                id    = response.user.id,
            )
            response
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: "Error al registrarse") }
        )

    suspend fun logout() = tokenManager.clearAuth()

    suspend fun isLoggedIn(): Boolean {
        val token = tokenManager.token.first() ?: return false
        return try {
            val decoded = com.auth0.android.jwt.JWT(token)
            !decoded.isExpired(0)
        } catch (_: Exception) { false }
    }

    suspend fun getCurrentUser(): User? {
        val name  = tokenManager.userName.first() ?: return null
        val email = tokenManager.userEmail.first() ?: return null
        val id    = tokenManager.userId.first() ?: return null
        // Never infer ADMIN from persisted client data. Refresh role from authenticated
        // /auth/me; on network failure, use a least-privilege local display fallback.
        return try {
            api.getMe().takeIf { it.id == id && it.email.equals(email, true) }
                ?: User(id, email, name)
        } catch (_: Exception) {
            User(id, email, name)
        }
    }
}