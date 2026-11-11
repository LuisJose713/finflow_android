package com.softdevcode.finflow_android.ui.screens.transactions

import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DropdownMenuItem
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
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.domain.model.FinancialAccount
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel(),
) {

    var description by remember {
        mutableStateOf("")
    }

    var amount by remember {
        mutableStateOf("")
    }

    var type by remember {
        mutableStateOf("expense")
    }

    var selectedCategory by remember {
        mutableStateOf<Category?>(null)
    }

    var date by remember {
        mutableStateOf(
            LocalDate.now().toString()
        )
    }

    var notes by remember {
        mutableStateOf("")
    }

    var categoryExpanded by remember {
        mutableStateOf(false)
    }
    var accountExpanded by remember { mutableStateOf(false) }
    var selectedAccount by remember { mutableStateOf<FinancialAccount?>(null) }

    val uiState by viewModel.uiState
        .collectAsStateWithLifecycle()

    // ─────────────────────────────────────────────────────────────
    // VOLVER DESPUÉS DE GUARDAR
    // ─────────────────────────────────────────────────────────────

    LaunchedEffect(uiState.saved) {

        if (uiState.saved) {
            onBack()
        }
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Column {
                        Text("Nueva transacción", style = MaterialTheme.typography.titleLarge)
                        Text("Registra un ingreso o un gasto",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {

                        Icon(
                            imageVector =
                                Icons.Outlined.ArrowBack,

                            contentDescription =
                                "Volver",
                        )
                    }
                }
            )
        }

    ) { paddingValues ->

        Column(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(
                        rememberScrollState()
                    )
                    .padding(20.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp),
        ) {

            FinFlowV4Hero(
                "NUEVA TRANSACCIÓN", "Registra con claridad.",
                "Cada ingreso o gasto, en su lugar.",
            )

            // ─────────────────────────────────────────────────────
            // TIPO
            // ─────────────────────────────────────────────────────

            Text(
                text = "Tipo de transacción",
                style =
                    MaterialTheme.typography.titleSmall,
            )

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(8.dp),
            ) {

                FilterChip(

                    selected =
                        type == "expense",

                    onClick = {

                        type = "expense"

                        // Si cambiamos de tipo,
                        // limpiamos la categoría seleccionada.
                        selectedCategory = null
                    },

                    label = {
                        Text("Gasto")
                    }
                )

                FilterChip(

                    selected =
                        type == "income",

                    onClick = {

                        type = "income"

                        selectedCategory = null
                    },

                    label = {
                        Text("Ingreso")
                    }
                )
            }

            // ─────────────────────────────────────────────────────
            // DESCRIPCIÓN
            // ─────────────────────────────────────────────────────

            OutlinedTextField(

                value = description,

                onValueChange = {
                    description = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Descripción")
                },

                singleLine = true,
            )

            // ─────────────────────────────────────────────────────
            // MONTO
            // ─────────────────────────────────────────────────────

            OutlinedTextField(

                value = amount,

                onValueChange = {
                    amount = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Monto")
                },

                singleLine = true,
            )

            // ─────────────────────────────────────────────────────
            // CATEGORÍA
            // ─────────────────────────────────────────────────────

            val filteredCategories =
                uiState.categories.filter {

                    val categoryType =
                        it.type.lowercase()

                    if (type == "expense") {

                        categoryType == "expense"

                    } else {

                        categoryType == "income"
                    }
                }

            ExposedDropdownMenuBox(

                expanded = categoryExpanded,

                onExpandedChange = {

                    if (!uiState.isLoadingCategories) {
                        categoryExpanded =
                            !categoryExpanded
                    }
                },
            ) {

                OutlinedTextField(

                    value =
                        selectedCategory?.name
                            ?: "",

                    onValueChange = {},

                    readOnly = true,

                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .menuAnchor(),

                    label = {
                        Text("Categoría")
                    },

                    placeholder = {

                        Text(
                            if (uiState.isLoadingCategories)
                                "Cargando categorías..."
                            else
                                "Selecciona una categoría"
                        )
                    },

                    trailingIcon = {

                        if (uiState.isLoadingCategories) {

                            CircularProgressIndicator(
                                modifier =
                                    Modifier.height(20.dp),
                                strokeWidth = 2.dp,
                            )

                        } else {

                            ExposedDropdownMenuDefaults
                                .TrailingIcon(
                                    expanded =
                                        categoryExpanded
                                )
                        }
                    },

                    colors =
                        ExposedDropdownMenuDefaults
                            .textFieldColors(),
                )

                ExposedDropdownMenu(

                    expanded = categoryExpanded,

                    onDismissRequest = {
                        categoryExpanded = false
                    },
                ) {

                    if (filteredCategories.isEmpty()) {

                        DropdownMenuItem(

                            text = {

                                Text(
                                    "No hay categorías para este tipo"
                                )
                            },

                            onClick = {
                                categoryExpanded = false
                            }
                        )

                    } else {

                        filteredCategories.forEach { category ->

                            DropdownMenuItem(

                                text = {
                                    Text(category.name)
                                },

                                onClick = {

                                    selectedCategory =
                                        category

                                    categoryExpanded =
                                        false
                                }
                            )
                        }
                    }
                }
            }

            // Assign to one of the user's real wallets (optional, same as Web).
            ExposedDropdownMenuBox(
                expanded = accountExpanded,
                onExpandedChange = { accountExpanded = !accountExpanded },
            ) {
                OutlinedTextField(
                    value = selectedAccount?.name ?: "Sin cuenta",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    label = { Text("Cuenta / billetera (opcional)") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded)
                    },
                )
                ExposedDropdownMenu(
                    expanded = accountExpanded,
                    onDismissRequest = { accountExpanded = false },
                ) {
                    DropdownMenuItem(text = { Text("Sin cuenta") }, onClick = {
                        selectedAccount = null
                        accountExpanded = false
                    })
                    uiState.accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(account.name + " · " + account.currency) },
                            onClick = {
                                selectedAccount = account
                                accountExpanded = false
                            },
                        )
                    }
                }
            }

            // ─────────────────────────────────────────────────────
            // FECHA
            // ─────────────────────────────────────────────────────

            OutlinedTextField(

                value = date,

                onValueChange = {
                    date = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Fecha")
                },

                singleLine = true,
            )

            // ─────────────────────────────────────────────────────
            // NOTAS
            // ─────────────────────────────────────────────────────

            OutlinedTextField(

                value = notes,

                onValueChange = {
                    notes = it
                },

                modifier =
                    Modifier.fillMaxWidth(),

                label = {
                    Text("Notas (opcional)")
                },

                minLines = 3,
            )

            Spacer(
                modifier =
                    Modifier.height(8.dp)
            )

            // ─────────────────────────────────────────────────────
            // ERROR
            // ─────────────────────────────────────────────────────

            uiState.error?.let { error ->

                Text(

                    text = error,

                    color =
                        MaterialTheme.colorScheme.error,

                    style =
                        MaterialTheme.typography.bodySmall,
                )
            }

            // ─────────────────────────────────────────────────────
            // GUARDAR
            // ─────────────────────────────────────────────────────

            val amountValue =
                amount.toDoubleOrNull()

            val canSave =
                description.isNotBlank() &&
                        amountValue != null &&
                        amountValue > 0 &&
                        selectedCategory != null &&
                        date.isNotBlank() &&
                        !uiState.isLoading

            Button(

                onClick = {

                    val category =
                        selectedCategory
                            ?: return@Button

                    val finalAmount =
                        amount.toDoubleOrNull()
                            ?: return@Button

                    viewModel.createTransaction(

                        CreateTransactionRequest(

                            description =
                                description.trim(),

                            amount =
                                finalAmount,

                            type =
                                type,

                            categoryId =
                                category.id,

                            date =
                                date.trim(),

                            notes =
                                notes
                                    .trim()
                                    .ifBlank {
                                        null
                                    },
                            accountId = selectedAccount?.id,
                            currency = "COP",
                        )
                    )
                },

                modifier =
                    Modifier.fillMaxWidth(),

                enabled = canSave,
            ) {

                if (uiState.isLoading) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.height(20.dp),
                        strokeWidth = 2.dp,
                    )

                } else {

                    Text(
                        "Guardar transacción"
                    )
                }
            }
        }
    }
}