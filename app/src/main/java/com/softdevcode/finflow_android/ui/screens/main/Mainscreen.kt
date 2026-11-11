package com.softdevcode.finflow_android.ui.screens.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.softdevcode.finflow_android.ui.screens.goals.GoalsScreen
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardScreen
import com.softdevcode.finflow_android.ui.screens.goals.CreateGoalScreen
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.CreatePlannedExpenseScreen
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.PlannedExpensesScreen
import com.softdevcode.finflow_android.ui.screens.transactions.AddTransactionScreen
import com.softdevcode.finflow_android.ui.screens.transactions.TransactionsScreen
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy
import com.softdevcode.finflow_android.ui.theme.FinFlowGold
import com.softdevcode.finflow_android.ui.theme.Primary
import com.softdevcode.finflow_android.ui.theme.Warning


// ─────────────────────────────────────────────────────────────────────────────
// Bottom navigation
// ─────────────────────────────────────────────────────────────────────────────

private data class BottomTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

private val tabs = listOf(
    BottomTab(
        Screen.Dashboard,
        "Inicio",
        Icons.Outlined.GridView,
        Icons.Filled.GridView
    ),
    BottomTab(
        Screen.Transactions,
        "Movimientos",
        Icons.Outlined.SwapHoriz,
        Icons.Filled.SwapHoriz
    ),
    BottomTab(
        Screen.Goals,
        "Metas",
        Icons.Outlined.TrackChanges,
        Icons.Filled.TrackChanges
    ),
    BottomTab(
        Screen.PlannedExpenses,
        "Planeados",
        Icons.Outlined.CalendarMonth,
        Icons.Filled.CalendarMonth
    ),
    BottomTab(
        Screen.Advisor,
        "Asesor IA",
        Icons.Outlined.SmartToy,
        Icons.Filled.SmartToy
    ),
)


// ─────────────────────────────────────────────────────────────────────────────
// Main Screen
// ─────────────────────────────────────────────────────────────────────────────

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
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

            NavigationBar(
                containerColor = FinFlowNavy,
                tonalElevation = 0.dp,
            ) {

                tabs.forEach { tab ->

                    val isSelected =
                        currentDestination
                            ?.hierarchy
                            ?.any { it.route == tab.screen.route } == true


                    NavigationBarItem(

                        selected = isSelected,

                        onClick = {

                            navController.navigate(tab.screen.route) {

                                popUpTo(
                                    navController.graph.findStartDestination().id
                                ) {
                                    saveState = true
                                }

                                launchSingleTop = true

                                restoreState = true
                            }
                        },

                        icon = {

                            Icon(
                                imageVector =
                                    if (isSelected) {
                                        tab.iconSelected
                                    } else {
                                        tab.icon
                                    },

                                contentDescription = tab.label,
                            )
                        },

                        label = {

                            Text(
                                text = tab.label,

                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontWeight =
                                            if (isSelected) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            }
                                    )
                            )
                        },

                        colors =
                            NavigationBarItemDefaults.colors(

                                selectedIconColor = FinFlowGold,

                                selectedTextColor = FinFlowGold,

                                indicatorColor = Color.White.copy(alpha = 0.10f),

                                unselectedIconColor = Color.White.copy(alpha = 0.72f),

                                unselectedTextColor = Color.White.copy(alpha = 0.72f),
                            )
                    )
                }
            }
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
                    onViewTransactions = { navController.navigate(Screen.Transactions.route) { launchSingleTop = true } },
                    onViewPlannedExpenses = { navController.navigate(Screen.PlannedExpenses.route) { launchSingleTop = true } },
                    onViewGoals = { navController.navigate(Screen.Goals.route) { launchSingleTop = true } },
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
                    }
                )
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
            // Advisor
            // ─────────────────────────────────────────────────────────────────

            composable(Screen.Advisor.route) {

                PlaceholderScreen(
                    title = "Asesor IA",
                    user = "",
                    isDark = darkMode,

                    onToggleTheme = {
                        authViewModel.toggleDarkMode()
                    },

                    onLogout = { authViewModel.logout() }
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// Placeholder
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen(
    title: String,
    user: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
) {

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Column {

                        Text(
                            text = title,

                            style =
                                MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                        )

                        if (user.isNotEmpty()) {

                            Text(
                                text =
                                    "Hola, ${user.split(" ").first()} 👋",

                                style =
                                    MaterialTheme.typography.bodySmall,

                                color =
                                    MaterialTheme.colorScheme
                                        .onSurface
                                        .copy(alpha = 0.5f)
                            )
                        }
                    }
                },

                actions = {

                    // ─────────────────────────────────────────────────────────
                    // Dark mode
                    // ─────────────────────────────────────────────────────────

                    IconButton(
                        onClick = onToggleTheme
                    ) {

                        AnimatedContent(
                            targetState = isDark,
                            label = "theme"
                        ) { dark ->

                            Icon(

                                imageVector =
                                    if (dark) {
                                        Icons.Outlined.LightMode
                                    } else {
                                        Icons.Outlined.DarkMode
                                    },

                                contentDescription =
                                    "Cambiar tema",

                                tint =
                                    if (dark) {
                                        Warning
                                    } else {
                                        Primary
                                    }
                            )
                        }
                    }


                    // ─────────────────────────────────────────────────────────
                    // Logout
                    // ─────────────────────────────────────────────────────────

                    if (onLogout !== {}) {

                        IconButton(
                            onClick = onLogout
                        ) {

                            Icon(
                                imageVector = Icons.Outlined.Logout,

                                contentDescription = "Salir",

                                tint =
                                    MaterialTheme.colorScheme
                                        .onSurface
                                        .copy(alpha = 0.5f)
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.background
                    )
            )
        }
    ) { padding ->

        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = Primary.copy(alpha = 0.12f),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SmartToy,
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier
                                .padding(18.dp)
                                .size(38.dp),
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "Asesor IA de FinFlow",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Estamos preparando una experiencia de asesoría nativa para Android. Tus movimientos, metas y gastos planeados ya están disponibles en las otras pestañas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
