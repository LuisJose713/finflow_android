package com.softdevcode.finflow_android.ui.screens.navigation

import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Shared by the vertical drawer and the dashboard shortcuts. */
internal fun NavHostController.navigateToTopLevel(route: String) {
    val currentRoute = currentDestination?.route
    val currentModule = currentBackStackEntry?.arguments?.getString("section")
    if (currentDestination?.hierarchy?.any { it.route == route } == true ||
        (currentRoute == Screen.Module.route && route == "module/$currentModule")) return

    if (route == Screen.Dashboard.route) {
        // Home is the live root, not a saved tab stack. Restoring a stack saved under
        // the start destination can reopen the screen the user was trying to leave.
        if (!popBackStack(Screen.Dashboard.route, inclusive = false)) {
            navigate(Screen.Dashboard.route) {
                popUpTo(graph.id)
                launchSingleTop = true
            }
        }
        return
    }

    navigate(route) {
        popUpTo(graph.findStartDestination().id)
        launchSingleTop = true
        restoreState = false
    }
}

/** Forms retain their own back action and do not expose drawer gestures. */
internal fun isMainDestination(route: String?): Boolean = route in setOf(
    Screen.Dashboard.route, Screen.Transactions.route, Screen.Goals.route,
    Screen.More.route, Screen.Module.route, Screen.PlannedExpenses.route, Screen.Advisor.route,
)
