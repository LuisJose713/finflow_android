package com.softdevcode.finflow_android.ui.screens.transactions

import org.junit.Assert.*
import org.junit.Test

class ReceiptTextParserTest {
    @Test fun readsColombianTotalWithoutUsingSubtotal() {
        val text = """
            SUPER TIENDA
            NIT 900.222.333
            PAN 12.000
            SUBTOTAL 45.500
            IVA 5.000
            TOTAL A PAGAR $50.500,00
        """.trimIndent()
        val parsed = ReceiptTextParser.parse(text)
        assertEquals("SUPER TIENDA", parsed?.title)
        assertEquals("50500", parsed?.amount)
    }

    @Test fun refusesToGuessFromLineItems() {
        val parsed = ReceiptTextParser.parse("CAFE\nTinto $5.500\nAgua $3.000")
        assertEquals("CAFE", parsed?.title)
        assertNull(parsed?.amount)
    }

    @Test fun handlesDecimalTotal() {
        val parsed = ReceiptTextParser.parse("TIENDA\nTOTAL 12.345,67")
        assertEquals("12345.67", parsed?.amount)
    }

    @Test fun rejectsEmptyText() {
        assertNull(ReceiptTextParser.parse("  \n \t  "))
    }
}
