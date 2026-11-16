package com.softdevcode.finflow_android.ui.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.softdevcode.finflow_android.ui.screens.navigation.Screen
import com.softdevcode.finflow_android.ui.theme.*
import com.softdevcode.finflow_android.domain.model.AppTheme

/**
 * Native FinFlow navigation: Web V4 brand tokens and hierarchy, Android touch targets.
 * Only the destinations list scrolls: the account, quick action and controls stay visible.
 */
private data class MenuItem(val key: String, val title: String, val icon: ImageVector)

private val mainItems = listOf(
    MenuItem(Screen.Dashboard.route, "Inicio", Icons.Outlined.GridView),
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
    MenuItem("billing", "Planes y precios", Icons.Outlined.WorkspacePremium),
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
    onNewTransaction: () -> Unit,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onStartTutorial: () -> Unit = {},
    userEmail: String = "",
    themeMode: AppTheme = if (isDark) AppTheme.DARK else AppTheme.LIGHT,
    onSelectTheme: (AppTheme) -> Unit = { onToggleTheme() },
    content: @Composable () -> Unit,
) {
    var confirmLogout by remember { mutableStateOf(false) }
    var accountExpanded by rememberSaveable { mutableStateOf(currentSection in accountItems.map { it.key }) }
    // Never automatically expand a menu group when a route changes: doing so
    // pushes the chosen item and the user's scroll position away.
    val destinationsScroll = rememberScrollState()
    val drawerShape = RoundedCornerShape(topEnd = 22.dp, bottomEnd = 22.dp)
    val density = LocalDensity.current
    val edgeWidth = with(density) { 24.dp.toPx() }
    val edgeHeight = with(density) { 200.dp.toPx() }
    // Android 10+ permits a bounded exclusion area; never exclude the bottom or right edge.
    val edgeGesture = if (gesturesEnabled) Modifier.systemGestureExclusion { coordinates ->
        val height = coordinates.size.height.toFloat()
        val top = ((height - edgeHeight) / 2f).coerceAtLeast(0f)
        Rect(0f, top, edgeWidth.coerceAtMost(coordinates.size.width.toFloat()), (top + edgeHeight).coerceAtMost(height))
    } else Modifier
    BoxWithConstraints(Modifier.fillMaxSize().then(edgeGesture)) {
        val drawerWidth = (maxWidth * 0.90f).coerceAtMost(370.dp)
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = gesturesEnabled,
            scrimColor = Color(0xB3020617),
            drawerContent = {
                ModalDrawerSheet(
                    modifier = Modifier.width(drawerWidth).fillMaxHeight(),
                    drawerShape = drawerShape,
                    drawerContainerColor = MaterialTheme.extras.navigationBackground,
                ) {
                    Column(Modifier.fillMaxSize().background(MaterialTheme.extras.navigationBackground)) {
                        // Brand always stays visible when browsing long navigation menus.
                        Row(
                            Modifier.fillMaxWidth().padding(start = 18.dp, end = 10.dp, top = 16.dp, bottom = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            FinFlowV4BrandMark(Modifier.size(42.dp))
                            FinFlowV4Wordmark(Modifier.weight(1f), color = TextPrimaryDark)
                            IconButton(onClick = onClose, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Outlined.Close, "Cerrar menú", tint = TextSecondDark)
                            }
                        }
                        AccountSummary(userName, userEmail) {
                            accountExpanded = true
                            onModule("security")
                        }
                        Button(
                            onClick = onNewTransaction,
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 11.dp).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = FinFlowGold,
                                contentColor = FinFlowNavy,
                            ),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Nueva transacción", fontWeight = FontWeight.Bold)
                        }
                        HorizontalDivider(color = BorderDark, thickness = 1.dp)

                        // Scrolling only the destinations keeps logout and theme in reach,
                        // even on compact devices and large accessibility text settings.
                        Column(
                            modifier = Modifier.weight(1f).fillMaxWidth()
                                .verticalScroll(destinationsScroll)
                                .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            DrawerGroup("Tu panorama", mainItems, currentRoute, onNavigate)
                            TextButton(onClick = onStartTutorial,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                Icon(Icons.Outlined.HelpOutline, contentDescription = null)
                                Spacer(Modifier.width(9.dp))
                                Text("Guía paso a paso", color = FinFlowGold)
                            }
                            DrawerSeparator()
                            DrawerGroup(
                                "Tu dinero", financeItems,
                                if (currentRoute == Screen.Advisor.route) "advisor" else currentSection,
                                onModule,
                            )
                            DrawerSeparator()
                            CollapsibleDrawerGroup(
                                title = "Tu cuenta",
                                items = accountItems,
                                selectedKey = currentSection,
                                expanded = accountExpanded,
                                onExpand = { accountExpanded = !accountExpanded },
                                onSelect = onModule,
                            )
                            if (isAdmin) {
                                DrawerSeparator()
                                DrawerGroup(
                                    "Administración",
                                    listOf(MenuItem("admin", "Panel de administración", Icons.Outlined.AdminPanelSettings)),
                                    currentSection, onModule,
                                )
                            }
                        }
                        if (destinationsScroll.value < destinationsScroll.maxValue) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Desliza para explorar", color = TextTertiaryDark,
                                    style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = null, tint = TextTertiaryDark,
                                    modifier = Modifier.size(16.dp))
                            }
                        }
                        DrawerFooter(
                            theme = themeMode,
                            onSelectTheme = onSelectTheme,
                            onLogout = { confirmLogout = true },
                        )
                    }
                }
            },
            content = content,
        )
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Podrás volver a entrar con tu correo y contraseña.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    onClose()
                    onLogout()
                }) { Text("Cerrar sesión") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun AccountSummary(userName: String, userEmail: String, onClick: () -> Unit) {
    val cleanName = remember(userName) { userName.trim().ifBlank { "Tu espacio financiero" } }
    val initials = remember(cleanName) {
        cleanName.split(' ').filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
            .ifBlank { "F" }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(15.dp))
            .clickable(onClickLabel = "Editar perfil y seguridad", onClick = onClick),
        color = MaterialTheme.extras.navigationSurface,
        shape = RoundedCornerShape(15.dp),
        border = BorderStroke(1.dp, FinFlowGold.copy(alpha = 0.20f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(43.dp).clip(CircleShape)
                    .background(FinFlowGold.copy(alpha = .14f))
                    .border(1.dp, FinFlowGold.copy(alpha = .45f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initials, color = FinFlowGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(cleanName, color = TextPrimaryDark, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 14.sp)
                Text(userEmail.ifBlank { "Tu espacio financiero" }, color = TextSecondDark,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Editar perfil y seguridad", color = FinFlowGold,
                    style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun DrawerSeparator() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        color = BorderDark.copy(alpha = .7f),
    )
}

@Composable
private fun CollapsibleDrawerGroup(
    title: String,
    items: List<MenuItem>,
    selectedKey: String?,
    expanded: Boolean,
    onExpand: () -> Unit,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClickLabel = if (expanded) "Contraer $title" else "Expandir $title", onClick = onExpand)
            .heightIn(min = 48.dp)
            .padding(start = 15.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title.uppercase(), modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
            letterSpacing = 1.15.sp, color = TextTertiaryDark)
        if (!expanded && selectedKey in items.map { it.key }) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(FinFlowGold))
            Spacer(Modifier.width(8.dp))
        }
        Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "Contraer $title" else "Expandir $title",
            tint = TextSecondDark, modifier = Modifier.size(20.dp))
    }
    AnimatedVisibility(visible = expanded) {
        Column {
            DrawerGroup("", items, selectedKey, onSelect)
        }
    }
}

