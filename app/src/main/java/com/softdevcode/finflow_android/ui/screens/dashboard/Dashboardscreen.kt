package com.softdevcode.finflow_android.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.softdevcode.finflow_android.R
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.ui.theme.*
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userName: String,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLogout: () -> Unit,
    onAddTransaction: () -> Unit,
    onViewTransactions: () -> Unit,
    onViewPlannedExpenses: () -> Unit,
    onViewGoals: () -> Unit,
    onViewAccounts: () -> Unit,
    onViewAdvisor: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var amountsVisible by rememberSaveable { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullToRefreshBox(
        isRefreshing = uiState.isLoading,
        onRefresh = viewModel::refresh,
        state = rememberPullToRefreshState(),
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DashboardTopBar(userName, isDark, onToggleTheme, onLogout)
            uiState.stats?.let { stats ->
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FinFlowV4Overline("TU DINERO EN PERSPECTIVA")
                    Text("Panorama general", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold)
                }
                HeroBalance(stats, amountsVisible) { amountsVisible = !amountsVisible }
                MiniStatsRow(stats, amountsVisible)
                FinFlowQuickLinks(onViewAccounts, onViewGoals, onViewAdvisor)
            }
            uiState.lastUpdatedAt?.let {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (uiState.error != null) "Mostrando la última consulta" else
                            "Actualizado a las ${DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(it))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) {
                        Text(if (uiState.isLoading) "Actualizando…" else "Actualizar")
                    }
                }
            }
            if (uiState.isLoading && uiState.stats == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
                Text("Consultando tus movimientos…", modifier = Modifier.padding(horizontal = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            uiState.error?.let { message ->
                DashboardCard {
                    Icon(Icons.Outlined.CloudOff, null, tint = MaterialTheme.colorScheme.error)
                    Text("No pudimos actualizar todos tus datos", fontWeight = FontWeight.Bold)
                    Text(message, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = viewModel::refresh, enabled = !uiState.isLoading) { Text("Reintentar") }
                }
            }
            uiState.stats?.let { stats ->
                DashboardCard {
                    Text("PLAN FINANCIERO", style = MaterialTheme.typography.labelMedium,
                        color = FinFlowGold, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Text(if (amountsVisible && stats.balance < 0) "Recupera margen para lo que viene" else "Tu dinero, con un plan",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        if (amountsVisible && stats.balance < 0)
                            "Tus gastos registrados superan tus ingresos. Revisa los movimientos y organiza tus próximos pagos."
                        else "Revisa tus próximos pagos y avanza en tus metas antes de decidir cuánto gastar.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = onViewPlannedExpenses, modifier = Modifier.weight(1f)) { Text("Ver pagos") }
                        OutlinedButton(onClick = onViewGoals, modifier = Modifier.weight(1f)) { Text("Ver metas") }
                    }
                }
                ActivitySummary(stats, amountsVisible)
                FinFlowTrendChart(stats.monthlyData, amountsVisible)
            }
            if (uiState.hasLoadedTransactions) {
                RecentTransactionsCard(uiState.recentTransactions, viewModel::categoryName,
                    amountsVisible, onAddTransaction, onViewTransactions)
            }
            Spacer(Modifier.height(112.dp))
        }
    }
}

@Composable
private fun DashboardTopBar(userName: String, isDark: Boolean, onToggleTheme: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FinFlowV4BrandMark(Modifier.size(49.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                FinFlowV4Wordmark()
                FinFlowV4Overline("ESPACIO PERSONAL")
            }
            IconButton(onClick = onToggleTheme) {
                Icon(if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    if (isDark) "Activar tema claro" else "Activar tema oscuro")
            }
            IconButton(onClick = onLogout) {
                Icon(Icons.Outlined.Logout, "Cerrar sesión")
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF34A38C)))
            Spacer(Modifier.width(9.dp))
            Text("FINFLOW / ESPACIO PERSONAL", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold, letterSpacing = 1.3.sp)
        }
        Spacer(Modifier.height(24.dp))
        FinFlowV4Overline("TU PANORAMA FINANCIERO")
        Spacer(Modifier.height(6.dp))
        Text("Tu dinero,", style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp))
        Text("con claridad.", style = MaterialTheme.typography.headlineLarge.copy(
            fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.2).sp,
            brush = Brush.linearGradient(listOf(Color(0xFF376FB8), Color(0xFF7867CD))),
        ))
        Spacer(Modifier.height(8.dp))
        Text(
            "Hola, ${userName.trim().substringBefore(' ').ifBlank { "Usuario" }}. Así se mueve tu dinero.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FinFlowQuickLinks(
    onAccounts: () -> Unit,
    onGoals: () -> Unit,
    onAdvisor: () -> Unit,
) {
    data class QuickLink(
        val title: String,
        val subtitle: String,
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val onClick: () -> Unit,
    )
    val links = listOf(
        QuickLink("Mis cuentas", "Revisa tus saldos", Icons.Outlined.AccountBalanceWallet, onAccounts),
        QuickLink("Mis metas", "Avanza a tu ritmo", Icons.Outlined.TrackChanges, onGoals),
        QuickLink("Asesor FinFlow", "Explora nuevas ideas", Icons.Outlined.AutoAwesome, onAdvisor),
    )
    // Same rhythm as the mobile drawer: one surface, accessible rows and
    // understated separators instead of three bulky detached cards.
    FinFlowV4Card(modifier = Modifier.padding(horizontal = 20.dp)) {
        links.forEachIndexed { index, link ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clickable(onClick = link.onClick)
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(FinFlowGold.copy(alpha = .13f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(link.icon, null, tint = FinFlowGoldMuted, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(link.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(link.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall)
                }
                Icon(Icons.Outlined.NorthEast, contentDescription = "Abrir " + link.title,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            if (index < links.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun HeroBalance(stats: TransactionStats, visible: Boolean, onToggleVisibility: () -> Unit) {
    var showCalculation by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(26.dp))
        .background(Brush.linearGradient(listOf(FinFlowNavy, FinFlowNavy2, Color(0xFF22394D)))).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TU BALANCE  /  FINFLOW", Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                color = FinFlowGoldSoft, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            IconButton(onClick = onToggleVisibility) {
                Icon(if (visible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    if (visible) "Ocultar importes del inicio" else "Mostrar importes del inicio", tint = Color.White)
            }
        }
        PrivateAmount(formatCurrency(stats.balance), visible, Modifier.fillMaxWidth(), color = Color.White, large = true)
        Text("Según tus movimientos registrados. No es tu saldo bancario disponible.",
            style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .85f))
        HorizontalDivider(color = Color.White.copy(alpha = .2f))
        Text(
            when {
                !visible -> "Margen de ahorro oculto"
                stats.totalIncome <= 0 -> "Registra ingresos para calcular tu margen"
                else -> "${formatPercent(stats.savingsRate)} de margen de ahorro"
            },
            style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
            color = if (stats.savingsRate < 0 && visible) Color(0xFFFECACA) else FinFlowGoldSoft)
        TextButton(onClick = { showCalculation = !showCalculation },
            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
            Text(if (showCalculation) "Ocultar cálculo" else "Ver cómo se calcula")
            Icon(if (showCalculation) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null)
        }
        if (showCalculation) {
            Text("Balance = ingresos registrados − gastos registrados. Las metas y los pagos planeados se consultan por separado.",
                style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = .9f))
        }
    }
}

@Composable
private fun MiniStatsRow(stats: TransactionStats, visible: Boolean) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        if (maxWidth < 350.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Ingresos", stats.totalIncome, visible, MaterialTheme.extras.success, Modifier.fillMaxWidth())
                StatCard("Gastos", stats.totalExpenses, visible, MaterialTheme.extras.danger, Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Ingresos", stats.totalIncome, visible, MaterialTheme.extras.success, Modifier.weight(1f))
                StatCard("Gastos", stats.totalExpenses, visible, MaterialTheme.extras.danger, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, amount: Double, visible: Boolean, tint: Color, modifier: Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PrivateAmount(formatCurrency(amount), visible, color = tint)
        }
    }
}

@Composable
private fun ActivitySummary(stats: TransactionStats, visible: Boolean) {
    DashboardCard {
        Text("Tu actividad registrada", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (!visible) {
            Text("Importes ocultos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (stats.totalIncome <= 0 && stats.totalExpenses <= 0) {
            Text("Tu resumen aparecerá al registrar movimientos.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            ActivityBar("Ingresos", activityShare(stats.totalIncome, stats.totalIncome, stats.totalExpenses), MaterialTheme.extras.success)
            ActivityBar("Gastos", activityShare(stats.totalExpenses, stats.totalIncome, stats.totalExpenses), MaterialTheme.extras.danger)
            Text("Proporción del total de movimientos registrados.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityBar(label: String, share: Float, color: Color) {
    Column(Modifier.clearAndSetSemantics { contentDescription = "$label: ${formatPercent(share * 100.0)} del total" }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(formatPercent(share * 100.0), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(color.copy(alpha = .12f))) {
            if (share > 0) Box(Modifier.fillMaxWidth(share).fillMaxHeight().clip(CircleShape).background(color))
        }
    }
}

@Composable
private fun RecentTransactionsCard(
    transactions: List<Transaction>, categoryName: (Int?) -> String, visible: Boolean,
    onAddTransaction: () -> Unit, onViewTransactions: () -> Unit,
) {
    DashboardCard {
        Text("Movimientos recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (transactions.isEmpty()) {
            Icon(Icons.Outlined.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary)
            Text("Tu primer paso empieza aquí", style = MaterialTheme.typography.titleMedium)
            Text("Registra un ingreso o gasto para empezar a entender cómo se mueve tu dinero.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onAddTransaction) { Text("Añadir movimiento") }
        } else {
            transactions.forEachIndexed { index, tx ->
                TransactionItem(tx, categoryName(tx.categoryId), visible)
                if (index < transactions.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            TextButton(onClick = onViewTransactions, modifier = Modifier.fillMaxWidth()) {
                Text("Ver todos los movimientos")
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun TransactionItem(tx: Transaction, categoryName: String, visible: Boolean) {
    val isIncome = tx.type == "income"
    val tint = if (isIncome) MaterialTheme.extras.success else MaterialTheme.extras.danger
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                if (isIncome) "Ingreso" else "Gasto", tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(tx.description.ifBlank { if (isIncome) "Ingreso" else "Gasto" }, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("$categoryName · ${formatDate(tx.date ?: tx.createdAt)}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        PrivateAmount("${if (isIncome) "+" else "−"}${formatCurrency(tx.amount)}", visible, color = tint)
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    FinFlowV4Card(modifier = Modifier.padding(horizontal = 20.dp), content = content)
}

@Composable
private fun PrivateAmount(text: String, visible: Boolean, modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface, large: Boolean = false) {
    Text(text = if (visible) text else "••••••",
        modifier = if (visible) modifier else modifier.clearAndSetSemantics { contentDescription = "Importe oculto" },
        style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold, color = color)
}

private fun formatCurrency(amount: Double): String = com.softdevcode.finflow_android.domain.model.formatCopMoney(amount)
private fun formatPercent(amount: Double): String = String.format(Locale("es", "CO"), "%.1f%%", amount)

private fun formatDate(value: String?): String {
    if (value.isNullOrBlank()) return "Sin fecha"
    val parts = value.substringBefore("T").split("-")
    val day = parts.getOrNull(2)?.toIntOrNull() ?: return value
    val month = parts.getOrNull(1)?.toIntOrNull() ?: return value
    val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    return months.getOrNull(month - 1)?.let { "$day $it" } ?: value
}

