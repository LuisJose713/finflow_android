package com.softdevcode.finflow_android

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.softdevcode.finflow_android.domain.model.AppTheme
import com.softdevcode.finflow_android.ui.theme.*
import org.junit.Assert.*
import org.junit.Test

class OledThemeTest {
    @Test fun upgradesPreserveExistingLightAndDarkPreferences() {
        assertEquals(AppTheme.LIGHT, AppTheme.resolve(null, false))
        assertEquals(AppTheme.DARK, AppTheme.resolve(null, true))
        assertEquals(AppTheme.DARK, AppTheme.resolve(null, null))
        assertEquals(AppTheme.LIGHT, AppTheme.resolve("unknown", false))
        assertEquals(AppTheme.OLED, AppTheme.resolve("OLED", false))
    }

    @Test fun oledUsesTrueBlackWithoutTonalSurfaceTint() {
        val colors = finFlowColors(AppTheme.OLED)
        assertEquals(Color.Black, colors.background)
        assertEquals(Color.Black, colors.surface)
        assertEquals(Color.Black, colors.surfaceContainerLowest)
        assertEquals(Color.Transparent, colors.surfaceTint)
        assertNotEquals(colors.background, colors.surfaceContainer)
        assertEquals(BackgroundDark, finFlowColors(AppTheme.DARK).background)
        assertEquals(BackgroundLight, finFlowColors(AppTheme.LIGHT).background)
    }

    @Test fun oledTextAndActionsHaveReadableContrast() {
        val colors = finFlowColors(AppTheme.OLED)
        fun contrast(a: Color, b: Color): Float {
            val brighter = maxOf(a.luminance(), b.luminance())
            val darker = minOf(a.luminance(), b.luminance())
            return (brighter + .05f) / (darker + .05f)
        }
        assertTrue(contrast(colors.onBackground, colors.background) >= 4.5f)
        assertTrue(contrast(colors.onSurface, colors.surface) >= 4.5f)
        assertTrue(contrast(colors.onSurfaceVariant, colors.surfaceVariant) >= 4.5f)
        assertTrue(contrast(colors.onPrimary, colors.primary) >= 4.5f)
        assertTrue(contrast(colors.outline, colors.surface) >= 3f)
    }
}
