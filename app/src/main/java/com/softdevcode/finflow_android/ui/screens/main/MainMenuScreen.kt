package com.softdevcode.finflow_android.ui.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Overline
import com.softdevcode.finflow_android.ui.theme.FinFlowV4Wordmark

private data class MenuItem(val key: String, val title: String, val icon: ImageVector)
private val mainItems = listOf(
    MenuItem(Screen.Dashboard.route, "Inicio", Icons.Outlined.Home),
    MenuItem(Screen.Transactions.route, "Movimientos", Icons.Outlined.SwapHoriz),
    MenuItem(Screen.Goals.route, "Metas", Icons.Outlined.TrackChanges),
    MenuItem(Screen.PlannedExpenses.route, "Gastos planeados", Icons.Outlined.CalendarMonth),
)
private val financeItems = listOf(
    MenuItem("accounts", "Cuentas y billeteras", Icons.Outlined.AccountBalanceWallet),
    MenuItem("budgets", "Presupuestos", Icons.Outlined.PieChartOutline),
    MenuItem("recurring", "Recurrentes", Icons.Outlined.Autorenew),
    MenuItem("categories", "Categorías", Icons.Outlined.Category),
    MenuItem("advisor", "Asesor IA", Icons.Outlined.AutoAwesome),
    MenuItem("reports", "Reportes", Icons.Outlined.Assessment),
)
private val accountItems = listOf(
    MenuItem("billing", "Planes", Icons.Outlined.WorkspacePremium),
    MenuItem("security", "Seguridad", Icons.Outlined.Shield),
    MenuItem("privacy", "Privacidad y datos", Icons.Outlined.PrivacyTip),
    MenuItem("updates", "Actualizaciones", Icons.Outlined.SystemUpdate),
)

internal fun moduleTitle(section: String): String? =
    (financeItems + accountItems).firstOrNull { it.key == section }?.title
        ?: "Administración".takeIf { section == "admin" }

@Composable
internal fun MainMenu(
    drawerState: DrawerState,
    gesturesEnabled: Boolean,
    currentRoute: String?,
    currentSection: String?,
    userName: String,
    isDark: Boolean,
    isAdmin: Boolean,
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
    onModule: (String) -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    content: @Composable () -> Unit,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * 0.88f).coerceAtMost(360.dp)
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(drawerWidth).fillMaxHeight(), drawerContainerColor = MaterialTheme.colorScheme.surface) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FinFlowV4Wordmark(modifier = Modifier.weight(1f))
                            IconButton(onClick = onClose) { Icon(Icons.Outlined.Close, "Cerrar menú") }
                        }
                        if (userName.isNotBlank()) Text(
                            userName, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        DrawerGroup("Tu panorama", mainItems, currentRoute) { onNavigate(it); onClose() }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        DrawerGroup("Tu dinero", financeItems,
                            if (currentRoute == Screen.Advisor.route) "advisor" else currentSection,
                        ) { onModule(it); onClose() }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        DrawerGroup("Tu cuenta", accountItems, currentSection) { onModule(it); onClose() }
                        if (isAdmin) DrawerGroup("Administración", listOf(
                            MenuItem("admin", "Administración", Icons.Outlined.AdminPanelSettings),
                        ), currentSection) { onModule(it); onClose() }
                        HorizontalDivider(Modifier.padding(vertical = 4.dp))
                        Row(
                            Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Outlined.DarkMode, contentDescription = null)
                            Text("Modo oscuro", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = isDark, onCheckedChange = { onToggleTheme() })
                        }
                        NavigationDrawerItem(
                            selected = false,
                            onClick = { confirmLogout = true },
                            label = { Text("Cerrar sesión") },
                            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null) },
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            },
            content = content,
        )
    }
    if (confirmLogout) AlertDialog(
        onDismissRequest = { confirmLogout = false },
        title = { Text("¿Cerrar sesión?") },
        text = { Text("Podrás volver a entrar con tu correo y contraseña.") },
        confirmButton = { TextButton(onClick = { confirmLogout = false; onClose(); onLogout() }) { Text("Cerrar sesión") } },
        dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") } },
    )
}

@Composable
private fun DrawerGroup(title: String, items: List<MenuItem>, selectedKey: String?, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)) { FinFlowV4Overline(title) }
        items.forEach { item ->
            NavigationDrawerItem(
                selected = selectedKey == item.key,
                onClick = { onSelect(item.key) },
                label = { Text(item.title, fontWeight = if (selectedKey == item.key) FontWeight.Bold else FontWeight.Medium) },
                icon = { Icon(item.icon, contentDescription = null) },
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}
