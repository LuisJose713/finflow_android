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
        assertEquals(Color(0xFF0E192B), FinFlowNavy)
        assertEquals(Color(0xFF1B2B43), FinFlowNavy2)
        assertEquals(Color(0xFF1B2B43), FinFlowHeroEnd)
        assertEquals(Color(0xFFDCC29A), FinFlowGold)
        assertEquals(Color(0xFF0E192B), Primary)
        assertEquals(Color(0xFFF6F3ED), BackgroundLight)
        assertEquals(Color(0xFF14243A), TextPrimaryLight)
        assertEquals(Color(0xFFE3DED5), BorderLight)
        assertEquals(Color(0xFF0B1220), BackgroundDark)
        assertEquals(Color(0xFF0E192B), SurfaceDark)
        assertEquals(Color(0xFF26364D), BorderDark)
    }
}