@Composable
private fun DrawerGroup(
    title: String,
    items: List<MenuItem>,
    selectedKey: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (title.isNotBlank()) Text(
            title.uppercase(),
            modifier = Modifier.padding(start = 15.dp, top = 12.dp, bottom = 7.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.15.sp,
            color = TextTertiaryDark,
        )
        items.forEach { item ->
            val selected = selectedKey == item.key
            val shape = RoundedCornerShape(12.dp)
            val fill by animateColorAsState(
                targetValue = if (selected) MaterialTheme.extras.navigationSelected else Color.Transparent,
                animationSpec = tween(durationMillis = 160),
                label = "drawer-selection",
            )
            val foreground by animateColorAsState(
                targetValue = if (selected) Color.White else TextSecondDark,
                animationSpec = tween(durationMillis = 160),
                label = "drawer-text",
            )
            Row(
                modifier = Modifier.fillMaxWidth()
                    .clip(shape)
                    .background(fill)
                    .border(
                        1.dp, if (selected) FinFlowGold.copy(alpha = .23f) else Color.Transparent,
                        shape,
                    )
                    .selectable(selected = selected, role = Role.Button, onClick = { onSelect(item.key) })
                    .heightIn(min = 49.dp)
                    .padding(start = 12.dp, end = 10.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(width = 3.dp, height = 24.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (selected) FinFlowGold else Color.Transparent),
                )
                Box(
                    modifier = Modifier.size(31.dp).clip(RoundedCornerShape(9.dp))
                        .background(if (selected) FinFlowGold.copy(alpha = .13f) else Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        item.icon, contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (selected) FinFlowGold else TextSecondDark,
                    )
                }
                Text(
                    item.title,
                    modifier = Modifier.weight(1f),
                    color = foreground,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DrawerFooter(
    theme: AppTheme,
    onSelectTheme: (AppTheme) -> Unit,
    onLogout: () -> Unit,
) {
    HorizontalDivider(color = BorderDark)
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 9.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CurrencyPicker()
        AppearancePicker(theme, onSelectTheme)
        OutlinedButton(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BorderDark),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondDark),
        ) {
            Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null,
                modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text("Cerrar sesión", fontWeight = FontWeight.Medium)
        }
        Text(
            "Creado por AEVQOR",
            modifier = Modifier.fillMaxWidth(),
            color = FinFlowGold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}


@Composable
private fun CurrencyPicker() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current by DisplayCurrencyManager.quote.collectAsStateWithLifecycle()
    val busy by DisplayCurrencyManager.busy.collectAsStateWithLifecycle()
    val issue by DisplayCurrencyManager.error.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp),
            enabled = !busy,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimaryDark),
            border = BorderStroke(1.dp, BorderDark),
        ) {
            Icon(Icons.Outlined.CurrencyExchange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Moneda visual · " + current.code, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DisplayCurrencyManager.supported.forEach { code ->
                DropdownMenuItem(text = { Text(code) }, onClick = {
                    expanded = false
                    scope.launch { DisplayCurrencyManager.select(context, code) }
                })
            }
        }
    }
    if (busy) Text("Consultando tasa…", color = TextSecondDark, style = MaterialTheme.typography.labelSmall)
    if (current.code != "COP") {
        Text("Tasa de referencia " + (current.date ?: "") + " · Registros originales COP",
            color = TextSecondDark, style = MaterialTheme.typography.labelSmall)
        Text("Fuente: Frankfurter", color = TextSecondDark, style = MaterialTheme.typography.labelSmall)
    }
    if (issue != null) Text(issue ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
}
