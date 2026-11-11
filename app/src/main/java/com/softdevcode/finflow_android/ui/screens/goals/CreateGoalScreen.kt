package com.softdevcode.finflow_android.ui.screens.goals

import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.GoalRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGoalScreen(
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {

    var title by remember {
        mutableStateOf("")
    }

    var targetAmount by remember {
        mutableStateOf("")
    }

    var currentAmount by remember {
        mutableStateOf("")
    }

    var deadline by remember {
        mutableStateOf("")
    }

    var description by remember {
        mutableStateOf("")
    }

    var showDatePicker by remember {
        mutableStateOf(false)
    }

    val datePickerState = rememberDatePickerState()

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
                        Text("Nueva meta", style = MaterialTheme.typography.titleLarge)
                        Text("Define un objetivo para tu futuro",
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
                "TU FUTURO FINANCIERO", "Hazlo posible.",
                "Define tu meta y construye el camino para cumplirla.",
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Nombre de la meta")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = targetAmount,
                onValueChange = {
                    targetAmount = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Monto objetivo")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = currentAmount,
                onValueChange = {
                    currentAmount = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Monto inicial")
                },
                placeholder = {
                    Text("0")
                },
                singleLine = true,
            )

            // ─────────────────────────────────────────────
            // FECHA LÍMITE
            // ─────────────────────────────────────────────

            OutlinedTextField(
                value = deadline,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Fecha límite")
                },
                placeholder = {
                    Text("Selecciona una fecha")
                },
                singleLine = true,
                readOnly = true,
                trailingIcon = {

                    IconButton(
                        onClick = {
                            showDatePicker = true
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.DateRange,
                            contentDescription = "Seleccionar fecha",
                        )
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Descripción")
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

            Button(
                onClick = {

                    val target = targetAmount.toDoubleOrNull()

                    if (
                        title.isNotBlank() &&
                        target != null &&
                        target > 0
                    ) {

                        viewModel.createGoal(
                            GoalRequest(
                                title = title.trim(),
                                targetAmount = target,
                                currentAmount =
                                    currentAmount
                                        .toDoubleOrNull()
                                        ?.coerceAtLeast(0.0),
                                deadline =
                                    deadline
                                        .trim()
                                        .ifBlank { null },
                                description =
                                    description
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
                            targetAmount
                                .toDoubleOrNull()
                                ?.let { it > 0 } == true,

                ) {

                if (uiState.operationLoading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                    )

                } else {

                    Text("Crear meta")
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // CALENDARIO
    // ─────────────────────────────────────────────

    if (showDatePicker) {

        DatePickerDialog(
            onDismissRequest = {
                showDatePicker = false
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        datePickerState.selectedDateMillis?.let { millis ->

                            val formatter = SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.US
                            )

                            deadline = formatter.format(
                                Date(millis)
                            )
                        }

                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDatePicker = false
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