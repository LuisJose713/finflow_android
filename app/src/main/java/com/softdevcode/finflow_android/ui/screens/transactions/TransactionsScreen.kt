package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.Transaction
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Movimientos")
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
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddTransaction,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Agregar transacción",
                )
            }
        },
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {

            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = uiState.error ?: "Error al cargar transacciones",
                            color = MaterialTheme.colorScheme.error,
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

                uiState.transactions.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = "No hay transacciones",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Agrega tu primera transacción",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = onAddTransaction,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )

                            Spacer(
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Text("Agregar transacción")
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 20.dp,
                            vertical = 16.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = uiState.transactions,
                            key = { it.id },
                        ) { transaction ->

                            TransactionItem(
                                transaction = transaction,
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

@Composable
private fun TransactionItem(
    transaction: Transaction,
) {
    val isIncome = transaction.type == "income"
    val amountColor: Color = if (isIncome) Success else Danger

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {

            Icon(
                imageVector = if (isIncome) {
                    Icons.Outlined.ArrowUpward
                } else {
                    Icons.Outlined.ArrowDownward
                },
                contentDescription = null,
                tint = amountColor,
            )

            Spacer(
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = transaction.description,
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(
                    modifier = Modifier.height(2.dp)
                )

                Text(
                    text = transaction.date ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.5f
                    ),
                )
            }

            Text(
                text = formatAmount(transaction.amount),
                style = MaterialTheme.typography.titleSmall,
                color = amountColor,
            )
        }
    }
}

private fun formatAmount(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(
        Locale("es", "CO")
    )

    return formatter.format(amount)
}

