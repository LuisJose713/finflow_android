package com.softdevcode.finflow_android.ui.screens.plannedexpenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.PlannedExpense
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannedExpensesScreen(
    onAddPlannedExpense: () -> Unit,
    viewModel: PlannedExpensesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var expenseToDelete by remember {
        mutableStateOf<PlannedExpense?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Gastos planeados")
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadPlannedExpenses()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Actualizar",
                        )
                    }
                }
            )
        },

        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddPlannedExpense,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Agregar gasto planeado",
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            when {

                uiState.isLoading -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }

                uiState.error != null -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Text(
                            text = uiState.error
                                ?: "Error al cargar los gastos planeados",
                            color = MaterialTheme.colorScheme.error,
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.loadPlannedExpenses()
                            }
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                uiState.expenses.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.ReceiptLong,
                            contentDescription = null,
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "No tienes gastos planeados",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Planea un gasto futuro para llevar el control.",
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.6f
                            )
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Button(
                            onClick = onAddPlannedExpense,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )

                            Spacer(
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Text("Planear gasto")
                        }
                    }
                }

                else -> {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),

                        contentPadding = PaddingValues(
                            horizontal = 20.dp,
                            vertical = 16.dp,
                        ),

                        verticalArrangement = Arrangement.spacedBy(
                            12.dp
                        ),
                    ) {

                        items(
                            items = uiState.expenses,
                            key = { it.id },
                        ) { expense ->

                            PlannedExpenseCard(
                                expense = expense,
                                onExecute = {
                                    viewModel.executePlannedExpense(
                                        expense.id
                                    )
                                },
                                onCancel = {
                                    viewModel.cancelPlannedExpense(
                                        expense.id
                                    )
                                },
                                onDelete = {
                                    expenseToDelete = expense
                                }
                            )
                        }

                        item {
                            Spacer(
                                modifier = Modifier.height(80.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // CONFIRMAR ELIMINACIÓN
    // ============================================================

    expenseToDelete?.let { expense ->

        AlertDialog(
            onDismissRequest = {
                expenseToDelete = null
            },

            title = {
                Text("Eliminar gasto planeado")
            },

            text = {
                Text(
                    "¿Quieres eliminar \"${expense.title}\"?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.deletePlannedExpense(
                            expense.id
                        )

                        expenseToDelete = null
                    }
                ) {
                    Text("Eliminar")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        expenseToDelete = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PlannedExpenseCard(
    expense: PlannedExpense,
    onExecute: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
) {

    val isPending = expense.status == "pending"
    val isDone = expense.status == "done"
    val isCancelled = expense.status == "cancelled"

    Card(
        modifier = Modifier.fillMaxWidth(),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.titleMedium,
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = formatCurrency(
                            expense.amount
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                IconButton(
                    onClick = onDelete,
                    enabled = !isPending,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Eliminar",
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            expense.dueDate?.let { date ->

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        imageVector = Icons.Outlined.Event,
                        contentDescription = null,
                    )

                    Spacer(
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Text(
                        text = "Fecha: ${formatDate(date)}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )
            }

            expense.notes
                ?.takeIf { it.isNotBlank() }
                ?.let { notes ->

                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        )
                    )

                    Spacer(
                        modifier = Modifier.height(10.dp)
                    )
                }

            Text(
                text = when {
                    isDone -> "✓ Ejecutado"
                    isCancelled -> "✕ Cancelado"
                    else -> "Pendiente"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    isDone ->
                        MaterialTheme.colorScheme.primary

                    isCancelled ->
                        MaterialTheme.colorScheme.error

                    else ->
                        MaterialTheme.colorScheme.secondary
                }
            )

            if (isPending) {

                Spacer(
                    modifier = Modifier.height(14.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Button(
                        onClick = onExecute,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Ejecutar")
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

private fun formatCurrency(
    amount: Double,
): String {

    return NumberFormat
        .getCurrencyInstance(Locale("es", "CO"))
        .format(amount)
}

private fun formatDate(
    date: String,
): String {

    return try {

        val parts = date.split("-")

        if (parts.size == 3) {
            "${parts[2]}/${parts[1]}/${parts[0]}"
        } else {
            date
        }

    } catch (
        _: Exception
    ) {
        date
    }
}