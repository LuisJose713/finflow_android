package com.softdevcode.finflow_android.ui.screens.main

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Menu
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
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
import com.softdevcode.finflow_android.ui.theme.*
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import com.softdevcode.finflow_android.ui.tutorial.GuidedTourOverlay
import com.softdevcode.finflow_android.ui.tutorial.GuidedTourStep
import com.softdevcode.finflow_android.ui.tutorial.guideAnchor

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    updateViewModel: AppUpdateViewModel,
    authViewModel: AuthViewModel = hiltViewModel(),
    assistantDraft: AssistantMovementDraft? = null,
    onAssistantRequestHandled: () -> Unit = {},
) {
    var activeAssistantDraft by remember { mutableStateOf<AssistantMovementDraft?>(null) }
    val navController = rememberNavController()
    val context = LocalContext.current
    var guideStep by remember { mutableStateOf<GuidedTourStep?>(null) }
    var guideRect by remember { mutableStateOf<Rect?>(null) }
    var welcomeGuide by remember { mutableStateOf(false) }
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()
    val isAdmin = uiState.user?.role in setOf("ADMIN", "ROLE_ADMIN")
    val uid = uiState.user?.id
    val prefs = remember(context) { context.getSharedPreferences("finflow_guide", Context.MODE_PRIVATE) }
    LaunchedEffect(uiState.isAuthenticated, uid) {
        if (uiState.isAuthenticated && uid != null && assistantDraft == null &&
            !prefs.getBoolean("seen_$uid", false)) welcomeGuide = true
    }
    fun acknowledgeGuide() {
        uid?.let { prefs.edit().putBoolean("seen_$it", true).apply() }
        welcomeGuide = false
    }
    fun startGuide() {
        acknowledgeGuide()
        guideRect = null
        guideStep = GuidedTourStep.MOVEMENT
        navController.navigateToTopLevel(Screen.Dashboard.route)
    }
    fun endGuide() {
        acknowledgeGuide()
        guideStep = null
        guideRect = null
        navController.navigateToTopLevel(Screen.Dashboard.route)
    }
    fun nextGuide() {
        val next = guideStep?.next()
        if (next == null) endGuide()
        else { guideRect = null; guideStep = next }
    }
    val onGuideRect: (Rect) -> Unit = { if (guideRect != it) guideRect = it }
    LaunchedEffect(guideStep) {
        when (val destination = guideStep?.destination ?: return@LaunchedEffect) {
            Screen.Dashboard.route, Screen.Goals.route ->
                navController.navigateToTopLevel(destination)
            Screen.AddTransaction.route, Screen.CreateGoal.route ->
                if (navController.currentDestination?.route != destination)
                    navController.navigate(destination) { launchSingleTop = true }
            else -> navController.navigateToTopLevel(destination)
        }
    }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val preferences: UiPreferencesViewModel = hiltViewModel()
    val presentationState by preferences.state.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.user?.id) { preferences.load(uiState.user?.id) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var navigatingDrawer by remember { mutableStateOf(false) }
    val menuGesturesEnabled = isMainDestination(route) && !keyboardVisible &&
        guideStep == null && !welcomeGuide && !navigatingDrawer

    fun navigateFromDrawer(destination: () -> Unit) {
        // Finish the drawer animation before the destination transition begins.
        // Ignore repeat taps while the first navigation is still in progress.
        if (navigatingDrawer) return
        navigatingDrawer = true
        scope.launch {
            try {
                drawerState.close()
                destination()
            } finally {
                navigatingDrawer = false
            }
        }
    }
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
    // System assistant/launcher shortcuts only open the authenticated form.
    // The user must review and explicitly save the transaction.
    LaunchedEffect(assistantDraft, uiState.isAuthenticated) {
        if (assistantDraft != null && uiState.isAuthenticated) {
            // Keep the draft after acknowledging the external request, until
            // the authenticated user confirms or explicitly closes the form.
            activeAssistantDraft = assistantDraft
            navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
            onAssistantRequestHandled()
        }
    }

    CompositionLocalProvider(LocalPresentationControls provides PresentationControls(presentationState, preferences::setHidden)) {
    MainMenu(
        drawerState = drawerState,
        gesturesEnabled = menuGesturesEnabled,
        currentRoute = route,
        currentSection = module,
        userName = uiState.user?.name.orEmpty(),
        userEmail = uiState.user?.email.orEmpty(),
        isDark = darkMode,
        isAdmin = isAdmin,
        onClose = { scope.launch { drawerState.close() } },
        onNavigate = { target -> navigateFromDrawer { navController.navigateToTopLevel(target) } },
        onModule = { section -> navigateFromDrawer { openModule(section) } },
        onNewTransaction = {
            navigateFromDrawer {
                if (guideStep == GuidedTourStep.MOVEMENT) nextGuide()
                navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
            }
        },
        onToggleTheme = authViewModel::toggleDarkMode,
        onLogout = authViewModel::logout,
        onStartTutorial = { navigateFromDrawer { startGuide() } },
    ) {
        Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                if (title != null && route !in setOf(Screen.Dashboard.route, Screen.Goals.route, Screen.Transactions.route, Screen.PlannedExpenses.route)) TopAppBar(
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
                    onClick = {
                        if (guideStep == GuidedTourStep.MOVEMENT) nextGuide()
                        navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
                    },
                    modifier = Modifier.guideAnchor(guideStep == GuidedTourStep.MOVEMENT, onGuideRect),
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("Nuevo movimiento", fontWeight = FontWeight.SemiBold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            },

        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Dashboard.route,
                modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding).imePadding(),
                enterTransition = { finFlowScreenEnter() },
                exitTransition = { finFlowScreenExit() },
                popEnterTransition = { finFlowScreenEnter() },
                popExitTransition = { finFlowScreenExit() },
            ) {
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        userName = uiState.user?.name.orEmpty(),
                        onOpenMenu = { scope.launch { drawerState.open() } },
                        isDark = darkMode,
                        onToggleTheme = authViewModel::toggleDarkMode,
                        onLogout = authViewModel::logout,
                        onAddTransaction = {
                            if (guideStep == GuidedTourStep.MOVEMENT) nextGuide()
                            navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
                        },
                        onViewTransactions = { navController.navigateToTopLevel(Screen.Transactions.route) },
                        onViewPlannedExpenses = { navController.navigateToTopLevel(Screen.PlannedExpenses.route) },
                        onViewGoals = { navController.navigateToTopLevel(Screen.Goals.route) },
                        onViewAccounts = { openModule("accounts") },
                        onViewAdvisor = { openModule("advisor") },
                    )
                }
                composable(Screen.Transactions.route) {
                    TransactionsScreen(onOpenMenu = { scope.launch { drawerState.open() } }, onAddTransaction = {
                        navController.navigate(Screen.AddTransaction.route) { launchSingleTop = true }
                    })
                }
                composable(Screen.AddTransaction.route) {
                    AddTransactionScreen(
                        assistantDraft = activeAssistantDraft,
                        tutorialStep = guideStep,
                        onTutorialTarget = onGuideRect,
                        onBack = {
                            activeAssistantDraft = null
                            navController.popBackStack()
                        }
                    )
                }
                composable(Screen.Goals.route) {
                    GoalsScreen(
                    onOpenMenu = { scope.launch { drawerState.open() } },
                        onAddGoal = {
                            if (guideStep == GuidedTourStep.NEW_GOAL) nextGuide()
                            navController.navigate(Screen.CreateGoal.route) { launchSingleTop = true }
                        },
                        tutorialStep = guideStep,
                        onTutorialTarget = onGuideRect,
                        onOpenGoal = { navController.navigate("goal_detail/$it") { launchSingleTop = true } },
                    )
                }
                composable("goal_detail/{goalId}") { goalEntry ->
                    goalEntry.arguments?.getString("goalId")?.toIntOrNull()?.let { id ->
                        GoalDetailScreen(goalId = id, onBack = { navController.popBackStack() })
                    }
                }
                composable(Screen.CreateGoal.route) {
                    CreateGoalScreen(onBack = { navController.popBackStack() },
                        tutorialStep = guideStep, onTutorialTarget = onGuideRect)
                }
                composable(Screen.PlannedExpenses.route) {
                    PlannedExpensesScreen(onOpenMenu = { scope.launch { drawerState.open() } }, onAddPlannedExpense = {
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
                            userName = uiState.user?.name.orEmpty(),
                            userEmail = uiState.user?.email.orEmpty(),
                            onReauthenticate = authViewModel::logout,
                            isAdmin = isAdmin,
                            initialSection = section,
                            tutorialStep = guideStep,
                            onTutorialTarget = onGuideRect,
                        )
                    }
                }
                composable(Screen.Advisor.route) {
                    FinFlowModulesScreen(
                        updateViewModel = updateViewModel,
                        userId = uiState.user?.id,
                        userName = uiState.user?.name.orEmpty(),
                        userEmail = uiState.user?.email.orEmpty(),
                        onReauthenticate = authViewModel::logout,
                        isAdmin = isAdmin,
                        initialSection = "advisor",
                        tutorialStep = guideStep,
                        onTutorialTarget = onGuideRect,
                    )
                }
            }
        }
        guideStep?.let { step ->
            GuidedTourOverlay(step, guideRect, next = ::nextGuide, exit = ::endGuide)
        }
        }
    }
    }
    DrawerBackNavigation(enabled = menuGesturesEnabled && drawerState.isClosed) { drawerState.open() }

    if (welcomeGuide && guideStep == null) {
        AlertDialog(
            onDismissRequest = ::acknowledgeGuide,
            title = { Text("Conoce FinFlow paso a paso") },
            text = { Text("Te mostraremos dónde crear movimientos, metas, billeteras y categorías. Sin guardar datos de prueba.") },
            confirmButton = { TextButton(onClick = ::startGuide) { Text("Empezar guía") } },
            dismissButton = { TextButton(onClick = ::acknowledgeGuide) { Text("Más tarde") } },
        )
    }
}
