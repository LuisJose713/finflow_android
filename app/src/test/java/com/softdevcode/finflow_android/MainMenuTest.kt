package com.softdevcode.finflow_android

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import com.softdevcode.finflow_android.ui.screens.main.MainMenu
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.theme.FinFlowTheme
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w320dp-h640dp-mdpi", application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainMenuTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun verticalMenuRemainsUsableAt320DpWithLargeTextAndUpdatesAreReachable() {
        var route by mutableStateOf(Screen.Dashboard.route)
        var section by mutableStateOf<String?>(null)
        var closeCount = 0
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f, fontScale = 1.3f)) {
                FinFlowTheme(darkTheme = true) {
                    val drawer = rememberDrawerState(DrawerValue.Open)
                    MainMenu(drawer, true, route, section, "Luis José", true, false,
                        onClose = { closeCount++ },
                        onNavigate = { route = it; section = null },
                        onModule = { route = Screen.Module.route; section = it },
                        onToggleTheme = {}, onLogout = {},
                    ) { Box(Modifier.fillMaxSize()) }
                }
            }
        }
        compose.onNodeWithContentDescription("Cerrar menú").assertIsDisplayed()
        compose.runOnIdle {
            // PixelCopy requires hardware frame callbacks unavailable in Robolectric.
            val view = compose.activity.window.decorView
            val screenshot = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(screenshot))
            val output = File("build/reports/menu/android-menu-320-dark.png").apply { parentFile?.mkdirs() }
            output.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        compose.onNodeWithText("Movimientos").performClick()
        compose.onNodeWithText("Movimientos").assertIsSelected()
        compose.onNodeWithText("Actualizaciones").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("updates", section)
            assertEquals(2, closeCount)
        }
        compose.onNodeWithText("Administración").assertDoesNotExist()
        compose.onNodeWithText("Inicio").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(Screen.Dashboard.route, route) }
    }
}
