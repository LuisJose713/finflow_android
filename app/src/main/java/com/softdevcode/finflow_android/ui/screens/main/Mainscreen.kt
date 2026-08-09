package com.softdevcode.finflow_android.ui.screens.main

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardScreen
import com.softdevcode.finflow_android.ui.screens.navigation.Screen

// ── Tab definition ────────────────────────────────────────────────────────────
private data class BottomTab(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

private val tabs = listOf(
    BottomTab(Screen.Dashboard,       "Dashboard",    Icons.Outlined.GridView,       Icons.Filled.GridView),
    BottomTab(Screen.Transactions,    "Movimientos",  Icons.Outlined.SwapHoriz,      Icons.Filled.SwapHoriz),
    BottomTab(Screen.Goals,           "Metas",        Icons.Outlined.TrackChanges,   Icons.Filled.TrackChanges),
    BottomTab(Screen.PlannedExpenses, "Planeados",    Icons.Outlined.CalendarMonth,  Icons.Filled.CalendarMonth),
    BottomTab(Screen.Advisor,         "Asesor IA",    Icons.Outlined.SmartToy,       Icons.Filled.SmartToy),
)

// ── MainScreen ────────────────────────────────────────────────────────────────
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val navController    = rememberNavController()
    val navBackStack     by navController.currentBackStackEntryAsState()
    val currentDest      = navBackStack?.destination
    val uiState          by authViewModel.uiState.collectAsStateWithLifecycle()
    val darkMode         by authViewModel.darkMode.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isAuthenticated) {
        if (!uiState.isAuthenticated) onLogout()
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor    = MaterialTheme.colorScheme.surface,
                tonalElevation    = 0.dp,
            ) {
                tabs.forEach { tab ->
                    val isSelected = currentDest?.hierarchy
                        ?.any { it.route == tab.screen.route } == true

                    NavigationBarItem(
                        selected = isSelected,
                        onClick  = {
                            navController.navigate(tab.screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) tab.iconSelected else tab.icon,
                                contentDescription = tab.label,
                            )
                        },
                        label = {
                            Text(
                                text  = tab.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = com.softdevcode.finflow_android.ui.theme.Primary,
                            selectedTextColor   = com.softdevcode.finflow_android.ui.theme.Primary,
                            indicatorColor      = com.softdevcode.finflow_android.ui.theme.Primary.copy(alpha = 0.12f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                        ),
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Dashboard.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn(animationSpec = androidx.compose.animation.core.tween(180)) },
            exitTransition   = { fadeOut(animationSpec = androidx.compose.animation.core.tween(180)) },
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    userName = uiState.user?.name ?: "",
                    isDark = darkMode,
                    onToggleTheme = { authViewModel.toggleDarkMode() },
                    onLogout = { authViewModel.logout() }
                )
            }
            composable(Screen.Transactions.route) {
                PlaceholderScreen(title = "Transacciones", user = "", isDark = darkMode,
                    onToggleTheme = { authViewModel.toggleDarkMode() }, onLogout = {})
            }
            composable(Screen.Goals.route) {
                PlaceholderScreen(title = "Metas", user = "", isDark = darkMode,
                    onToggleTheme = { authViewModel.toggleDarkMode() }, onLogout = {})
            }
            composable(Screen.PlannedExpenses.route) {
                PlaceholderScreen(title = "Gastos Planeados", user = "", isDark = darkMode,
                    onToggleTheme = { authViewModel.toggleDarkMode() }, onLogout = {})
            }
            composable(Screen.Advisor.route) {
                PlaceholderScreen(title = "Asesor IA", user = "", isDark = darkMode,
                    onToggleTheme = { authViewModel.toggleDarkMode() }, onLogout = {})
            }
        }
    }
}

// ── Placeholder temporal hasta implementar cada pantalla ──────────────────────
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
                        Text(title, style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold))
                        if (user.isNotEmpty()) {
                            Text("Hola, ${user.split(" ").first()} 👋",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                },
                actions = {
                    // Dark mode toggle
                    IconButton(onClick = onToggleTheme) {
                        AnimatedContent(targetState = isDark, label = "theme") { dark ->
                            Icon(
                                imageVector = if (dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                                contentDescription = "Toggle theme",
                                tint = if (dark) com.softdevcode.finflow_android.ui.theme.Warning else com.softdevcode.finflow_android.ui.theme.Primary,
                            )
                        }
                    }
                    // Logout (only on Dashboard)
                    if (onLogout != {}) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.Outlined.Logout, contentDescription = "Salir",
                                tint = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Cada pantalla real reemplazará este placeholder
        }
    }
}