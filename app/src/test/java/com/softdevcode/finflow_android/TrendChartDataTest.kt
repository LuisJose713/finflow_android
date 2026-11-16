package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.domain.model.MonthlyData
import com.softdevcode.finflow_android.ui.screens.dashboard.buildTrendMonths
import com.softdevcode.finflow_android.ui.screens.dashboard.selectTrendIndex
import com.softdevcode.finflow_android.ui.screens.dashboard.trendTotals
import java.time.YearMonth
import org.junit.Assert.*
import org.junit.Test

class TrendChartDataTest {
    @Test fun padsCalendarMonthsWithZeroAndPreservesChronology() {
        val months = buildTrendMonths(
            listOf(MonthlyData("2026-11", 250.0, 80.0), MonthlyData("2026-08", 40.0, 20.0)),
            6, YearMonth.of(2026, 11))
        assertEquals(listOf("2026-06", "2026-07", "2026-08", "2026-09", "2026-10", "2026-11"),
            months.map { it.key })
        assertEquals(0.0, months[0].income, 0.0)
        assertEquals(40.0, months[2].income, 0.0)
        assertEquals(250.0, months[5].income, 0.0)
    }

    @Test fun circularAndLinearTotalsFollowTheSameSelectedPeriod() {
        val source = listOf(
            MonthlyData("2025-12", 100.0, 20.0),
            MonthlyData("2026-03", 70.0, 10.0),
            MonthlyData("2026-10", 300.0, 90.0))
        val six = trendTotals(buildTrendMonths(source, 6, YearMonth.of(2026, 11)))
        val year = trendTotals(buildTrendMonths(source, 12, YearMonth.of(2026, 11)))
        assertEquals(300.0, six.income, 0.0)
        assertEquals(90.0, six.expenses, 0.0)
        assertEquals(210.0, six.balance, 0.0)
        assertEquals(470.0, year.income, 0.0)
        assertEquals(120.0, year.expenses, 0.0)
    }

    @Test fun spansYearBoundaryAndSanitizesInvalidValues() {
        val result = buildTrendMonths(
            listOf(MonthlyData("2026-01", Double.NaN, Double.POSITIVE_INFINITY),
                MonthlyData("2025-12", 25.0, -1.0),
                MonthlyData("bad-value", 999.0, 999.0)),
            3, YearMonth.of(2026, 1))
        assertEquals(listOf("2025-11", "2025-12", "2026-01"), result.map { it.key })
        assertEquals(25.0, trendTotals(result).income, 0.0)
        assertEquals(0.0, trendTotals(result).expenses, 0.0)
    }

    @Test fun selectionClampsAtEdgesAndMiddle() {
        assertEquals(0, selectTrendIndex(-50f, 332f, 6))
        assertEquals(5, selectTrendIndex(1000f, 332f, 6))
        assertEquals(2, selectTrendIndex(135f, 332f, 6))
        assertEquals(0, selectTrendIndex(200f, 332f, 1))
    }
}
