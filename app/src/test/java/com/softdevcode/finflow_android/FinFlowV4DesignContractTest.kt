package com.softdevcode.finflow_android

import androidx.compose.ui.graphics.Color
import com.softdevcode.finflow_android.ui.theme.*
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Native/Web design-token parity contract.
 * Must track :root --ff-v4-* in LuisJose713/finflow/src/styles.scss.
 * This is a lightweight JVM regression test, NOT a rendered screenshot test.
 */
class FinFlowV4DesignContractTest {
    @Test fun officialBrandPaletteMatchesWebV4() {
        assertEquals(Color(0xFF09162A), FinFlowNavy)
        assertEquals(Color(0xFF172B4A), FinFlowNavy2)
        assertEquals(Color(0xFF273A5B), FinFlowHeroEnd)
        assertEquals(Color(0xFFD7A94D), FinFlowGold)
        assertEquals(Color(0xFF122944), Primary)
        assertEquals(Color(0xFFF5F7FB), BackgroundLight)
        assertEquals(Color(0xFF13243B), TextPrimaryLight)
        assertEquals(Color(0xFFE2E8F0), BorderLight)
        assertEquals(Color(0xFF101C30), BackgroundDark)
        assertEquals(Color(0xFF17253B), SurfaceDark)
        assertEquals(Color(0xFF2C405A), BorderDark)
    }
}
