package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy
import com.softdevcode.finflow_android.ui.theme.Danger
import com.softdevcode.finflow_android.ui.theme.Success
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Transaction?>(null) }
    var deleting by remember { mutableStateOf<Transaction?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var typeFilter by rememberSaveable { mutableStateOf("") }
    val visibleTransactions = uiState.transactions.filter { tx ->
        (typeFilter.isBlank() || tx.type == typeFilter) &&
        (search.isBlank() || tx.description.contains(search.trim(), ignoreCase = true))
    }

    editing?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            categories = uiState.categories,
            onDismiss = { editing = null },
            onSave = { request ->
                viewModel.updateTransaction(transaction.id, request)
                editing = null
            },
        )
    }
    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("Eliminar movimiento") },
            text = { Text("¿Eliminar '" + transaction.description +
                "'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTransaction(transaction.id)
                    deleting = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("Cancelar") }
            },
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    /*
     * Cada vez que volvemos a esta pantalla,
     * volvemos a consultar las transacciones.
     *
     * Esto ocurre cuando:
     *
     * Transactions
     *      ↓
     * AddTransaction
     *      ↓
     * Guardar
     *      ↓
     * popBackStack()
     *      ↓
     * Transactions
     *
     * Al volver, ON_RESUME dispara load().
     */
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.load()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(

        topBar = {
            Column {

            TopAppBar(

                title = {
                    Column {
                        Text("MOVIMIENTOS Y CONTROL", style = MaterialTheme.typography.labelSmall,
                            color = FinFlowGold, fontWeight = FontWeight.Bold)
                        Text("Tus movimientos", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                },

                actions = {

                    IconButton(
                        onClick = {
                            viewModel.load()
                        }
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = "Actualizar",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
            FinFlowV4Hero(
                "MOVIMIENTOS Y CONTROL",
                "Cada movimiento, en su lugar.",
                "Consulta, filtra y exporta tu historial con total claridad.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            }
        },

        floatingActionButton = {

            ExtendedFloatingActionButton(
                onClick = onAddTransaction,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva transacción") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {

            when {

                // ─────────────────────────────────────────────────────────────
                // Loading
                // ─────────────────────────────────────────────────────────────

                uiState.isLoading -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        )
                    )
                }


                // ─────────────────────────────────────────────────────────────
                // Error
                // ─────────────────────────────────────────────────────────────

                uiState.error != null -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center,
                    ) {

                        Text(
                            text =
                                uiState.error
                                    ?: "Error al cargar transacciones",

                            color =
                                MaterialTheme.colorScheme.error,
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.load()
                            }
                        ) {

                            Text("Reintentar")
                        }
                    }
                }


                // ─────────────────────────────────────────────────────────────
                // Empty
                // ─────────────────────────────────────────────────────────────

                uiState.transactions.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),

                        horizontalAlignment =
                            Alignment.CenterHorizontally,

                        verticalArrangement =
                            Arrangement.Center,
                    ) {

                        Text(
                            text = "No hay transacciones",

                            style =
                                MaterialTheme.typography.titleMedium,
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "Agrega tu primera transacción",

                            style =
                                MaterialTheme.typography.bodyMedium,

                            color =
                                MaterialTheme.colorScheme
                                    .onSurface
                                    .copy(alpha = 0.6f),
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = onAddTransaction,
                        ) {

                            Icon(
                                imageVector =
                                    Icons.Outlined.Add,

                                contentDescription = null,
                            )

                            Spacer(
                                modifier = Modifier.padding(
                                    horizontal = 4.dp
                                )
                            )

                            Text("Agregar transacción")
                        }
                    }
                }


                // ─────────────────────────────────────────────────────────────
                // Transactions
                // ─────────────────────────────────────────────────────────────

                else -> {

                    LazyColumn(

                        modifier =
                            Modifier.fillMaxSize(),

                        contentPadding =
                            PaddingValues(
                                horizontal = 20.dp,
                                vertical = 16.dp,
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp),
                    ) {

                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = search, onValueChange = { search = it },
                                    label = { Text("Buscar por descripción") },
                                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("" to "Todos", "income" to "Ingresos",
                                        "expense" to "Gastos").forEach { (value, label) ->
                                        FilterChip(
                                            selected = typeFilter == value,
                                            onClick = { typeFilter = value },
                                            label = { Text(label) },
                                        )
                                    }
                                }
                                Text(visibleTransactions.size.toString() + " movimientos",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (visibleTransactions.isEmpty()) {
                            item {
                                Text("No hay movimientos con estos filtros.",
                                    style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        items(
                            items = visibleTransactions,

                            key = {
                                it.id
                            },

                            ) { transaction ->

                            TransactionItem(
                                transaction = transaction,
                                onEdit = { editing = transaction },
                                onDelete = { deleting = transaction },
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
}


// ─────────────────────────────────────────────────────────────────────────────
// Transaction item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransactionItem(
    transaction: Transaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {

    val isIncome =
        transaction.type.equals(
            "income",
            ignoreCase = true
        )

    val amountColor: Color =
        if (isIncome) {
            Success
        } else {
            Danger
        }

    Card(
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),

        modifier =
            Modifier.fillMaxWidth(),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface,
            ),
    ) {

        Row(

            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

            verticalAlignment =
                Alignment.CenterVertically,
        ) {

            Icon(

                imageVector =
                    if (isIncome) {
                        Icons.Outlined.ArrowUpward
                    } else {
                        Icons.Outlined.ArrowDownward
                    },

                contentDescription = null,

                tint = amountColor,
            )

            Spacer(
                modifier = Modifier.padding(
                    horizontal = 6.dp
                )
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {

                Text(

                    text =
                        transaction.description,

                    style =
                        MaterialTheme.typography.titleSmall,
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(

                    text =
                        transaction.date ?: "",

                    style =
                        MaterialTheme.typography.bodySmall,

                    color =
                        MaterialTheme.colorScheme
                            .onSurface
                            .copy(alpha = 0.5f),
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatAmount(transaction.amount),
                    style = MaterialTheme.typography.titleSmall,
                    color = amountColor,
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Editar movimiento",
                            tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Eliminar movimiento",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Currency
// ─────────────────────────────────────────────────────────────────────────────

private fun formatAmount(
    amount: Double
): String {

    val formatter =
        NumberFormat.getCurrencyInstance(
            Locale("es", "CO")
        )

    return formatter.format(amount)
}