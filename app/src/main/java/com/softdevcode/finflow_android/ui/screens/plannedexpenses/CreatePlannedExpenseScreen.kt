package com.softdevcode.finflow_android.ui.screens.plannedexpenses

import com.softdevcode.finflow_android.ui.components.CopAmountField
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.PlannedExpenseRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlannedExpenseScreen(
    onBack: () -> Unit,
    viewModel: PlannedExpensesViewModel = hiltViewModel(),
) {

    var title by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }

    var dueDate by remember {
        mutableStateOf<String?>(null)
    }

    var remindAt by remember {
        mutableStateOf<String?>(null)
    }

    var notes by remember {
        mutableStateOf("")
    }

    var showDueDatePicker by remember {
        mutableStateOf(false)
    }

    var showRemindDatePicker by remember {
        mutableStateOf(false)
    }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.operationSuccess) {

        if (uiState.operationSuccess) {
            viewModel.clearOperationState()
            onBack()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Column {
                        Text("Nuevo gasto planeado", style = MaterialTheme.typography.titleLarge)
                        Text("Organiza tus próximos pagos",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),

            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {

            FinFlowV4Hero(
                "PLANIFICACIÓN INTELIGENTE", "Anticípate a lo que viene.",
                "Organiza tus compromisos antes de gastar.",
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Nombre del gasto")
                },
                placeholder = {
                    Text("Ej. Pago del arriendo")
                },
                singleLine = true,
            )

            CopAmountField(
                value = amount,
                onValueChange = {
                    amount = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Monto")
                },
                placeholder = {
                    Text("0")
                },
                singleLine = true,
            )

            // ─────────────────────────────────────────────
            // FECHA DEL GASTO
            // ─────────────────────────────────────────────

            OutlinedTextField(
                value = dueDate ?: "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Fecha del gasto")
                },
                placeholder = {
                    Text("Seleccionar fecha")
                },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            showDueDatePicker = true
                        }
                    ) {
                        Text("Elegir")
                    }
                },
            )

            // ─────────────────────────────────────────────
            // RECORDATORIO
            // ─────────────────────────────────────────────

            OutlinedTextField(
                value = remindAt ?: "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Recordatorio")
                },
                placeholder = {
                    Text("Seleccionar fecha")
                },
                readOnly = true,
                singleLine = true,
                trailingIcon = {
                    TextButton(
                        onClick = {
                            showRemindDatePicker = true
                        }
                    ) {
                        Text("Elegir")
                    }
                },
            )

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Notas")
                },
                minLines = 3,
            )

            uiState.operationError?.let {

                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {

                    val parsedAmount =
                        amount
                            .replace(",", ".")
                            .toDoubleOrNull()

                    if (
                        title.isNotBlank() &&
                        parsedAmount != null &&
                        parsedAmount > 0
                    ) {

                        viewModel.createPlannedExpense(

                            PlannedExpenseRequest(
                                title = title.trim(),

                                amount = parsedAmount,

                                categoryId = null,

                                dueDate = dueDate,

                                remindAt = remindAt,

                                notes =
                                    notes
                                        .trim()
                                        .ifBlank { null },
                            )
                        )
                    }
                },

                modifier = Modifier.fillMaxWidth(),

                enabled =
                    !uiState.operationLoading &&
                            title.isNotBlank() &&
                            amount
                                .replace(",", ".")
                                .toDoubleOrNull()
                                ?.let {
                                    it > 0
                                } == true,
            ) {

                if (uiState.operationLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )

                } else {

                    Text("Crear gasto planeado")
                }
            }
        }
    }

    // ============================================================
    // CALENDARIO FECHA DEL GASTO
    // ============================================================

    if (showDueDatePicker) {

        val datePickerState =
            rememberDatePickerState()

        DatePickerDialog(

            onDismissRequest = {
                showDueDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState.selectedDateMillis?.let {
                            dueDate = formatDate(it)
                        }

                        showDueDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDueDatePicker = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }

    // ============================================================
    // CALENDARIO RECORDATORIO
    // ============================================================

    if (showRemindDatePicker) {

        val datePickerState =
            rememberDatePickerState()

        DatePickerDialog(

            onDismissRequest = {
                showRemindDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState.selectedDateMillis?.let {
                            remindAt = formatDate(it)
                        }

                        showRemindDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showRemindDatePicker = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        ) {

            DatePicker(
                state = datePickerState
            )
        }
    }
}

private fun formatDate(
    millis: Long,
): String {

    return SimpleDateFormat(
        "yyyy-MM-dd",
        Locale.US
    ).format(
        Date(millis)
    )
}
