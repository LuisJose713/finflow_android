package com.softdevcode.finflow_android.ui.screens.navigation

import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/** Shared by the bottom menu and the dashboard shortcuts. */
internal fun NavHostController.navigateToTopLevel(route: String) {
    if (currentDestination?.hierarchy?.any { it.route == route } == true) return

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
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
