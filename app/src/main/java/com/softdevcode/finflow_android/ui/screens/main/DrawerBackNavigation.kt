package com.softdevcode.finflow_android.ui.screens.main

import androidx.activity.BackEventCompat
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.collect

internal fun opensDrawerFromBackEdge(edge: Int?): Boolean = edge == BackEventCompat.EDGE_LEFT

/** Left-edge predictive swipes open the drawer; buttons/right-edge back retain navigation. */
@Composable
internal fun DrawerBackNavigation(enabled: Boolean, onOpen: suspend () -> Unit) {
    val dispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    var delegateBack by remember { mutableStateOf(false) }
    PredictiveBackHandler(enabled = enabled && !delegateBack) { progress ->
        var edge: Int? = null
        // Cancellation exits this coroutine before either completion action runs.
        progress.collect { event -> if (edge == null) edge = event.swipeEdge }
        if (opensDrawerFromBackEdge(edge)) onOpen() else delegateBack = true
    }
    LaunchedEffect(delegateBack) {
        if (delegateBack) {
            // The callback is disabled in this committed composition before delegating.
            dispatcher?.onBackPressed()
            delegateBack = false
        }
    }
}
