package com.softdevcode.finflow_android.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

/** The editable model uses decimal dots; only the display uses es-CO punctuation. */
fun normalizeCopInput(input: String, previous: String = ""): String? {
    val raw = input.replace("COP", "", ignoreCase = true).replace("$", "")
        .filterNot { it.isWhitespace() || it == '\u00a0' }
    if (raw.isEmpty()) return ""
    if (raw.any { it !in '0'..'9' && it != '.' && it != ',' }) return null
    if (raw.count { it == ',' } > 1) return null
    val comma = raw.indexOf(',')
    val dotCount = raw.count { it == '.' }
    val dot = raw.indexOf('.')
    val editingDecimal = previous.contains('.') && input.length - previous.length in -1..1
    val decimalDot = comma < 0 && dotCount == 1 &&
        (editingDecimal || raw.length - dot - 1 <= 2)
    val separator = if (comma >= 0) comma else if (decimalDot) dot else -1
    val wholeText = if (separator >= 0) raw.take(separator) else raw
    if (wholeText.contains('.') && !wholeText.matches(Regex("[0-9]{1,3}(\\.[0-9]{3})+"))) return null
    val whole = wholeText.replace(".", "").trimStart('0').ifEmpty { "0" }
    val fraction = if (separator >= 0) raw.substring(separator + 1) else ""
    if (fraction.length > 2 || fraction.any { it !in '0'..'9' } || whole.length > 13) return null
    val normalized = whole + if (separator >= 0) ".$fraction" else ""
    if (normalized.toBigDecimalOrNull()?.let { it > BigDecimal("1000000000000") } != false) return null
    return normalized
}

fun copEditableAmount(amount: Double): String = BigDecimal.valueOf(amount)
    .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()

data class CopInputDisplay(val text: String, val offsets: List<Int>)

/** Offset mapping keeps the caret beside the same digit when thousands are inserted. */
fun copInputDisplay(raw: String, padDecimals: Boolean): CopInputDisplay {
    if (raw.isEmpty()) return CopInputDisplay("", listOf(0))
    val wholeLength = raw.substringBefore('.').length
    val display = StringBuilder()
    val offsets = mutableListOf<Int>()
    raw.forEachIndexed { index, character ->
        if (index > 0 && index < wholeLength && (wholeLength - index) % 3 == 0) display.append('.')
        offsets.add(display.length)
        display.append(if (character == '.') ',' else character)
    }
    offsets.add(display.length)
    if (padDecimals) {
        if (!raw.contains('.')) display.append(",00")
        else display.append("0".repeat((2 - raw.substringAfter('.').length).coerceAtLeast(0)))
    }
    return CopInputDisplay(display.toString(), offsets)
}

fun formatCopMoney(amount: Double): String {
    val sign = if (amount < 0) "−" else ""
    return sign + "$ " + copInputDisplay(copEditableAmount(kotlin.math.abs(amount)), padDecimals = true).text
}
