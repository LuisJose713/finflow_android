package com.softdevcode.finflow_android.ui.screens.transactions

import com.softdevcode.finflow_android.ui.components.CopAmountField
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.domain.model.Transaction

@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (CreateTransactionRequest) -> Unit,
) {
    var description by remember(transaction.id) { mutableStateOf(transaction.description) }
    var amount by remember(transaction.id) { mutableStateOf(com.softdevcode.finflow_android.domain.model.copEditableAmount(transaction.amount)) }
    var type by remember(transaction.id) { mutableStateOf(transaction.type) }
    var categoryId by remember(transaction.id) { mutableIntStateOf(transaction.categoryId) }
    var date by remember(transaction.id) {
        mutableStateOf((transaction.date ?: transaction.createdAt ?: "").substringBefore("T"))
    }
    val validAmount = amount.replace(",", ".").toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0 }
    val validDate = date.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
    val matchingCategories = categories.filter { it.type.equals(type, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar movimiento") },
        text = {
            Column(
                Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Tipo")
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("expense" to "Gasto", "income" to "Ingreso").forEach { (key, label) ->
                        FilterChip(selected = type == key, onClick = {
                            type = key
                            categoryId = 0
                        }, label = { Text(label) })
                    }
                }
                OutlinedTextField(description, { description = it.take(255) },
                    label = { Text("Descripción") }, singleLine = true)
                CopAmountField(amount, { amount = it },
                    label = { Text("Monto") }, singleLine = true)
                Text("Categoría")
                Row(Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    matchingCategories.forEach { category ->
                        FilterChip(selected = categoryId == category.id,
                            onClick = { categoryId = category.id },
                            label = { Text(category.name) })
                    }
                }
                OutlinedTextField(date, { date = it.take(10) },
                    label = { Text("Fecha AAAA-MM-DD") }, singleLine = true)
                Text("La cuenta y la moneda originales se conservan al editar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = {
            TextButton(
                enabled = description.isNotBlank() && validAmount != null &&
                    categoryId > 0 && validDate,
                onClick = {
                    val value = validAmount ?: return@TextButton
                    onSave(CreateTransactionRequest(
                        description = description.trim(), amount = value, type = type,
                        categoryId = categoryId, date = date,
                        accountId = transaction.accountId,
                        currency = transaction.currency ?: "COP",
                    ))
                },
            ) { Text("Guardar cambios") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

