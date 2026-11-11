package com.softdevcode.finflow_android.ui.screens.navigation

import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.*
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.auth.LoginScreen
import com.softdevcode.finflow_android.ui.screens.auth.RegisterScreen
import com.softdevcode.finflow_android.ui.screens.main.MainScreen


sealed class Screen(val route: String) {
    data object Login           : Screen("login")
    data object Register        : Screen("register")
    data object Main            : Screen("main")
    data object Dashboard       : Screen("dashboard")
    data object Transactions    : Screen("transactions")
    data object AddTransaction  : Screen("add_transaction")
    data object Goals           : Screen("goals")
    data object PlannedExpenses : Screen("planned_expenses")

    object CreatePlannedExpense : Screen("create_planned_expense")
    data object Advisor         : Screen("advisor")
    data object More            : Screen("more")
    data object CreateGoal : Screen("create_goal")
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FinFlowNavGraph(
    startDestination: String,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel,
    updateViewModel: AppUpdateViewModel,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        enterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 5 } },
        exitTransition   = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { -it / 5 } },
        popEnterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 5 } },
        popExitTransition   = { fadeOut(tween(180)) + slideOutHorizontally(tween(180)) { it / 5 } },
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel,
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                viewModel = authViewModel,
            )
        }
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                },
                authViewModel = authViewModel,
                updateViewModel = updateViewModel,
            )
        }
    }
}