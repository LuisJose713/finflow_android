package com.softdevcode.finflow_android.ui.theme

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween

/** A quiet entrance follows the drawer's settle. Compose honors system animation scale. */
internal object FinFlowMotion {
    const val FeedbackMillis = 130
    const val RevealMillis = 220
    const val PanelMillis = 300
}

internal fun finFlowScreenEnter() =
    fadeIn(tween(FinFlowMotion.RevealMillis, easing = FastOutSlowInEasing)) +
        slideInVertically(tween(FinFlowMotion.RevealMillis, easing = FastOutSlowInEasing)) {
            (it / 80).coerceAtMost(16)
        }

internal fun finFlowScreenExit() = fadeOut(tween(FinFlowMotion.FeedbackMillis))
