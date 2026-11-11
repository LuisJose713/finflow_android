package com.softdevcode.finflow_android.ui.screens.auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.softdevcode.finflow_android.data.repository.AuthRepository
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.FinFlowPrivacyPolicy
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.domain.model.User
import com.softdevcode.finflow_android.domain.model.registrationPasswordError
import com.softdevcode.finflow_android.util.onError
import com.softdevcode.finflow_android.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isCheckingAuth: Boolean = true,
    val user: User? = null,
    val registrationPolicy: FinFlowPrivacyPolicy? = null,
    val isLoadingRegistrationPolicy: Boolean = false,
    val registrationPolicyError: String? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val api: FinFlowApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val darkMode: StateFlow<Boolean> = tokenManager.darkMode
        .map { it ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        checkAuth()
    }

    private fun checkAuth() {
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()

            if (loggedIn) {
                val user = authRepository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        isAuthenticated = true,
                        user = user,
                        isCheckingAuth = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(isCheckingAuth = false)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.login(email.trim(), password)
                .onSuccess() { response ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = response.user,
                        )
                    }
                }
                .onError { msg ->
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun loadRegistrationPolicy() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(isLoadingRegistrationPolicy = true, registrationPolicyError = null)
            }
            try {
                val policy = api.getPrivacyPolicy()
                _uiState.update {
                    it.copy(registrationPolicy = policy, isLoadingRegistrationPolicy = false)
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoadingRegistrationPolicy = false,
                        registrationPolicy = null,
                        registrationPolicyError = "No se pudo consultar la política vigente. Reintenta.")
                }
            }
        }
    }

    fun register(
        email: String, password: String, name: String, confirmPassword: String,
        accepted: Boolean, marketingConsent: Boolean,
    ) {
        registrationPasswordError(password)?.let { message ->
            _uiState.update { it.copy(isLoading = false, error = message) }
            return
        }
        val policy = _uiState.value.registrationPolicy
        if (policy == null) {
            _uiState.update { it.copy(error = "Consulta primero la política de privacidad vigente.") }
            return
        }
        if (password != confirmPassword) {
            _uiState.update { it.copy(error = "Las contraseñas deben coincidir.") }
            return
        }
        if (policy.ready && !accepted) {
            _uiState.update { it.copy(error = "Debes autorizar expresamente la política vigente.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(
                email = email.trim(), password = password, name = name.trim(),
                confirmPassword = confirmPassword,
                privacyAccepted = if (policy.ready) true else null,
                privacyVersion = if (policy.ready) policy.version else null,
                marketingConsent = if (policy.ready) marketingConsent else null,
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = true, user = response.user)
                }
            }.onError { msg ->
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            // Never re-enable the indefinite splash-screen gate after logout.
            _uiState.update { AuthUiState(isCheckingAuth = false) }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            tokenManager.setDarkMode(!darkMode.value)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
