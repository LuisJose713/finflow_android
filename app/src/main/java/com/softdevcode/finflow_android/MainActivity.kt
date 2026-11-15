package com.softdevcode.finflow_android
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import com.softdevcode.finflow_android.data.notifications.DailyReminders
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import javax.inject.Inject
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
import com.softdevcode.finflow_android.ui.screens.navigation.AssistantMovementDraft
import com.softdevcode.finflow_android.ui.theme.FinFlowTheme
import com.softdevcode.finflow_android.ui.theme.DisplayCurrencyManager
import com.softdevcode.finflow_android.ui.updates.AppUpdateHost
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var assistantDraft by mutableStateOf<AssistantMovementDraft?>(null)

    @Inject lateinit var finFlowApi: FinFlowApi

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        assistantDraft = AssistantMovementDraft.fromIntent(intent)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val authViewModel: AuthViewModel = hiltViewModel()
            val updateViewModel: AppUpdateViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val darkMode by authViewModel.darkMode.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.user?.id, uiState.isAuthenticated) {
                DisplayCurrencyManager.initialize(
                    context, finFlowApi,
                    if (uiState.isAuthenticated) uiState.user?.id else null
                )
            }

            val navController = rememberNavController() // ✅ SOLO UNA VEZ
            
            val isReady = !uiState.isCheckingAuth
            LaunchedEffect(isReady, uiState.isAuthenticated) {
                if (!isReady) return@LaunchedEffect
                if (!uiState.isAuthenticated) {
                    DailyReminders.setSessionActive(context, false)
                } else {
                    DailyReminders.setSessionActive(context, true)
                    // Notification permission alone is not consent to recurring reminders.
                    // Only an explicit preference may enable them.
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
                            assistantDraft = assistantDraft,
                            onAssistantRequestHandled = { assistantDraft = null },
                    )
                }
            }
        }
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AssistantMovementDraft.fromIntent(intent)?.let { assistantDraft = it }
    }

}