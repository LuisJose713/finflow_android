package com.softdevcode.finflow_android
import android.os.Bundle
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
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()

            // Keep splash visible while checking auth
            var isReady by remember { mutableStateOf(false) }
            LaunchedEffect(uiState) { isReady = true }
            splashScreen.setKeepOnScreenCondition { !isReady }

            FinFlowTheme(darkTheme = darkMode) {
                if (isReady) {
                    val startDestination = if (uiState.isAuthenticated)
                        Screen.Main.route else Screen.Login.route

                    FinFlowNavGraph(
                        startDestination = startDestination,
                        navController    = rememberNavController(),
                        authViewModel    = authViewModel,
                    )
                }
            }
        }
    }
}