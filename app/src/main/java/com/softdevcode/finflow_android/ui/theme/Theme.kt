package com.softdevcode.finflow_android.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ── Light color scheme ────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = Primary,
    onPrimary          = Color.White,
    primaryContainer   = Color(0xFFF3E6D3),
    onPrimaryContainer = PrimaryDark,
    secondary          = Color(0xFF64748B),
    onSecondary        = Color.White,
    background         = BackgroundLight,
    onBackground       = TextPrimaryLight,
    surface            = SurfaceLight,
    onSurface          = TextPrimaryLight,
    surfaceVariant     = Color(0xFFF1F5F9),
    onSurfaceVariant   = TextSecondLight,
    outline            = BorderLight,
    error              = Danger,
    onError            = Color.White,
    errorContainer     = Color(0xFFFEE2E2),
)

// ── Dark color scheme ─────────────────────────────────────────────────────────
private val DarkColorScheme = darkColorScheme(
    primary            = FinFlowGoldPale,
    onPrimary          = FinFlowNavy,
    primaryContainer   = Color(0xFF22334A),
    onPrimaryContainer = Color(0xFFE2BD83),
    secondary          = Color(0xFF94A3B8),
    onSecondary        = Color(0xFF1E293B),
    background         = BackgroundDark,
    onBackground       = TextPrimaryDark,
    surface            = SurfaceDark,
    onSurface          = TextPrimaryDark,
    surfaceVariant     = CardDark,
    onSurfaceVariant   = TextSecondDark,
    outline            = BorderDark,
    error              = DangerLight,
    onError            = Color(0xFF450A0A),
    errorContainer     = Color(0xFF7F1D1D),
)

// ── FinFlow extras (non-MD3 tokens) ──────────────────────────────────────────
data class FinFlowExtras(
    val success: Color,
    val successLight: Color,
    val danger: Color,
    val dangerLight: Color,
    val warning: Color,
    val cardBg: Color,
    val border: Color,
    val textSecondary: Color,
    val textTertiary: Color,
)

val LocalFinFlowExtras = staticCompositionLocalOf {
    FinFlowExtras(
        success       = Success,
        successLight  = SuccessLight,
        danger        = Danger,
        dangerLight   = DangerLight,
        warning       = Warning,
        cardBg        = CardLight,
        border        = BorderLight,
        textSecondary = TextSecondLight,
        textTertiary  = TextTertiaryLight,
    )
}

val MaterialTheme.extras: FinFlowExtras
    @Composable @ReadOnlyComposable
    get() = LocalFinFlowExtras.current

// ── Theme entry point ─────────────────────────────────────────────────────────
@Composable
fun FinFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val extras = if (darkTheme) {
        FinFlowExtras(
            success       = SuccessLight,
            successLight  = Success,
            danger        = DangerLight,
            dangerLight   = Danger,
            warning       = WarningLight,
            cardBg        = CardDark,
            border        = BorderDark,
            textSecondary = TextSecondDark,
            textTertiary  = TextTertiaryDark,
        )
    } else {
        FinFlowExtras(
            success       = Success,
            successLight  = SuccessLight,
            danger        = Danger,
            dangerLight   = DangerLight,
            warning       = Warning,
            cardBg        = CardLight,
            border        = BorderLight,
            textSecondary = TextSecondLight,
            textTertiary  = TextTertiaryLight,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFinFlowExtras provides extras) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = FinFlowTypography,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(30.dp)
            ),
            content     = content
        )
    }
}