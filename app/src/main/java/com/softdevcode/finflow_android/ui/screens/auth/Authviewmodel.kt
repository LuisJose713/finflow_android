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
import kotlinx.coroutines.Job
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

/** Prevent duplicate sign-in/register requests before the UI disables its buttons. */
internal fun canStartAuthRequest(state: AuthUiState): Boolean = !state.isLoading && !state.isAuthenticated

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
    private val api: FinFlowApi,
) : ViewModel() {

    private var authOperation = 0L
    private var authRequestJob: Job? = null
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val darkMode: StateFlow<Boolean> = tokenManager.darkMode
        .map { it ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        checkAuth()
        viewModelScope.launch {
            tokenManager.token.distinctUntilChanged().collect { token ->
                if (token.isNullOrEmpty() && _uiState.value.isAuthenticated) {
                    authOperation++
                    _uiState.update { AuthUiState(isCheckingAuth = false) }
                }
            }
        }
    }

    private fun checkAuth() {
        val operation = authOperation
        viewModelScope.launch {
            val loggedIn = authRepository.isLoggedIn()

            if (loggedIn) {
                val user = authRepository.getCurrentUser()
                if (operation != authOperation) return@launch
                _uiState.update {
                    it.copy(
                        isAuthenticated = user != null,
                        user = user,
                        isCheckingAuth = false
                    )
                }
            } else {
                if (operation != authOperation) return@launch
                _uiState.update {
                    it.copy(isCheckingAuth = false)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        if (!canStartAuthRequest(_uiState.value)) return
        authOperation++
        val operation = authOperation
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRequestJob = viewModelScope.launch {
            authRepository.login(email.trim(), password)
                .onSuccess() { response ->
                    if (operation != authOperation) return@onSuccess
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isAuthenticated = true,
                            user = response.user,
                        )
                    }
                }
                .onError { msg ->
                    if (operation != authOperation) return@onError
                    _uiState.update { it.copy(isLoading = false, error = msg) }
                }
        }
    }

    fun googleLogin(idToken: String) {
        if (!canStartAuthRequest(_uiState.value) || idToken.length > 8192) return
        authOperation++
        val operation = authOperation
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRequestJob = viewModelScope.launch {
            authRepository.googleLogin(idToken).onSuccess { response ->
                if (operation != authOperation) return@onSuccess
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = true, user = response.user)
                }
            }.onError { message ->
                if (operation != authOperation) return@onError
                _uiState.update { it.copy(isLoading = false, error = message) }
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
        if (!canStartAuthRequest(_uiState.value)) return
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
        authOperation++
        val operation = authOperation
        _uiState.update { it.copy(isLoading = true, error = null) }
        authRequestJob = viewModelScope.launch {
            authRepository.register(
                email = email.trim(), password = password, name = name.trim(),
                confirmPassword = confirmPassword,
                privacyAccepted = if (policy.ready) true else null,
                privacyVersion = if (policy.ready) policy.version else null,
                marketingConsent = if (policy.ready) marketingConsent else null,
            ).onSuccess { response ->
                if (operation != authOperation) return@onSuccess
                _uiState.update {
                    it.copy(isLoading = false, isAuthenticated = true, user = response.user)
                }
            }.onError { msg ->
                if (operation != authOperation) return@onError
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    fun logout() {
        authOperation++
        authRequestJob?.cancel()
        authRequestJob = null
        val operation = authOperation
        // Disable new sign-ins until the previous session has been cleared.
        _uiState.update { AuthUiState(isLoading = true, isCheckingAuth = false) }
        viewModelScope.launch {
            try {
                authRepository.logout()
            } finally {
                if (operation == authOperation) {
                    // Never re-enable the indefinite splash-screen gate after logout.
                    _uiState.update { AuthUiState(isCheckingAuth = false) }
                }
            }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            tokenManager.setDarkMode(!darkMode.value)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun setLoginError(message: String) = _uiState.update { it.copy(error = message) }
}

