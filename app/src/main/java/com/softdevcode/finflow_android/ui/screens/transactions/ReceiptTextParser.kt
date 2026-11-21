package com.softdevcode.finflow_android.ui.screens.transactions

import java.math.BigDecimal
import java.text.Normalizer
import java.util.Locale

/**
 * Receipts are processed on-device. The extracted values only prefill the
 * existing form. No transaction is created until the user confirms Save.
 */
object ReceiptTextParser {
    data class Draft(val title: String, val amount: String?)

    fun parse(text: String): Draft? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        if (lines.isEmpty()) return null

        val merchant = lines.take(8).firstOrNull { item ->
            val normalized = normalize(item)
            item.any(Char::isLetter) &&
                listOf("factura", "nit", "fecha", "telefono", "documento",
                    "cliente", "subtotal", "total", "cajero").none { normalized.contains(it) }
        }?.take(80) ?: "Compra de factura"

        // Never mistake an arbitrary line-item price for the payable total.
        val totalLine = lines.lastOrNull { line ->
            val value = normalize(line)
            (value.contains("total") && !value.contains("subtotal")) ||
                value.contains("valor a pagar")
        }
        val candidate = totalLine?.let { line ->
            Regex("""\d[\d.,]*""").findAll(line).lastOrNull()?.value
        }
        return Draft(merchant, candidate?.let(::amount))
    }

    private fun amount(token: String): String? {
        val clean = token.trim().trimEnd(',', '.')
        val separator = clean.indexOfLast { it == ',' || it == '.' }
        val number = if (separator < 0) clean
        else {
            val tail = clean.length - separator - 1
            if (tail in 1..2) clean.take(separator).filter(Char::isDigit) + "." + clean.drop(separator + 1)
            else clean.filter(Char::isDigit)
        }
        return runCatching { BigDecimal(number) }.getOrNull()
            ?.takeIf { it.signum() > 0 && it <= BigDecimal("1000000000000") && it.scale() <= 2 }
            ?.stripTrailingZeros()?.toPlainString()
    }

    private fun normalize(input: String): String = Normalizer.normalize(
        input.lowercase(Locale.ROOT), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "")
}
