package com.softdevcode.finflow_android.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.screens.navigation.navigateToTopLevel
import com.softdevcode.finflow_android.ui.theme.FinFlowGoldPale
import com.softdevcode.finflow_android.ui.theme.FinFlowNavy

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
        Screen.More,
        "Más",
        Icons.Outlined.MoreHoriz,
        Icons.Filled.MoreHoriz
    ),
)


@Composable
internal fun MainBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentDestination = entry?.destination
    NavigationBar(containerColor = FinFlowNavy, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == tab.screen.route } == true
            NavigationBarItem(
                selected = isSelected,
                onClick = { navController.navigateToTopLevel(tab.screen.route) },
                icon = {
                    Icon(if (isSelected) tab.iconSelected else tab.icon, contentDescription = tab.label)
                },
                label = {
                    Text(tab.label, style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    ))
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = FinFlowGoldPale,
                    selectedTextColor = Color.White,
                    indicatorColor = Color(0xFF22334A),
                    unselectedIconColor = Color(0xFFAEBED2),
                    unselectedTextColor = Color(0xFFAEBED2),
                ),
            )
        }
    }
}
