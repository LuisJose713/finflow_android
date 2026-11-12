package com.softdevcode.finflow_android
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.softdevcode.finflow_android.data.notifications.DailyReminders
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.navigation.FinFlowNavGraph
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.theme.FinFlowTheme
import com.softdevcode.finflow_android.ui.updates.AppUpdateHost
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val notificationPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                DailyReminders.setEnabled(context, granted)
            }
            val authViewModel: AuthViewModel = hiltViewModel()
            val updateViewModel: AppUpdateViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()

            val navController = rememberNavController() // ✅ SOLO UNA VEZ
            
            val isReady = !uiState.isCheckingAuth
            LaunchedEffect(isReady, uiState.isAuthenticated) {
                if (!isReady) return@LaunchedEffect
                if (!uiState.isAuthenticated) {
                    DailyReminders.setSessionActive(context, false)
                } else {
                    DailyReminders.setSessionActive(context, true)
                    if (!DailyReminders.wasAsked(context)) {
                        DailyReminders.markAsked(context)
                        if (DailyReminders.notificationPermission(context)) {
                            DailyReminders.setEnabled(context, true)
                        } else if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            }
            splashScreen.setKeepOnScreenCondition {
                uiState.isCheckingAuth
            }

            FinFlowTheme(darkTheme = darkMode) {
                AppUpdateHost(updateViewModel)
                if (isReady) {

                    val startDestination = if (uiState.isAuthenticated)
                        Screen.Main.route else Screen.Login.route

                    FinFlowNavGraph(
                        startDestination = startDestination,
                        navController    = navController, // ✅ reutilizado
                        authViewModel    = authViewModel,
                        updateViewModel  = updateViewModel,
                    )
                }
            }
        }
    }
}