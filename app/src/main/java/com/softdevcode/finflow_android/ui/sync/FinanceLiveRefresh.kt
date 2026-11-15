package com.softdevcode.finflow_android.ui.sync

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Fetches authoritative data only while this destination is RESUMED.
 * Cancels automatically on navigation, backgrounding, or logout.
 * Never sends a financial write or changes a form's local input state.
 */
@Composable
fun FinanceLiveRefresh(vararg keys: Any?, refresh: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(refresh)
    LaunchedEffect(owner, *keys) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(12_000L)
                if (isActive) latest()
            }
        }
    }
}
