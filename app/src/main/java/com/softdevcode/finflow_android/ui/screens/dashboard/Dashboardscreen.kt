    package com.softdevcode.finflow_android.ui.screens.dashboard
    import androidx.lifecycle.Lifecycle
    import androidx.lifecycle.LifecycleEventObserver
    import androidx.lifecycle.compose.LocalLifecycleOwner

    import androidx.compose.animation.*
    import androidx.compose.foundation.*
    import androidx.compose.foundation.layout.*
    import androidx.compose.foundation.shape.CircleShape
    import androidx.compose.foundation.shape.RoundedCornerShape
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.outlined.*
    import androidx.compose.material3.*
    import androidx.compose.material3.pulltorefresh.*
    import androidx.compose.runtime.*
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.draw.clip
    import androidx.compose.ui.graphics.Brush
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.vector.ImageVector
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextOverflow
    import androidx.compose.ui.unit.*
    import androidx.hilt.navigation.compose.hiltViewModel
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import com.softdevcode.finflow_android.domain.model.*
    import com.softdevcode.finflow_android.ui.theme.*
    import java.text.NumberFormat
    import java.util.Locale

    // ── DashboardScreen ───────────────────────────────────────────────────────────
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun DashboardScreen(
        userName: String,
        isDark: Boolean,
        onToggleTheme: () -> Unit,
        onLogout: () -> Unit,
        viewModel: DashboardViewModel = hiltViewModel(),
    ) {
        val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
        val pullRefreshState = rememberPullToRefreshState()

        val lifecycleOwner = LocalLifecycleOwner.current

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->

                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refresh()
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }


        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Top bar ───────────────────────────────────────────────────
                DashboardTopBar(
                    userName     = userName,
                    isDark       = isDark,
                    onToggleTheme = onToggleTheme,
                    onLogout     = onLogout,
                )

                // ── Hero balance ──────────────────────────────────────────────
                uiState.stats?.let { stats ->
                    HeroBalance(stats = stats)
                }

                // ── Mini stats row ────────────────────────────────────────────
                uiState.stats?.let { stats ->
                    MiniStatsRow(stats = stats)
                }

                Spacer(Modifier.height(20.dp))

                // ── Loading ───────────────────────────────────────────────────
                if (uiState.isLoading) {
                    DashboardShimmer()
                }

                // ── Error ─────────────────────────────────────────────────────
                uiState.error?.let { error ->
                    Spacer(Modifier.height(8.dp))
                    ErrorCard(
                        message = error,
                        onRetry = { viewModel.load() },
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                // ── Donut chart summary ───────────────────────────────────────
                if (!uiState.isLoading && uiState.stats != null) {
                    Text(
                        "Tu actividad",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(start = 22.dp, bottom = 12.dp),
                    )
                    SummaryCard(
                        stats   = uiState.stats!!,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Recent transactions ───────────────────────────────────────
                if (!uiState.isLoading && uiState.recentTransactions.isNotEmpty()) {
                    RecentTransactionsCard(
                        transactions = uiState.recentTransactions,
                        categoryName = { viewModel.categoryName(it) },
                        modifier     = Modifier.padding(horizontal = 20.dp),
                    )
                }

                Spacer(Modifier.height(128.dp))
            }


        }
    }

    // ── FinFlow identity, greeting and real account actions ────────────────
    @Composable
    private fun DashboardTopBar(
        userName: String,
        isDark: Boolean,
        onToggleTheme: () -> Unit,
        onLogout: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Primary, PrimaryDark))),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "F",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 9.dp, end = 9.dp)
                            .size(6.dp)
                            .background(Warning, CircleShape),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "FinFlow",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.7).sp,
                        ),
                    )
                    Text(
                        "Tu espacio financiero",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                        contentDescription = "Cambiar tema",
                        tint = if (isDark) Warning else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onLogout) {
                    Icon(
                        imageVector = Icons.Outlined.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Hola, ${userName.trim().split(" ").firstOrNull()?.ifEmpty { "Usuario" } ?: "Usuario"} 👋",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.7).sp,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Así se mueve tu dinero hoy.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    // ── Prominent live balance, sourced from the existing dashboard ViewModel ──
    @Composable
    private fun HeroBalance(stats: TransactionStats) {
        val savingsPositive = stats.savingsRate >= 0
        val savingsTint = if (savingsPositive) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF0A244E), Color(0xFF194B9A), PrimaryDark)
                    )
                )
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "BALANCE TOTAL",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp,
                    ),
                    color = Color.White.copy(alpha = 0.74f),
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.84f),
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                formatCurrency(stats.balance),
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold, letterSpacing = (-1.3).sp,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (savingsPositive)
                            Icons.Outlined.TrendingUp else Icons.Outlined.TrendingDown,
                        contentDescription = null,
                        tint = savingsTint,
                        modifier = Modifier.size(17.dp),
                    )
                }
                Spacer(Modifier.width(9.dp))
                Text(
                    "${String.format(Locale("es", "CO"), "%.1f", Math.abs(stats.savingsRate))}% de ahorro",
                    color = savingsTint,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Resumen general",
                    color = Color.White.copy(alpha = 0.72f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }

    // ── Mini Stats Row ────────────────────────────────────────────────────────────
    @Composable
    private fun MiniStatsRow(stats: TransactionStats) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MiniStatCard(
                label  = "Ingresos",
                value  = formatCompact(stats.totalIncome),
                icon   = Icons.Outlined.ArrowUpward,
                color  = Success,
                modifier = Modifier.weight(1f),
            )
            MiniStatCard(
                label  = "Gastos",
                value  = formatCompact(stats.totalExpenses),
                icon   = Icons.Outlined.ArrowDownward,
                color  = Danger,
                modifier = Modifier.weight(1f),
            )
            MiniStatCard(
                label  = "Ahorro",
                value  = "${String.format("%.1f", stats.savingsRate)}%",
                icon   = Icons.Outlined.BarChart,
                color  = Primary,
                modifier = Modifier.weight(1f),
            )
        }
    }

    @Composable
    private fun MiniStatCard(
        label: String,
        value: String,
        icon: ImageVector,
        color: Color,
        modifier: Modifier = Modifier,
    ) {
        val isDark = MaterialTheme.colorScheme.background == BackgroundDark
        Card(
            modifier = modifier,
            shape    = RoundedCornerShape(20.dp),
            colors   = CardDefaults.cardColors(
                containerColor = if (isDark) SurfaceDark else Color.White,
            ),
            border   = androidx.compose.foundation.BorderStroke(
                0.5.dp, if (isDark) BorderDark else BorderLight,
            ),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(color.copy(alpha = 0.12f), RoundedCornerShape(7.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.3.sp,
                        fontWeight    = FontWeight.SemiBold,
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text  = value,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = color,
                )
            }
        }
    }

    // ── Summary Card (donut) ──────────────────────────────────────────────────────
    @Composable
    private fun SummaryCard(
        stats: TransactionStats,
        modifier: Modifier = Modifier,
    ) {
        val isDark = MaterialTheme.colorScheme.background == BackgroundDark

        Card(
            modifier  = modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (isDark) SurfaceDark else Color.White,
            ),
            border    = androidx.compose.foundation.BorderStroke(
                0.5.dp, if (isDark) BorderDark else BorderLight,
            ),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Resumen",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                )

                Spacer(Modifier.height(16.dp))

                // Totals
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TotalBox(
                        label      = "TOTAL INGRESOS",
                        value      = "+${formatCompact(stats.totalIncome)}",
                        color      = Success,
                        bgColor    = if (isDark) Color(0xFF0C1A0F) else Color(0xFFF4FDF7),
                        modifier   = Modifier.weight(1f),
                    )
                    TotalBox(
                        label      = "TOTAL EGRESOS",
                        value      = "-${formatCompact(stats.totalExpenses)}",
                        color      = Danger,
                        bgColor    = if (isDark) Color(0xFF1A0C0C) else Color(0xFFFEF2F2),
                        modifier   = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Simple bar chart (ingresos vs egresos)
                SimpleBarChart(
                    income   = stats.totalIncome,
                    expenses = stats.totalExpenses,
                )

                Spacer(Modifier.height(16.dp))

                // Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LegendDot(color = Success, label = "Ingresos")
                    Spacer(Modifier.width(24.dp))
                    LegendDot(color = Danger,  label = "Egresos")
                }
            }
        }
    }

    @Composable
    private fun TotalBox(
        label: String,
        value: String,
        color: Color,
        bgColor: Color,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier
                .background(bgColor, RoundedCornerShape(10.dp))
                .border(
                    width = 2.dp,
                    color = color,
                    shape = RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                )
                .padding(start = 12.dp, top = 10.dp, end = 10.dp, bottom = 10.dp)
        ) {
            Column {
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight    = FontWeight.SemiBold,
                    ),
                    color = color,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text  = value,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight    = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                    ),
                    color = color,
                )
            }
        }
    }

    @Composable
    private fun SimpleBarChart(income: Double, expenses: Double) {
        val total  = income + expenses
        if (total == 0.0) return

        val incomeRatio   = (income   / total).toFloat()
        val expenseRatio  = (expenses / total).toFloat()

        Column(modifier = Modifier.fillMaxWidth()) {
            // Income bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(incomeRatio.coerceAtLeast(0.02f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Success, SuccessLight))
                        )
                )
                if (expenseRatio > 0.02f) Spacer(Modifier.width(2.dp))
            }

            Spacer(Modifier.height(6.dp))

            // Expense bar
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .weight(expenseRatio.coerceAtLeast(0.02f))
                        .height(10.dp)
                        .clip(RoundedCornerShape(topStart = 5.dp, bottomStart = 5.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Danger, DangerLight))
                        )
                )
            }
        }
    }

    @Composable
    private fun LegendDot(color: Color, label: String) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color, CircleShape)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface.copy(0.6f),
            )
        }
    }

    // ── Recent Transactions ───────────────────────────────────────────────────────
    @Composable
    private fun RecentTransactionsCard(
        transactions: List<Transaction>,
        categoryName: (Int?) -> String,
        modifier: Modifier = Modifier,
    ) {
        val isDark = MaterialTheme.colorScheme.background == BackgroundDark

        Card(
            modifier  = modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (isDark) SurfaceDark else Color.White,
            ),
            border    = androidx.compose.foundation.BorderStroke(
                0.5.dp, if (isDark) BorderDark else BorderLight,
            ),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Transacciones recientes",
                        style    = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f),
                    )
                    Surface(
                        color = Primary.copy(alpha = 0.1f),
                        shape = CircleShape,
                    ) {
                        Text(
                            "Últimas 5",
                            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color    = Primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                transactions.forEachIndexed { index, tx ->
                    TxItem(
                        tx           = tx,
                        categoryName = categoryName(tx.categoryId),
                    )
                    if (index < transactions.lastIndex) {
                        HorizontalDivider(
                            color    = if (isDark) BorderDark else BorderLight,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun TxItem(tx: Transaction, categoryName: String) {
        val isIncome = tx.type == "income"
        val color    = if (isIncome) Success else Danger

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint     = color,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text  = tx.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text  = "$categoryName · ${formatDate(tx.date ?: tx.createdAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(0.4f),
                )
            }

            Text(
                text  = "${if (isIncome) "+" else "-"}${formatCompact(tx.amount)}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight    = FontWeight.Bold,
                    letterSpacing = (-0.3).sp,
                ),
                color = color,
            )
        }
    }

    // ── Shimmer ───────────────────────────────────────────────────────────────────
    @Composable
    private fun DashboardShimmer() {
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(0.07f),
                            RoundedCornerShape(14.dp),
                        )
                )
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ── Error Card ────────────────────────────────────────────────────────────────
    @Composable
    private fun ErrorCard(
        message: String,
        onRetry: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Surface(
            color    = MaterialTheme.colorScheme.errorContainer,
            shape    = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(message, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) {
                    Text("Reintentar", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }

    // ── Formatters ────────────────────────────────────────────────────────────────
    private fun formatCurrency(amount: Double): String {
        val abs = Math.abs(amount)
        val fmt = NumberFormat.getNumberInstance(Locale("es", "CO"))
        val str = "\$${fmt.format(abs)}"
        return if (amount < 0) "-$str" else str
    }

    private fun formatCompact(amount: Double): String {
        val abs = Math.abs(amount)
        return when {
            abs >= 1_000_000 -> "\$${String.format("%.1f", abs / 1_000_000)}M"
            abs >= 1_000     -> "\$${String.format("%.0f", abs / 1_000)}K"
            else             -> "\$${String.format("%.0f", abs)}"
        }
    }

    private fun formatDate(dateStr: String?): String {
        if (dateStr == null) return ""
        return try {
            val parts = dateStr.split("T")[0].split("-")
            val months = listOf("", "Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic")
            "${parts[2].toInt()} ${months[parts[1].toInt()]}"
        } catch (_: Exception) { dateStr }
    }