package com.softdevcode.finflow_android.ui.screens.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardScreen
import com.softdevcode.finflow_android.ui.screens.goals.CreateGoalScreen
import com.softdevcode.finflow_android.ui.screens.goals.GoalDetailScreen
import com.softdevcode.finflow_android.ui.screens.goals.GoalsScreen
import com.softdevcode.finflow_android.ui.screens.modules.FinFlowModulesScreen
import com.softdevcode.finflow_android.ui.screens.navigation.navigateToTopLevel
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.CreatePlannedExpenseScreen
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.PlannedExpensesScreen
import com.softdevcode.finflow_android.ui.screens.transactions.AddTransactionScreen
import com.softdevcode.finflow_android.ui.screens.transactions.TransactionsScreen
import com.softdevcode.finflow_android.ui.theme.Primary
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel

// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    updateViewModel: AppUpdateViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
) {

    /*
     * Este NavController controla únicamente la navegación interna
     * de la aplicación una vez que el usuario está autenticado.
     */
    val navController = rememberNavController()

    val navBackStackEntry by navController.currentBackStackEntryAsState()

    val currentDestination = navBackStackEntry?.destination

    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()
    var requestedModule by remember { mutableStateOf("accounts") }


    // ─────────────────────────────────────────────────────────────────────────
    // Authentication
    // ─────────────────────────────────────────────────────────────────────────

    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) {
            onLogout()
        }
    }


    // ─────────────────────────────────────────────────────────────────────────
    // Scaffold
    // ─────────────────────────────────────────────────────────────────────────

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            // The dashboard shares the existing transaction form with Movimientos.
            val isDashboard = currentDestination?.hierarchy?.any {
                it.route == Screen.Dashboard.route
            } == true
            if (isDashboard) {
                ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.route) },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo movimiento", fontWeight = FontWeight.SemiBold) },
                    containerColor = Primary,
                    contentColor = Color.White,
                )
            }
        },
        bottomBar = {

            MainBottomBar(navController)
        }
    ) { innerPadding ->


        // ─────────────────────────────────────────────────────────────────────
        // Internal navigation graph
        // ─────────────────────────────────────────────────────────────────────

        NavHost(

            navController = navController,

            startDestination = Screen.Dashboard.route,

            modifier = Modifier.padding(innerPadding),

            enterTransition = {
                fadeIn(
                    animationSpec = tween(180)
                )
            },

            exitTransition = {
                fadeOut(
                    animationSpec = tween(180)
                )
            },

            popEnterTransition = {
                fadeIn(
                    animationSpec = tween(180)
                )
            },

            popExitTransition = {
                fadeOut(
                    animationSpec = tween(180)
                )
            }
        ) {


            // ─────────────────────────────────────────────────────────────────
            // Dashboard
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.Dashboard.route) {

                DashboardScreen(

                    userName = uiState.user?.name ?: "",

                    isDark = darkMode,

                    onToggleTheme = {
                        authViewModel.toggleDarkMode()
                    },

                    onLogout = {
                        authViewModel.logout()
                    },
                    onAddTransaction = { navController.navigate(Screen.AddTransaction.route) },
                    onViewTransactions = { navController.navigateToTopLevel(Screen.Transactions.route) },
                    onViewPlannedExpenses = { navController.navigateToTopLevel(Screen.PlannedExpenses.route) },
                    onViewGoals = { navController.navigateToTopLevel(Screen.Goals.route) },
                    onViewAccounts = {
                        requestedModule = "accounts"
                        navController.navigateToTopLevel(Screen.More.route)
                    },
                    onViewAdvisor = {
                        requestedModule = "advisor"
                        navController.navigateToTopLevel(Screen.More.route)
                    },
                )
            }


            // ─────────────────────────────────────────────────────────────────
            // Transactions
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.Transactions.route) {

                TransactionsScreen(

                    onAddTransaction = {

                        navController.navigate(
                            Screen.AddTransaction.route
                        )
                    }
                )
            }


            // ─────────────────────────────────────────────────────────────────
            // ADD TRANSACTION
            //
            // ESTA ERA LA PARTE QUE FALTABA.
            //
            // MainScreen intentaba navegar hacia:
            //
            //     add_transaction
            //
            // pero ese destino no existía dentro de este NavHost.
            //
            // Por eso Navigation lanzaba:
            //
            // IllegalArgumentException:
            // Navigation destination ... add_transaction
            // cannot be found in the navigation graph
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.AddTransaction.route) {

                AddTransactionScreen(

                    onBack = {

                        /*
                         * Volvemos a TransactionsScreen.
                         *
                         * Más adelante vamos a hacer que TransactionsScreen
                         * detecte este regreso y vuelva a consultar el backend.
                         */
                        navController.popBackStack()
                    }
                )
            }


            // ─────────────────────────────────────────────────────────────────
            // Goals
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.Goals.route) {
                GoalsScreen(
                    onAddGoal = {
                        navController.navigate(Screen.CreateGoal.route)
                    },
                    onOpenGoal = { id -> navController.navigate("goal_detail/" + id) }
                )
            }

            composable("goal_detail/{goalId}") { entry ->
                val goalId = entry.arguments?.getString("goalId")?.toIntOrNull()
                if (goalId != null) {
                    GoalDetailScreen(goalId = goalId, onBack = { navController.popBackStack() })
                }
            }

            composable(Screen.CreateGoal.route) {
                CreateGoalScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }


            // ─────────────────────────────────────────────────────────────────
            // Planned expenses
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.PlannedExpenses.route) {
                PlannedExpensesScreen(
                    onAddPlannedExpense = {
                        navController.navigate(
                            Screen.CreatePlannedExpense.route
                        )
                    }
                )
            }

            composable(Screen.CreatePlannedExpense.route) {
                CreatePlannedExpenseScreen(
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }


            // ─────────────────────────────────────────────────────────────────
            // Financial modules — account balances, budgets, recurring, categories & AI.
            composable(Screen.More.route) {
                FinFlowModulesScreen(
                    updateViewModel = updateViewModel,
                    userId = uiState.user?.id,
                    onReauthenticate = { authViewModel.logout() },
                    isAdmin = uiState.user?.role in setOf("ADMIN", "ROLE_ADMIN"),
                    initialSection = requestedModule,
                )
            }

            // Advisor legacy route, available for compatibility.
            // Advisor
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.Advisor.route) {
                FinFlowModulesScreen(
                    updateViewModel = updateViewModel,
                    userId = uiState.user?.id,
                    onReauthenticate = { authViewModel.logout() },
                    isAdmin = uiState.user?.role in setOf("ADMIN", "ROLE_ADMIN"),
                    initialSection = "advisor",
                )
            }
        }
    }
}

