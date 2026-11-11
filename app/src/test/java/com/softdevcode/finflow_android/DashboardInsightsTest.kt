package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.TransactionStats
import com.softdevcode.finflow_android.domain.model.activityShare
import com.softdevcode.finflow_android.domain.model.mostRecent
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardUiState
import com.softdevcode.finflow_android.ui.screens.dashboard.withResults
import com.softdevcode.finflow_android.util.Result
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DashboardInsightsTest {
    @Test
    fun activityBarsReflectTheirActualProportion() {
        assertEquals(0.75f, activityShare(300.0, 300.0, 100.0), 0.0001f)
        assertEquals(0.25f, activityShare(100.0, 300.0, 100.0), 0.0001f)
        assertEquals(0f, activityShare(0.0, 300.0, 0.0), 0f)
    }

    @Test
    fun emptyAndInvalidActivityProducesNoFilledBar() {
        assertEquals(0f, activityShare(0.0, 0.0, 0.0), 0f)
        assertEquals(0f, activityShare(Double.NaN, 100.0, 50.0), 0f)
        assertEquals(0f, activityShare(-20.0, 100.0, 50.0), 0f)
        assertEquals(0f, activityShare(10.0, Double.POSITIVE_INFINITY, 10.0), 0f)
    }

    @Test
    fun recentTransactionsAreSortedBeforeTheLimitIsApplied() {
        val transactions = listOf(
            Transaction(1, "Old", 1.0, "income", date = "2026-01-01"),
            Transaction(2, "Recent", 1.0, "income", date = "2026-11-11"),
            Transaction(3, "Fallback", 1.0, "income", createdAt = "2026-10-01T12:00:00Z"),
        )
        assertEquals(listOf(2, 3), transactions.mostRecent(2).map { it.id })
        assertEquals(emptyList<Transaction>(), transactions.mostRecent(0))
    }

    @Test
    fun failedRefreshPreservesPreviouslyLoadedMoneyAndTimestamp() {
        val previous = DashboardUiState(
            stats = TransactionStats(totalIncome = 300.0, totalExpenses = 100.0, balance = 200.0),
            recentTransactions = listOf(Transaction(1, "Salary", 300.0, "income")),
            categories = listOf(Category(1, "Salary", "income")),
            hasLoadedTransactions = true, lastUpdatedAt = 10,
        )
        val failed = Result.Error("network error")
        val refreshed = previous.withResults(failed, failed, failed, 20)
        assertEquals(previous.stats, refreshed.stats)
        assertEquals(previous.recentTransactions, refreshed.recentTransactions)
        assertEquals(previous.categories, refreshed.categories)
        assertEquals(10L, refreshed.lastUpdatedAt)
        assertTrue(refreshed.hasLoadedTransactions)
        assertNotNull(refreshed.error)
        assertFalse(refreshed.isLoading)
    }

    @Test
    fun initialFailureIsNotPresentedAsAnEmptyAccount() {
        val failed = Result.Error("network error")
        val state = DashboardUiState().withResults(failed, failed, failed, 20)
        assertNull(state.stats)
        assertFalse(state.hasLoadedTransactions)
        assertNull(state.lastUpdatedAt)
    }

    @Test
    fun successfulRefreshCanEmptyTheAccountAndClearsTheError() {
        val state = DashboardUiState(error = "Previous error", lastUpdatedAt = 10).withResults(
            Result.Success(TransactionStats()), Result.Success(emptyList()), Result.Success(emptyList()), 20,
        )
        assertTrue(state.hasLoadedTransactions)
        assertTrue(state.recentTransactions.isEmpty())
        assertEquals(20L, state.lastUpdatedAt)
        assertNull(state.error)
    }
}
