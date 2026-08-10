package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("expense") }
    var categoryId by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var notes by remember { mutableStateOf("") }

    // Estado del ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Cuando la transacción se guarda correctamente,
    // volvemos a la pantalla anterior.
    LaunchedEffect(uiState.saved) {
        if (uiState.saved) {
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Agregar transacción")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
            )
        },
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {

            Text(
                text = "Tipo de transacción",
                style = MaterialTheme.typography.titleSmall,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = type == "expense",
                    onClick = {
                        type = "expense"
                    },
                    label = {
                        Text("Gasto")
                    },
                )

                FilterChip(
                    selected = type == "income",
                    onClick = {
                        type = "income"
                    },
                    label = {
                        Text("Ingreso")
                    },
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = {
                    description = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Descripción")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = amount,
                onValueChange = {
                    amount = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Monto")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = categoryId,
                onValueChange = {
                    categoryId = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("ID de categoría")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = date,
                onValueChange = {
                    date = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Fecha")
                },
                singleLine = true,
            )

            OutlinedTextField(
                value = notes,
                onValueChange = {
                    notes = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Notas (opcional)")
                },
                minLines = 3,
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Button(
                onClick = {
                    val amountValue = amount.toDoubleOrNull()
                    val categoryValue = categoryId.toIntOrNull()

                    if (
                        description.isNotBlank() &&
                        amountValue != null &&
                        amountValue > 0 &&
                        categoryValue != null &&
                        date.isNotBlank()
                    ) {
                        viewModel.createTransaction(
                            CreateTransactionRequest(
                                description = description.trim(),
                                amount = amountValue,
                                type = type,
                                categoryId = categoryValue,
                                date = date.trim(),
                                notes = notes.trim().ifBlank { null },
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled =
                    description.isNotBlank() &&
                            amount.toDoubleOrNull()?.let { it > 0 } == true &&
                            categoryId.toIntOrNull() != null &&
                            date.isNotBlank(),
            ) {
                Text("Guardar transacción")
            }

            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
