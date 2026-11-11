package com.softdevcode.finflow_android

import androidx.navigation.NavDestination
import androidx.navigation.Navigator
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.screens.navigation.navigateToTopLevel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = android.app.Application::class)
class TopLevelNavigationTest {
    @Navigator.Name("test")
    class TestScreenNavigator : Navigator<NavDestination>() {
        override fun createDestination() = NavDestination(this)
    }

    private lateinit var controller: TestNavHostController
    private val routes = listOf(Screen.Dashboard.route, Screen.Transactions.route, Screen.Goals.route,
        Screen.PlannedExpenses.route, Screen.More.route, Screen.AddTransaction.route, Screen.CreateGoal.route)

    @Before fun setup() {
        controller = TestNavHostController(ApplicationProvider.getApplicationContext())
        val navigator = TestScreenNavigator()
        controller.navigatorProvider.addNavigator(navigator)
        controller.graph = controller.createGraph(startDestination = Screen.Dashboard.route) {
            routes.forEach { route -> addDestination(navigator.createDestination().apply { this.route = route }) }
        }
    }

    @Test fun homeReturnsFromEveryMenuTabAndRemainsUsable() {
        repeat(3) {
            routes.take(5).drop(1).forEach { route ->
                controller.navigateToTopLevel(route)
                assertEquals(route, controller.currentDestination?.route)
                controller.navigateToTopLevel(Screen.Dashboard.route)
                assertEquals(Screen.Dashboard.route, controller.currentDestination?.route)
                assertNull(controller.previousBackStackEntry)
            }
        }
    }

    @Test fun homeDoesNotRestoreAFormSavedUnderTheDashboard() {
        controller.navigate(Screen.AddTransaction.route)
        controller.navigateToTopLevel(Screen.More.route)
        controller.navigateToTopLevel(Screen.Goals.route)
        controller.navigate(Screen.CreateGoal.route)
        controller.navigateToTopLevel(Screen.Dashboard.route)
        assertEquals(Screen.Dashboard.route, controller.currentDestination?.route)
        assertNull(controller.previousBackStackEntry)
    }

    @Test fun quickLinksAndRepeatedMenuTapsDoNotDuplicateDestinations() {
        repeat(10) { controller.navigateToTopLevel(Screen.Dashboard.route) }
        assertNull(controller.previousBackStackEntry)
        controller.navigateToTopLevel(Screen.Transactions.route)
        val entry = controller.currentBackStackEntry
        repeat(10) { controller.navigateToTopLevel(Screen.Transactions.route) }
        assertSame(entry, controller.currentBackStackEntry)
        controller.navigateToTopLevel(Screen.Dashboard.route)
        assertEquals(Screen.Dashboard.route, controller.currentDestination?.route)
    }
}
