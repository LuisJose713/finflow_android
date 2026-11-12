package com.softdevcode.finflow_android.ui.screens.main

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Menu
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardScreen
import com.softdevcode.finflow_android.ui.screens.goals.*
import com.softdevcode.finflow_android.ui.screens.modules.FinFlowModulesScreen
import com.softdevcode.finflow_android.ui.screens.navigation.*
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.*
import com.softdevcode.finflow_android.ui.screens.transactions.*
import com.softdevcode.finflow_android.ui.theme.Primary
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    updateViewModel: AppUpdateViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()
    val isAdmin = uiState.user?.role in setOf("ADMIN", "ROLE_ADMIN")
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val module = entry?.arguments?.getString("section")
    val title = when (route) {
        Screen.Module.route -> moduleTitle(module.orEmpty())
        Screen.PlannedExpenses.route -> "Gastos planeados"
        Screen.Advisor.route -> "Asesor IA"
        Screen.Dashboard.route -> "Inicio"
        Screen.Transactions.route -> "Movimientos"
        Screen.Goals.route -> "Metas"
        Screen.More.route -> "FinFlow"
        else -> null
    }

    fun openModule(section: String) {
        if (moduleTitle(section) != null && (section != "admin" || isAdmin)) {
            navController.navigateToTopLevel("module/$section")
        }
    }

    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) onLogout()
    }

    MainMenu(
        drawerState = drawerState,
        gesturesEnabled = isMainDestination(route) && !keyboardVisible,
        currentRoute = route,
        currentSection = module,
        userName = uiState.user?.name.orEmpty(),
        isDark = darkMode,
        isAdmin = isAdmin,
        onClose = { scope.launch { drawerState.close() } },
        onNavigate = { navController.navigateToTopLevel(it) },
        onModule = ::openModule,
        onToggleTheme = authViewModel::toggleDarkMode,
        onLogout = authViewModel::logout,
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (title != null) TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Outlined.Menu, "Abrir menú")
                        }
                    },
                    actions = {
                        if (route == Screen.Module.route || route == Screen.PlannedExpenses.route || route == Screen.Advisor.route) {
                            IconButton(onClick = { navController.navigateToTopLevel(Screen.Dashboard.route) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver a Inicio")
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (route == Screen.Dashboard.route && !keyboardVisible) ExtendedFloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true } },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo movimiento", fontWeight = FontWeight.SemiBold) },
                    containerColor = Primary,
                    contentColor = Color.White,
                )
            },

        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
                enterTransition = { fadeIn(tween(120)) },
                exitTransition = { fadeOut(tween(120)) },
                popEnterTransition = { fadeIn(tween(120)) },
                popExitTransition = { fadeOut(tween(120)) },
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        userName = uiState.user?.name.orEmpty(),
                        isDark = darkMode,
                        onToggleTheme = authViewModel::toggleDarkMode,
                        onLogout = authViewModel::logout,
                        onAddTransaction = { navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true } },
                        onViewTransactions = { navController.navigateToTopLevel(Screen.Transactions.route) },
                        onViewPlannedExpenses = { navController.navigateToTopLevel(Screen.PlannedExpenses.route) },
                        onViewGoals = { navController.navigateToTopLevel(Screen.Goals.route) },
                        onViewAccounts = { openModule("accounts") },
                        onViewAdvisor = { openModule("advisor") },
                    )
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
                    })
                }
                composable(Screen.AddTransaction.route) {
                    AddTransactionScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Goals.route) {
                    GoalsScreen(
                        onAddGoal = { navController.navigate(Screen.CreateGoal.route) { launchSingleTop = true } },
                        onOpenGoal = { navController.navigate("goal_detail/$it") { launchSingleTop = true } },
                    )
                }
                composable("goal_detail/{goalId}") { goalEntry ->
                    goalEntry.arguments?.getString("goalId")?.toIntOrNull()?.let { id ->
                        GoalDetailScreen(goalId = id, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.CreateGoal.route) {
                    CreateGoalScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.PlannedExpenses.route) {
                    PlannedExpensesScreen(onAddPlannedExpense = {
                        navController.navigate(Screen.CreatePlannedExpense.route) { launchSingleTop = true }
                    })
                }
                composable(Screen.CreatePlannedExpense.route) {
                    CreatePlannedExpenseScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.More.route) {
                    LaunchedEffect(Unit) {
                        navController.navigateToTopLevel(Screen.Dashboard.route)
                        drawerState.open()
                    }
                }
                composable(Screen.Module.route) { moduleEntry ->
                    val section = moduleEntry.arguments?.getString("section").orEmpty()
                    if (moduleTitle(section) != null && (section != "admin" || isAdmin)) {
                        FinFlowModulesScreen(
                            updateViewModel = updateViewModel,
                            userId = uiState.user?.id,
                            onReauthenticate = authViewModel::logout,
                            isAdmin = isAdmin,
                            initialSection = section,
                        )
                    }
                }
                composable(Screen.Advisor.route) {
                    FinFlowModulesScreen(
                        updateViewModel = updateViewModel,
                        userId = uiState.user?.id,
                        onReauthenticate = authViewModel::logout,
                        isAdmin = isAdmin,
                        initialSection = "advisor",
                    )
                }
            }
    }
    }
}
