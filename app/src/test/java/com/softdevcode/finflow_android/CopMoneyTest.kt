package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CopMoneyTest {
    @Test fun typingWholePesosDoesNotDivideTheAmountByOneHundred() {
        var raw = ""
        "25000".forEach { raw = normalizeCopInput(raw + it, raw)!! }
        assertEquals("25000", raw)
        assertEquals("25.000", copInputDisplay(raw, false).text)
        assertEquals("25.000,00", copInputDisplay(raw, true).text)
        assertEquals(25000.0, raw.toDouble(), 0.0)
    }

    @Test fun colombianPastedAmountsAndDecimalKeyboardsPreserveCentavos() {
        assertEquals("1250.50", normalizeCopInput("$ 1.250,50 COP"))
        assertEquals("1250.50", normalizeCopInput("1250.50"))
        assertEquals("1250", normalizeCopInput("1.250"))
        assertEquals("1250000", normalizeCopInput("1.250.000"))
        assertEquals("0.5", normalizeCopInput(",5"))
        assertEquals("1.250,50", copInputDisplay("1250.5", true).text)
    }

    @Test fun aThirdCentavoDigitIsRejectedInsteadOfMultiplyingTheValue() {
        assertNull(normalizeCopInput("12.345", "12.34"))
        assertNull(normalizeCopInput("12,345", "12.34"))
        assertEquals("12.3", normalizeCopInput("12.3", "12.34"))
    }

    @Test fun zeroAndEmptyAreDistinctAndInvalidOrExcessiveValuesAreRejected() {
        assertEquals("", normalizeCopInput(""))
        assertEquals("0", normalizeCopInput("0"))
        assertEquals("0,00", copInputDisplay("0", true).text)
        listOf("-100", "NaN", "Infinity", "1e6", "1,2,3", "1000000000001").forEach {
            assertNull(it, normalizeCopInput(it))
        }
        assertEquals("1000000000000", normalizeCopInput("1000000000000"))
    }

    @Test fun everyDisplayUsesTwoDecimalsAndExistingLargeValuesStayPlain() {
        assertEquals("$ 25.000,00", formatCopMoney(25000.0))
        assertEquals("$ 1.250,50", formatCopMoney(1250.5))
        assertEquals("−$ 2,50", formatCopMoney(-2.5))
        assertEquals("10000000", copEditableAmount(10000000.0))
        assertEquals("$ 0,00", formatCopMoney(0.0))
    }

    @Test fun caretOffsetsRemainOrderedAndInsideTheFormattedText() {
        listOf("", "0", "1234", "1234567.89", "12.").forEach { raw ->
            listOf(false, true).forEach { padded ->
                val result = copInputDisplay(raw, padded)
                assertEquals(raw.length + 1, result.offsets.size)
                assertTrue(result.offsets.all { it in 0..result.text.length })
                assertEquals(result.offsets.sorted(), result.offsets)
            }
        }
    }
}
