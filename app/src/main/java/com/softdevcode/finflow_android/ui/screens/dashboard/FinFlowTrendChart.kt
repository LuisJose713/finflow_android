package com.softdevcode.finflow_android.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.softdevcode.finflow_android.domain.model.MonthlyData
import com.softdevcode.finflow_android.domain.model.formatCopMoney
import com.softdevcode.finflow_android.ui.theme.*
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

/** Calendar-correct series for both visualizations, matching FinFlow Web's 6/12-month scope. */
internal data class TrendMonth(val key: String, val label: String, val income: Double, val expenses: Double)
internal data class TrendTotals(val income: Double, val expenses: Double) {
    val balance: Double get() = income - expenses
    val combined: Double get() = income + expenses
}

private val monthKeyPattern = Regex("""\d{4}-(0[1-9]|1[0-2])""")
private fun moneyOrZero(value: Double): Double = value.takeIf { it.isFinite() && it > 0 } ?: 0.0

internal fun buildTrendMonths(
    monthlyData: List<MonthlyData>,
    count: Int,
    currentMonth: YearMonth = YearMonth.now(),
): List<TrendMonth> {
    require(count in 1..12)
    val byMonth = monthlyData.filter { monthKeyPattern.matches(it.month) }.groupBy { it.month }
    return (count - 1 downTo 0).map { offset ->
        val month = currentMonth.minusMonths(offset.toLong())
        val values = byMonth[month.toString()].orEmpty()
        TrendMonth(
            key = month.toString(),
            label = month.month.getDisplayName(TextStyle.SHORT, Locale("es", "CO")).removeSuffix("."),
            income = values.sumOf { moneyOrZero(it.income) },
            expenses = values.sumOf { moneyOrZero(it.expenses) },
        )
    }
}

internal fun trendTotals(months: List<TrendMonth>): TrendTotals =
    TrendTotals(months.sumOf { it.income }, months.sumOf { it.expenses })

internal fun selectTrendIndex(touchX: Float, chartWidth: Float, count: Int, inset: Float = 16f): Int {
    if (count <= 1) return 0
    val fraction = ((touchX - inset) / (chartWidth - 2f * inset).coerceAtLeast(1f)).coerceIn(0f, 1f)
    return (fraction * (count - 1)).toInt().let { index ->
        if (fraction * (count - 1) - index >= .5f) index + 1 else index
    }.coerceIn(0, count - 1)
}

