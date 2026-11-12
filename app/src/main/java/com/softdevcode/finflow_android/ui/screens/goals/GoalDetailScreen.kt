package com.softdevcode.finflow_android.ui.screens.goals

import com.softdevcode.finflow_android.ui.components.CopAmountField
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.Goal
import com.softdevcode.finflow_android.ui.theme.Primary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goalId: Int,
    onBack: () -> Unit,
    viewModel: GoalsViewModel = hiltViewModel(),
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val goal = uiState.goals.find {
        it.id == goalId
    }

    var showContributionDialog by remember {
        mutableStateOf(false)
    }

    var showDeleteDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(Unit) {
        viewModel.loadGoals()
    }

    if (goal == null && uiState.isLoading) {

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator()
        }

        return
    }

    if (goal == null) {

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Meta")
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onBack
                        ) {
                            Icon(
                                Icons.Outlined.ArrowBack,
                                contentDescription = "Volver",
                            )
                        }
                    }
                )
            }
        ) { padding ->

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {

                Text("Meta no encontrada")
            }
        }

        return
    }

    Scaffold(

        topBar = {

            TopAppBar(

                title = {
                    Text(goal.title)
                },

                navigationIcon = {

                    IconButton(
                        onClick = onBack
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },

                actions = {

                    IconButton(
                        onClick = {
                            showDeleteDialog = true
                        }
                    ) {

                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error,
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {

            Text(
                text = goal.title,
                style = MaterialTheme.typography.headlineSmall,
            )

            if (!goal.description.isNullOrBlank()) {

                Text(
                    text = goal.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.65f
                    ),
                )
            }

            LinearProgressIndicator(
                progress = {
                    goal.progress
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
            )

            Text(
                text = "${(goal.progress * 100).toInt()}%",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Column {

                    Text(
                        "Ahorrado",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Text(
                        formatAmount(goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {

                    Text(
                        "Objetivo",
                        style = MaterialTheme.typography.bodySmall,
                    )

                    Text(
                        formatAmount(goal.targetAmount),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            HorizontalDivider()

            Column {

                Text(
                    "Falta",
                    style = MaterialTheme.typography.bodySmall,
                )

                Text(
                    formatAmount(goal.remaining),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (goal.deadline != null) {

                Text(
                    "Fecha límite: ${goal.deadline}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (goal.status == "completed") {

                Text(
                    "🎉 ¡Meta completada!",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                )

            } else {

                Button(
                    onClick = {
                        showContributionDialog = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {

                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = null,
                    )

                    Spacer(
                        Modifier.width(8.dp)
                    )

                    Text("Agregar dinero")
                }
            }
        }
    }

    if (showContributionDialog) {

        ContributionDialog(
            loading = uiState.operationLoading,

            onDismiss = {
                if (!uiState.operationLoading) {
                    showContributionDialog = false
                }
            },

            onConfirm = { amount, note ->

                viewModel.addContribution(
                    goalId = goal.id,
                    amount = amount,
                    note = note,
                )
            }
        )
    }

    if (uiState.operationSuccess && showContributionDialog) {

        LaunchedEffect(uiState.operationSuccess) {

            showContributionDialog = false
            viewModel.clearOperationState()
        }
    }

    if (showDeleteDialog) {

        AlertDialog(

            onDismissRequest = {
                showDeleteDialog = false
            },

            title = {
                Text("Eliminar meta")
            },

            text = {
                Text(
                    "¿Quieres eliminar definitivamente \"${goal.title}\"?"
                )
            },

            confirmButton = {

                TextButton(
                    onClick = {

                        viewModel.deleteGoal(goal.id)
                        showDeleteDialog = false
                        onBack()
                    }
                ) {

                    Text(
                        "Eliminar",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },

            dismissButton = {

                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun ContributionDialog(
    loading: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Double, String?) -> Unit,
) {

    var amount by remember {
        mutableStateOf("")
    }

    var note by remember {
        mutableStateOf("")
    }

    AlertDialog(

        onDismissRequest = onDismiss,

        title = {
            Text("Agregar dinero")
        },

        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                CopAmountField(
                    value = amount,
                    onValueChange = {
                        amount = it
                    },
                    label = {
                        Text("Monto")
                    },
                    singleLine = true,
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it
                    },
                    label = {
                        Text("Nota (opcional)")
                    },
                    singleLine = true,
                )
            }
        },

        confirmButton = {

            TextButton(

                enabled =
                    !loading &&
                            amount.toDoubleOrNull()
                                ?.let { it > 0 } == true,

                onClick = {

                    val value =
                        amount.toDoubleOrNull()

                    if (value != null && value > 0) {

                        onConfirm(
                            value,
                            note.trim().ifBlank { null }
                        )
                    }
                }

            ) {

                if (loading) {

                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )

                } else {

                    Text("Agregar")
                }
            }
        },

        dismissButton = {

            TextButton(
                onClick = onDismiss,
                enabled = !loading,
            ) {
                Text("Cancelar")
            }
        }
    )
}

private fun formatAmount(
    amount: Double
): String {

    return com.softdevcode.finflow_android.domain.model.formatCopMoney(amount)
}
