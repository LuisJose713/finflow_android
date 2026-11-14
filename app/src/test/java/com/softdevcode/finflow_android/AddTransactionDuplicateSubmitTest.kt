package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.data.repository.CategoryRepository
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.ui.screens.transactions.AddTransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class AddTransactionDuplicateSubmitTest {
    private val request = CreateTransactionRequest(
        description = "Compra de prueba",
        amount = 15000.0,
        type = "expense",
        categoryId = 1,
        date = "2026-11-14",
    )

    @Suppress("UNCHECKED_CAST")
    private fun api(onCreate: () -> Transaction): FinFlowApi =
        Proxy.newProxyInstance(
            FinFlowApi::class.java.classLoader,
            arrayOf(FinFlowApi::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getCategories", "getAccounts" -> emptyList<Any>()
                "createTransaction" -> onCreate()
                else -> error("Unexpected API request ${method.name}")
            }
        } as FinFlowApi

    @Test fun fastDoubleTapMakesOnlyOneFinancialMovement() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var created = 0
            val fake = api {
                created++
                Transaction(1, "Compra de prueba", 15000.0, "expense")
            }
            val vm = AddTransactionViewModel(TransactionRepository(fake), CategoryRepository(fake), fake)
            vm.createTransaction(request)
            vm.createTransaction(request)
            advanceUntilIdle()
            assertEquals(1, created)
            assertTrue(vm.uiState.value.saved)
            assertFalse(vm.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test fun userCanRetryAfterARealServerError() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var attempts = 0
            val fake = api {
                attempts++
                if (attempts == 1) throw IOException("Network unavailable")
                Transaction(1, "Compra de prueba", 15000.0, "expense")
            }
            val vm = AddTransactionViewModel(TransactionRepository(fake), CategoryRepository(fake), fake)
            vm.createTransaction(request)
            advanceUntilIdle()
            assertEquals(1, attempts)
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.saved)

            vm.createTransaction(request)
            advanceUntilIdle()
            assertEquals(2, attempts)
            assertTrue(vm.uiState.value.saved)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
