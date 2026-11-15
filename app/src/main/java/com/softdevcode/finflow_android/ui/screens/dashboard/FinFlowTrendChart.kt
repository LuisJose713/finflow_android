package com.softdevcode.finflow_android.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.softdevcode.finflow_android.domain.model.MonthlyData
import com.softdevcode.finflow_android.ui.theme.*
import java.text.NumberFormat
import java.util.Locale

/** Responsive, real-data equivalent of Web's "EVOLUCIÓN FINANCIERA" card. */
@Composable
fun FinFlowTrendChart(
    monthlyData: List<MonthlyData>,
    amountsVisible: Boolean,
    totalIncome: Double,
    totalExpenses: Double,
) {
    var chartStyle by rememberSaveable { mutableStateOf("circle") }
    var highlightedSlice by rememberSaveable { mutableStateOf("income") }
    var period by rememberSaveable { mutableIntStateOf(6) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val data = monthlyData.takeLast(period)
    val selection = data.getOrNull(selected.coerceIn(0, (data.size - 1).coerceAtLeast(0)))
    FinFlowV4Card(modifier = Modifier.padding(horizontal = 20.dp)) {
        FinFlowV4Overline("EVOLUCIÓN FINANCIERA")
        Text(
            "Actividad de tus movimientos",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("circle" to "Circular", "line" to "Lineal").forEach { (value, label) ->
                FilterChip(
                    selected = chartStyle == value,
                    onClick = { chartStyle = value },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinFlowGold.copy(alpha = .15f),
                    ),
                )
            }
        }
        if (chartStyle == "circle") {
            if (!amountsVisible) {
                Text("Importes ocultos", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                FinFlowDonutChart(
                    income = totalIncome,
                    expenses = totalExpenses,
                    highlighted = highlightedSlice,
                    onHighlight = { highlightedSlice = it },
                )
            }
        } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(6 to "6 meses", 12 to "12 meses").forEach { (months, label) ->
                FilterChip(
                    selected = period == months,
                    onClick = { period = months; selected = -1 },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinFlowGold.copy(alpha = .15f),
                    )
                )
            }
        }
        if (!amountsVisible) {
            Text("Importes ocultos",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (data.isEmpty()) {
            Text("Tu actividad aparecerá aquí cuando registres movimientos.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val maxValue = data.maxOf { maxOf(it.income, it.expenses) }
                .coerceAtLeast(1.0)
            val chartGridColor = MaterialTheme.colorScheme.outline
            Canvas(Modifier.fillMaxWidth().height(158.dp)) {
                val w = size.width
                val h = size.height
                val inset = 12.dp.toPx()
                val drawableWidth = (w - inset * 2).coerceAtLeast(1f)
                val drawableHeight = (h - 18.dp.toPx()).coerceAtLeast(1f)
                val widthStep = if (data.size == 1) 0f
                    else drawableWidth / (data.size - 1)
                fun point(index: Int, value: Double): Offset {
                    val x = inset + (index * widthStep)
                    val y = drawableHeight * (1f - (value / maxValue).toFloat().coerceIn(0f, 1f))
                    return Offset(x, y)
                }
                repeat(4) { line ->
                    val y = drawableHeight * line / 3f
                    drawLine(
                        color = chartGridColor.copy(alpha = .65f),
                        start = Offset(inset, y),
                        end = Offset(w - inset, y),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
                fun renderLine(values: (MonthlyData) -> Double,
                               color: androidx.compose.ui.graphics.Color) {
                    val path = Path()
                    data.forEachIndexed { index, month ->
                        val point = point(index, values(month))
                        if (index == 0) path.moveTo(point.x, point.y)
                        else path.lineTo(point.x, point.y)
                    }
                    drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
                    data.forEachIndexed { index, month ->
                        drawCircle(
                            color = color,
                            radius = if (index == selected) 4.6.dp.toPx() else 3.dp.toPx(),
                            center = point(index, values(month)),
                        )
                    }
                }
                renderLine({ it.income }, FinFlowBlue)
                renderLine({ it.expenses }, FinFlowChartExpense)
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                data.forEachIndexed { index, point ->
                    TextButton(
                        modifier = Modifier.widthIn(min = 42.dp),
                        onClick = { selected = index },
                        contentPadding = PaddingValues(0.dp),
                    ) {
                        Text(
                            point.month.take(3),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected == index) FinFlowGold
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            val active = if (selected >= 0) selection else data.lastOrNull()
            if (active != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Text(active.month, fontWeight = FontWeight.Bold)
                val formatter = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Ingresos", color = FinFlowBlue)
                    Text(formatter.format(active.income), fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gastos", color = FinFlowGoldMuted)
                    Text(formatter.format(active.expenses), fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Importes de tus movimientos reales, sin cifras de ejemplo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

@Composable
private fun FinFlowDonutChart(
    income: Double,
    expenses: Double,
    highlighted: String,
    onHighlight: (String) -> Unit,
) {
    val positiveIncome = income.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val positiveExpenses = expenses.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    val total = positiveIncome + positiveExpenses
    if (total <= 0.0 || !total.isFinite()) {
        Text(
            "Registra un ingreso o gasto para ver la distribución circular.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val isIncome = highlighted == "income"
    val displayAmount = if (isIncome) positiveIncome else positiveExpenses
    val accent = if (isIncome) FinFlowBlue else FinFlowChartExpense
    val formatted = remember(displayAmount) {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).format(displayAmount)
    }
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(212.dp)) {
            val thickness = 30.dp.toPx()
            val outerInset = thickness / 2f + 5.dp.toPx()
            val chartSize = Size(
                (size.width - 2 * outerInset).coerceAtLeast(1f),
                (size.height - 2 * outerInset).coerceAtLeast(1f)
            )
            val arcPosition = Offset(outerInset, outerInset)
            val incomeSweep = (positiveIncome / total * 360.0).toFloat()
            drawArc(
                color = FinFlowBlue, startAngle = -90f,
                sweepAngle = incomeSweep, useCenter = false,
                topLeft = arcPosition, size = chartSize,
                style = Stroke(thickness)
            )
            drawArc(
                color = FinFlowChartExpense, startAngle = -90f + incomeSweep,
                sweepAngle = 360f - incomeSweep, useCenter = false,
                topLeft = arcPosition, size = chartSize,
                style = Stroke(thickness)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (isIncome) "Ingresos" else "Gastos",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatted,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accent,
            )
        }
    }
    val formatter = remember { NumberFormat.getCurrencyInstance(Locale("es", "CO")) }
    listOf(
        Triple("income", "Ingresos", positiveIncome),
        Triple("expense", "Gastos", positiveExpenses),
    ).forEach { (key, label, amount) ->
        val selected = highlighted == key
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable { onHighlight(key) }
                .heightIn(min = 48.dp)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                Modifier.size(11.dp).background(
                    if (key == "income") FinFlowBlue else FinFlowChartExpense,
                    shape = CircleShape,
                )
            )
            Text(label, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
            Text(
                formatter.format(amount),
                fontWeight = FontWeight.SemiBold,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
    Text(
        "Distribución de ingresos y gastos registrados · COP. Toca una categoría para ver su valor.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