@Composable
fun FinFlowTrendChart(
    monthlyData: List<MonthlyData>,
    amountsVisible: Boolean,
) {
    // Android remains circular by default, as requested.
    var chartStyle by rememberSaveable { mutableStateOf("circle") }
    var period by rememberSaveable { mutableIntStateOf(6) }
    var filter by rememberSaveable { mutableStateOf("all") }
    var highlighted by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedKey by rememberSaveable { mutableStateOf<String?>(null) }

    val data = remember(monthlyData, period) { buildTrendMonths(monthlyData, period) }
    val totals = remember(data) { trendTotals(data) }
    val hasActivity = data.any { it.income > 0 || it.expenses > 0 }

    FinFlowV4Card(modifier = Modifier.padding(horizontal = 20.dp)) {
        FinFlowV4Overline("EVOLUCIÓN FINANCIERA")
        Text("Actividad de tus movimientos", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold)

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6 to "6 meses", 12 to "12 meses").forEach { (value, label) ->
                FilterChip(
                    selected = period == value,
                    onClick = { period = value; selectedKey = null; highlighted = null },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinFlowGold.copy(alpha = .15f)),
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("circle" to "Circular", "line" to "Lineal").forEach { (value, label) ->
                FilterChip(
                    selected = chartStyle == value,
                    onClick = { chartStyle = value },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinFlowGold.copy(alpha = .15f)),
                )
            }
        }

        // The Web filters apply to either representation.
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf("all" to "Ambos", "income" to "Ingresos", "expense" to "Gastos").forEach { (value, label) ->
                FilterChip(
                    selected = filter == value,
                    onClick = { filter = value; highlighted = null },
                    label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = FinFlowGold.copy(alpha = .15f)),
                )
            }
        }

        if (!amountsVisible) {
            Text("Importes ocultos", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (!hasActivity) {
            Text(
                if (monthlyData.any { it.income > 0 || it.expenses > 0 })
                    "No hay actividad en este periodo. Prueba con 12 meses."
                else "Registra un ingreso o gasto para conocer cómo evoluciona tu dinero.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Box(
                Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .27f), RoundedCornerShape(15.dp))
                    .padding(horizontal = 12.dp, vertical = 14.dp)
            ) {
                if (chartStyle == "circle") {
                    FinFlowPeriodDonut(
                        totals = totals,
                        filter = filter,
                        selected = highlighted,
                        onSelect = { highlighted = it },
                    )
                } else {
                    FinFlowPeriodLine(
                        data = data,
                        filter = filter,
                        selectedKey = selectedKey,
                        onSelect = { selectedKey = it },
                    )
                }
            }
            TrendPeriodSummary(totals)
            Text(
                "Importes registrados en los últimos " + period +
                    " meses, incluido el actual. Los meses sin movimientos se muestran en cero.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FinFlowPeriodDonut(
    totals: TrendTotals,
    filter: String,
    selected: String?,
    onSelect: (String) -> Unit,
) {
    val income = if (filter == "expense") 0.0 else totals.income
    val expenses = if (filter == "income") 0.0 else totals.expenses
    val combined = income + expenses
    val incomeShare = if (combined > 0) (income / combined * 100).coerceIn(0.0, 100.0) else 0.0
    val expensesShare = if (combined > 0) (expenses / combined * 100).coerceIn(0.0, 100.0) else 0.0
    val focus = when (filter) {
        "income" -> "income"
        "expense" -> "expense"
        else -> selected
    }
    val displayedValue = when (focus) {
        "income" -> income
        "expense" -> expenses
        else -> combined
    }
    val displayedLabel = when (focus) {
        "income" -> "Ingresos"
        "expense" -> "Gastos"
        else -> "Total del período"
    }
    val percentage = when (focus) {
        "income" -> incomeShare
        "expense" -> expensesShare
        else -> null
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(226.dp), contentAlignment = Alignment.Center) {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val ringWidthPx = with(density) { 28.dp.toPx() }
            val tolerancePx = with(density) { 13.dp.toPx() }
            val trackColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(
                Modifier.matchParentSize()
                    .pointerInput(income, expenses, filter) {
                        detectTapGestures { location ->
                            val cx = size.width / 2f
                            val cy = size.height / 2f
                            val ringRadius = minOf(size.width, size.height).toFloat() / 2f - ringWidthPx / 2f - 5f
                            val distance = hypot(location.x - cx, location.y - cy)
                            if (kotlin.math.abs(distance - ringRadius) <= ringWidthPx / 2f + tolerancePx
                                && combined > 0) {
                                val angle = ((atan2((location.y - cy).toDouble(), (location.x - cx).toDouble()) *
                                    180.0 / PI + 90 + 360) % 360)
                                val segment = if (angle < incomeShare * 3.6) "income" else "expense"
                                if ((segment == "income" && income > 0) || (segment == "expense" && expenses > 0)) {
                                    onSelect(segment)
                                }
                            }
                        }
                    }
                    .semantics {
                        contentDescription = "Gráfico circular. Ingresos " + formatCopMoney(totals.income) +
                            ", gastos " + formatCopMoney(totals.expenses)
                    }
            ) {
                val thickness = 28.dp.toPx()
                val inset = thickness / 2f + 6.dp.toPx()
                val position = Offset(inset, inset)
                val dimension = Size((size.width - 2 * inset).coerceAtLeast(1f),
                    (size.height - 2 * inset).coerceAtLeast(1f))
                drawArc(color = trackColor,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = position, size = dimension, style = Stroke(thickness))
                if (income > 0) {
                    drawArc(
                        color = FinFlowBlue.copy(alpha = if (focus == "expense") .33f else 1f),
                        startAngle = -90f,
                        sweepAngle = (incomeShare * 3.6).toFloat(),
                        useCenter = false, topLeft = position, size = dimension,
                        style = Stroke(thickness),
                    )
                }
                if (expenses > 0) {
                    drawArc(
                        color = FinFlowChartExpense.copy(alpha = if (focus == "income") .33f else 1f),
                        startAngle = -90f + (incomeShare * 3.6).toFloat(),
                        sweepAngle = (expensesShare * 3.6).toFloat(),
                        useCenter = false, topLeft = position, size = dimension,
                        style = Stroke(thickness),
                    )
                }
            }
            Column(
                Modifier.width(150.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(displayedLabel, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(formatCopMoney(displayedValue),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                if (percentage != null) {
                    Text(String.format(Locale("es", "CO"), "%.1f%% del total", percentage),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        listOf(
            Triple("income", "Ingresos", totals.income),
            Triple("expense", "Gastos", totals.expenses)
        ).forEach { (key, label, amount) ->
            val active = focus == key
            val share = if (totals.combined > 0) amount / totals.combined * 100 else 0.0
            Row(
                Modifier.fillMaxWidth()
                    .clickable(enabled = filter == "all") { onSelect(key) }
                    .padding(vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(Modifier.size(11.dp).background(
                    if (key == "income") FinFlowBlue else FinFlowChartExpense,
                    shape = CircleShape,
                ))
                Text(label, Modifier.weight(1f),
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                Text(formatCopMoney(amount), fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(String.format(Locale("es", "CO"), "%.1f%%", share),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("Toca el anillo o un importe para consultar el valor exacto.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun FinFlowPeriodLine(
    data: List<TrendMonth>,
    filter: String,
    selectedKey: String?,
    onSelect: (String) -> Unit,
) {
    val maxAmount = data.maxOf { maxOf(it.income, it.expenses) }.coerceAtLeast(1.0)
    val activeIndex = data.indexOfFirst { it.key == selectedKey }.takeIf { it >= 0 } ?: data.lastIndex
    val active = data[activeIndex]
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val chartInsetPx = with(androidx.compose.ui.platform.LocalDensity.current) { 12.dp.toPx() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Canvas(
            Modifier.fillMaxWidth().height(186.dp)
                .pointerInput(data) {
                    detectTapGestures { offset ->
                        onSelect(data[selectTrendIndex(offset.x, size.width.toFloat(), data.size, chartInsetPx)].key)
                    }
                }
                .pointerInput(data) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { change, _ ->
                            onSelect(data[selectTrendIndex(change.position.x, size.width.toFloat(), data.size, chartInsetPx)].key)
                            change.consume()
                        },
                    )
                }
                .semantics {
                    contentDescription = "Gráfico lineal interactivo. Arrastra horizontalmente para explorar los meses."
                }
        ) {
            val left = 12.dp.toPx()
            val right = size.width - left
            val top = 10.dp.toPx()
            val bottom = size.height - 10.dp.toPx()
            val plotHeight = (bottom - top).coerceAtLeast(1f)
            val plotWidth = (right - left).coerceAtLeast(1f)
            val spacing = if (data.size <= 1) 0f else plotWidth / (data.size - 1)
            fun point(index: Int, amount: Double): Offset = Offset(
                left + spacing * index,
                bottom - (amount / maxAmount).toFloat().coerceIn(0f, 1f) * plotHeight,
            )
            repeat(4) { index ->
                val y = top + index * plotHeight / 3f
                drawLine(gridColor.copy(alpha = .75f), Offset(left, y), Offset(right, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 6.dp.toPx())))
            }
            val selectedX = point(activeIndex, 0.0).x
            drawLine(FinFlowChartExpense.copy(alpha = .65f), Offset(selectedX, top),
                Offset(selectedX, bottom), strokeWidth = 1.3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())))

            fun line(values: (TrendMonth) -> Double, accent: Color, fill: Boolean) {
                val path = Path()
                data.forEachIndexed { index, item ->
                    val point = point(index, values(item))
                    if (index == 0) path.moveTo(point.x, point.y)
                    else path.lineTo(point.x, point.y)
                }
                if (fill) {
                    val shade = Path().apply {
                        addPath(path)
                        lineTo(point(data.lastIndex, 0.0).x, bottom)
                        lineTo(point(0, 0.0).x, bottom)
                        close()
                    }
                    drawPath(shade, brush = Brush.verticalGradient(
                        colors = listOf(accent.copy(alpha = .17f), accent.copy(alpha = 0f)),
                        startY = top, endY = bottom,
                    ))
                }
                drawPath(path, color = accent,
                    style = Stroke(width = 2.7.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                data.forEachIndexed { index, item ->
                    val dot = point(index, values(item))
                    drawCircle(color = gridColor, radius = if (index == activeIndex) 6.dp.toPx() else 4.2.dp.toPx(), center = dot)
                    drawCircle(color = accent,
                        radius = if (index == activeIndex) 4.7.dp.toPx() else 3.dp.toPx(), center = dot)
                }
            }
            if (filter != "expense") line({ it.income }, FinFlowBlue, fill = true)
            if (filter != "income") line({ it.expenses }, FinFlowChartExpense, fill = false)
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            data.forEach { item ->
                TextButton(
                    modifier = Modifier.widthIn(min = 46.dp).heightIn(min = 44.dp),
                    onClick = { onSelect(item.key) },
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (item.key == active.key) FinFlowGoldMuted
                            else MaterialTheme.colorScheme.onSurfaceVariant),
                ) {
                    Text(item.label, fontWeight = if (item.key == active.key) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(active.label.replaceFirstChar { it.uppercase() } + " " + active.key.take(4),
            fontWeight = FontWeight.Bold)
        if (filter != "expense") AmountRow("Ingresos", active.income, FinFlowBlue)
        if (filter != "income") AmountRow("Gastos", active.expenses, FinFlowChartExpense)
        Text("Toca o desliza horizontalmente sobre el gráfico para explorar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TrendPeriodSummary(totals: TrendTotals) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    AmountRow("Ingresos del período", totals.income, FinFlowBlue)
    AmountRow("Gastos del período", totals.expenses, FinFlowChartExpense)
    AmountRow("Balance del período", totals.balance,
        if (totals.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun AmountRow(label: String, amount: Double, color: Color) {
    Row(Modifier.fillMaxWidth().heightIn(min = 38.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(label, modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium)
        Text(formatCopMoney(amount), color = color, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}
