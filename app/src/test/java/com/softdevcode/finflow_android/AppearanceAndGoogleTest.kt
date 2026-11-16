package com.softdevcode.finflow_android

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.data.repository.AuthRepository
import com.softdevcode.finflow_android.domain.model.AppTheme
import com.softdevcode.finflow_android.domain.model.FinFlowPrivacyPolicy
import com.softdevcode.finflow_android.ui.screens.auth.AuthViewModel
import com.softdevcode.finflow_android.ui.screens.auth.LoginScreen
import com.softdevcode.finflow_android.ui.screens.auth.RegisterScreen
import com.softdevcode.finflow_android.ui.theme.FinFlowTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.lang.reflect.Proxy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-mdpi", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppearanceAndGoogleTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private fun authViewModel(): AuthViewModel {
        val tokens = TokenManager(compose.activity.applicationContext)
        val api = Proxy.newProxyInstance(FinFlowApi::class.java.classLoader, arrayOf(FinFlowApi::class.java)) { _, method, _ ->
            when (method.name) {
                "getPrivacyPolicy" -> FinFlowPrivacyPolicy(version = "published-test-version", ready = true,
                    controllerName = "FinFlow", controllerNit = "test")
                else -> error("Unexpected request: " + method.name)
            }
        } as FinFlowApi
        return ViewModelProvider(compose.activity, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AuthViewModel(AuthRepository(api, tokens), tokens, api) as T
        })[AuthViewModel::class.java]
    }

    private fun screenshot(name: String): Bitmap {
        val view = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        val file = File("build/reports/menu/$name.png").apply { parentFile?.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    @Test fun googleLoginIsVisibleBeforeTheFormInEveryThemeWithLargeText() {
        val vm = authViewModel()
        var selected by mutableStateOf(AppTheme.LIGHT)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.3f)) {
                FinFlowTheme(darkTheme = selected.isDark, oledTheme = selected == AppTheme.OLED) {
                    LoginScreen(onNavigateToRegister = {}, onLoginSuccess = {}, viewModel = vm)
                }
            }
        }
        AppTheme.entries.forEach { option ->
            compose.runOnIdle { selected = option }
            val google = compose.onNodeWithText("Continuar con Google").assertIsDisplayed().assertIsEnabled()
            val email = compose.onNodeWithText("Correo electrónico")
            assertTrue(google.fetchSemanticsNode().boundsInRoot.top < email.fetchSemanticsNode().boundsInRoot.top)
        }
        compose.runOnIdle {
            val bitmap = screenshot("android-login-320-oled")
            assertEquals(android.graphics.Color.BLACK, bitmap.getPixel(1, bitmap.height / 2))
            assertEquals(android.graphics.Color.BLACK, compose.activity.window.statusBarColor)
            assertEquals(android.graphics.Color.BLACK, compose.activity.window.navigationBarColor)
            assertFalse(compose.activity.window.isNavigationBarContrastEnforced)
        }
    }

    @Test fun googleRegistrationIsVisibleAndNeedsConsentButNoPasswordFields() {
        val vm = authViewModel()
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.3f)) {
                FinFlowTheme(darkTheme = true, oledTheme = true) {
                    RegisterScreen(onNavigateToLogin = {}, onRegisterSuccess = {}, viewModel = vm)
                }
            }
        }
        compose.waitUntil(5_000) { vm.uiState.value.registrationPolicy != null }
        compose.onNodeWithText("Crear cuenta con Google").assertIsDisplayed().assertIsNotEnabled()
        compose.runOnIdle { screenshot("android-register-320-oled") }
        compose.onNodeWithText("Revisar privacidad para continuar").performClick()
        compose.onNodeWithContentDescription("Autorizar tratamiento de datos").performScrollTo().performClick()
        compose.onNodeWithText("Crear cuenta con Google").performScrollTo().assertIsEnabled()
        compose.onNode(hasText("Crear cuenta") and hasClickAction()).assertIsNotEnabled()
    }

    @Test fun savedOledSurvivesNewTokenManagersAndTheQuickThemeSwitch() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val first = TokenManager(context)
        try {
            first.setThemeMode(AppTheme.OLED)
            assertEquals(AppTheme.OLED, TokenManager(context).themeMode.first())
            first.toggleDarkMode()
            assertEquals(AppTheme.LIGHT, first.themeMode.first())
            first.toggleDarkMode()
            assertEquals(AppTheme.OLED, TokenManager(context).themeMode.first())
            first.setThemeMode(AppTheme.DARK)
            first.toggleDarkMode()
            first.toggleDarkMode()
            assertEquals(AppTheme.DARK, first.themeMode.first())
        } finally { first.setThemeMode(AppTheme.DARK) }
    }
}
