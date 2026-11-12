package com.softdevcode.finflow_android.ui.screens.goals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Hero
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.Goal
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    onAddGoal: () -> Unit,
    onOpenGoal: (Int) -> Unit,
    onOpenMenu: (() -> Unit)? = null,
    viewModel: GoalsViewModel = hiltViewModel(),

) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
            TopAppBar(
                navigationIcon = {
                    onOpenMenu?.let { open -> IconButton(onClick = open) {
                        Icon(Icons.Outlined.Menu, "Abrir menú")
                    } }
                },
                title = {
                    Column {
                        Text("TUS OBJETIVOS", style = MaterialTheme.typography.labelSmall,
                            color = FinFlowGold, fontWeight = FontWeight.Bold)
                        Text("Metas", style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.loadGoals()
                        },
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
                "TU FUTURO FINANCIERO",
                "Los grandes planes empiezan hoy.",
                "Haz seguimiento a cada objetivo y celebra tus avances.",
                cardId = "goals.intro",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            }
        },

        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddGoal,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("Nueva meta") },
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

                // ─────────────────────────────────────────────
                // Cargando
                // ─────────────────────────────────────────────
                uiState.isLoading -> {

                    CircularProgressIndicator(
                        modifier = Modifier.align(
                            Alignment.Center
                        ),
                    )
                }

                // ─────────────────────────────────────────────
                // Error
                // ─────────────────────────────────────────────
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
                                ?: "Error al cargar las metas",
                            color = MaterialTheme.colorScheme.error,
                        )

                        Spacer(
                            modifier = Modifier.height(16.dp)
                        )

                        Button(
                            onClick = {
                                viewModel.loadGoals()
                            },
                        ) {
                            Text("Reintentar")
                        }
                    }
                }

                // ─────────────────────────────────────────────
                // Sin metas
                // ─────────────────────────────────────────────
                uiState.goals.isEmpty() -> {

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {

                        Icon(
                            imageVector = Icons.Outlined.Savings,
                            contentDescription = null,
                        )

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )

                        Text(
                            text = "No tienes metas todavía",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )

                        Text(
                            text = "Crea una meta para empezar a ahorrar.",
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = 0.6f
                            ),
                        )

                        Spacer(
                            modifier = Modifier.height(20.dp)
                        )

                        Button(
                            onClick = onAddGoal,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                            )

                            Spacer(
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )

                            Text("Crear meta")
                        }
                    }
                }

                // ─────────────────────────────────────────────
                // Metas
                // ─────────────────────────────────────────────
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
                            items = uiState.goals,
                            key = { it.id },
                        ) { goal ->

                            GoalCard(
                                goal = goal,
                                onClick = { onOpenGoal(goal.id) },
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
private fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),

        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {

        Column(
            modifier = Modifier.padding(18.dp),
        ) {

            Text(
                text = goal.title,
                style = MaterialTheme.typography.titleMedium,
            )

            goal.description
                ?.takeIf { it.isNotBlank() }
                ?.let { description ->

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.6f
                        ),
                    )
                }

            Spacer(
                modifier = Modifier.height(16.dp)
            )

            LinearProgressIndicator(
                progress = {
                    goal.progress
                },
                modifier = Modifier.fillMaxWidth(),
                color = FinFlowGold,
                trackColor = FinFlowGold.copy(alpha = .14f),
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {

                Text(
                    text = formatCurrency(
                        goal.currentAmount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Text(
                    text = formatCurrency(
                        goal.targetAmount
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(
                        alpha = 0.6f
                    ),
                )
            }

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text = "${(goal.progress * 100).toInt()}% completado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )

            if (goal.status == "completed") {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "✓ Meta completada",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun formatCurrency(
    amount: Double,
): String {

    return com.softdevcode.finflow_android.domain.model.formatCopMoney(amount)
}
