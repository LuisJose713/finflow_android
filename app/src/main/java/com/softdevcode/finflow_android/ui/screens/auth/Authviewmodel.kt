package com.softdevcode.finflow_android.ui.screens.auth


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.softdevcode.finflow_android.data.repository.AuthRepository
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.domain.model.User
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
    val user: User? = null,
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager,
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
                _uiState.update { it.copy(isAuthenticated = true, user = user) }
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

    fun register(email: String, password: String, name: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            authRepository.register(email.trim(), password, name.trim())
                .onSuccess { response ->
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

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _uiState.update { AuthUiState() }
        }
    }

    fun toggleDarkMode() {
        viewModelScope.launch {
            tokenManager.setDarkMode(!darkMode.value)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}