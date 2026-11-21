package com.softdevcode.finflow_android
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.softdevcode.finflow_android.ui.screens.transactions.ReceiptTextParser
import android.os.Bundle
import androidx.compose.ui.platform.LocalContext
import com.softdevcode.finflow_android.data.notifications.DailyReminders
import com.softdevcode.finflow_android.data.notifications.NativeMessages
import com.softdevcode.finflow_android.data.local.TokenManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
import com.softdevcode.finflow_android.domain.model.AppTheme
import com.softdevcode.finflow_android.ui.theme.DisplayCurrencyManager
import com.softdevcode.finflow_android.ui.updates.AppUpdateHost
import com.softdevcode.finflow_android.ui.updates.AppUpdateViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var assistantDraft by mutableStateOf<AssistantMovementDraft?>(null)

    @Inject lateinit var finFlowApi: FinFlowApi
    @Inject lateinit var notificationTokens: TokenManager

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        assistantDraft = AssistantMovementDraft.fromIntent(intent)
        enableEdgeToEdge()

        // ACTION_SEND from the photo gallery or another scanner.
        // Never persist the shared image or post a transaction directly.
        handleSharedReceipt(intent)
        setContent {
            val context = LocalContext.current
            val authViewModel: AuthViewModel = hiltViewModel()
            val updateViewModel: AppUpdateViewModel = hiltViewModel()
            val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
            val themeMode by authViewModel.themeMode.collectAsStateWithLifecycle()
            LaunchedEffect(uiState.user?.id, uiState.isAuthenticated) {
                DisplayCurrencyManager.initialize(
                    context, finFlowApi,
                    if (uiState.isAuthenticated) uiState.user?.id else null
                )
            }

            val navController = rememberNavController() // ✅ SOLO UNA VEZ
            
            val isReady = !uiState.isCheckingAuth
            LaunchedEffect(isReady, uiState.isAuthenticated, uiState.user?.id) {
                if (!isReady || !uiState.isAuthenticated) return@LaunchedEffect
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (isActive) {
                        try { NativeMessages.sync(context, finFlowApi, notificationTokens) }
                        catch (ex: CancellationException) { throw ex }
                        catch (_: Exception) { /* The next sync retries without interrupting navigation. */ }
                        delay(30_000)
                    }
                }
            }
            LaunchedEffect(isReady, uiState.isAuthenticated, uiState.user?.id) {
                if (!isReady) return@LaunchedEffect
                if (!uiState.isAuthenticated) {
                    DailyReminders.setSessionActive(context, false)
                } else {
                    DailyReminders.setSessionActive(context, true)
                    DailyReminders.setEnabled(context, uiState.user?.id?.let { NativeMessages.enabled(context, it) } == true)
                    // Notification permission alone is not consent to recurring reminders.
                    // Only an explicit preference may enable them.
                }
                NativeMessages.bindSession(context, if (uiState.isAuthenticated) uiState.user?.id else null)
            }
            splashScreen.setKeepOnScreenCondition {
                uiState.isCheckingAuth
            }

            FinFlowTheme(darkTheme = themeMode.isDark, oledTheme = themeMode == AppTheme.OLED) {
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
    override fun onResume() {
        super.onResume()
        // Reconcile OS permission/channel changes made outside the app.
        DailyReminders.reschedule(this)
        lifecycleScope.launch {
            NativeMessages.bindSession(this@MainActivity, notificationTokens.userId.first())
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        AssistantMovementDraft.fromIntent(intent)?.let { assistantDraft = it }
        handleSharedReceipt(intent)
    }

    private fun handleSharedReceipt(incoming: Intent?) {
        if (incoming?.action != Intent.ACTION_SEND ||
            incoming.type?.startsWith("image/") != true) return
        @Suppress("DEPRECATION")
        val imageUri = incoming.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: return
        try {
            val image = InputImage.fromFilePath(this, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(image)
                .addOnSuccessListener { response ->
                    val draft = ReceiptTextParser.parse(response.text)
                    if (draft == null) {
                        Toast.makeText(this, "No encontramos texto en la factura", Toast.LENGTH_LONG).show()
                    } else {
                        // MainScreen requires authentication and navigates to
                        // the editable confirmation form. Nothing is saved yet.
                        assistantDraft = AssistantMovementDraft(
                            type = "expense", title = draft.title,
                            amount = draft.amount, category = null
                        )
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "No pudimos leer la factura", Toast.LENGTH_LONG).show()
                }
                .addOnCompleteListener { recognizer.close() }
        } catch (_: Exception) {
            Toast.makeText(this, "No pudimos abrir la imagen compartida", Toast.LENGTH_LONG).show()
        }
    }

}
