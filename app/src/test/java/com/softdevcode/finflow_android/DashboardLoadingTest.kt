package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.domain.model.TransactionStats
import com.softdevcode.finflow_android.ui.screens.dashboard.DashboardViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardLoadingTest {
    private val stats = TransactionStats(totalIncome = 100.0, balance = 100.0)
    private val transactions = listOf(Transaction(1, "Ingreso", 100.0, "income"))
    private val categories = listOf(Category(1, "Ingresos", "income"))

    @Suppress("UNCHECKED_CAST")
    private fun api(stalled: () -> Boolean): FinFlowApi = Proxy.newProxyInstance(
        FinFlowApi::class.java.classLoader, arrayOf(FinFlowApi::class.java),
    ) { _, method, args ->
        if (stalled()) {
            val pending: suspend () -> Any = { awaitCancellation() }
            pending.startCoroutineUninterceptedOrReturn(args!!.last() as Continuation<Any>)
        } else when (method.name) {
            "getStats" -> stats
            "getTransactions" -> transactions
            "getCategories" -> categories
            else -> error("Unexpected API call ${method.name}")
        }
    } as FinFlowApi

    @Test fun aStalledDashboardStopsLoadingAndOffersRetry() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = DashboardViewModel(TransactionRepository(api { true }))
            val result = vm.uiState.first { !it.isLoading }
            assertTrue(result.error.orEmpty().contains("reintentar"))
            assertNull(result.stats)
            assertFalse(result.hasLoadedTransactions)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun aSlowRefreshPreservesThePreviouslyLoadedFinancialData() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var stalled = false
            val vm = DashboardViewModel(TransactionRepository(api { stalled }))
            val initial = vm.uiState.first { !it.isLoading }
            assertEquals(stats, initial.stats)
            stalled = true
            vm.refresh()
            runCurrent()
            val result = vm.uiState.first { !it.isLoading }
            assertEquals(initial.stats, result.stats)
            assertEquals(initial.recentTransactions, result.recentTransactions)
            assertEquals(initial.lastUpdatedAt, result.lastUpdatedAt)
            assertNotNull(result.error)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
