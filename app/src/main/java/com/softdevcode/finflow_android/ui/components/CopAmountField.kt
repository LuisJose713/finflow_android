package com.softdevcode.finflow_android.ui.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.softdevcode.finflow_android.domain.model.copInputDisplay
import com.softdevcode.finflow_android.domain.model.normalizeCopInput

/** Shared by every monetary form. A whole peso stays a whole peso while typing. */
@Composable
fun CopAmountField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    allowDecimals: Boolean = true,
) {
    var focused by remember { mutableStateOf(false) }
    val transformation = remember(focused) { CopAmountTransformation(padDecimals = !focused) }
    OutlinedTextField(
        value = value,
        onValueChange = { proposed ->
            normalizeCopInput(proposed, value)?.let { normalized ->
                if (allowDecimals) onValueChange(normalized)
                else if (normalized.substringAfter('.', "").all { it == '0' })
                    onValueChange(normalized.substringBefore('.'))
            }
        },
        modifier = modifier.onFocusChanged { focused = it.isFocused },
        label = label,
        placeholder = placeholder,
        supportingText = supportingText,
        prefix = { Text("$") },
        suffix = { Text("COP") },
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = if (allowDecimals) KeyboardType.Decimal else KeyboardType.Number),
        visualTransformation = transformation,
    )
}

internal class CopAmountTransformation(private val padDecimals: Boolean) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val display = copInputDisplay(text.text, padDecimals)
        return TransformedText(AnnotatedString(display.text), object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = display.offsets[offset.coerceIn(0, text.length)]
            override fun transformedToOriginal(offset: Int): Int =
                display.offsets.indexOfFirst { it >= offset }.takeIf { it >= 0 } ?: text.length
        })
    }
}
