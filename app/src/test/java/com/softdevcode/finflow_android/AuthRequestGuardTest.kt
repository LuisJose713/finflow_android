package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.ui.screens.auth.AuthUiState
import com.softdevcode.finflow_android.ui.screens.auth.canStartAuthRequest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRequestGuardTest {
    @Test
    fun allowsFirstLoginAttempt() {
        assertTrue(canStartAuthRequest(AuthUiState(isCheckingAuth = false)))
    }

    @Test
    fun blocksRepeatedRequestsWhileLoginOrRegisterIsLoading() {
        assertFalse(canStartAuthRequest(AuthUiState(isLoading = true)))
    }

    @Test
    fun blocksNewRequestsAfterAuthentication() {
        assertFalse(canStartAuthRequest(AuthUiState(isAuthenticated = true)))
    }
}
