package com.softdevcode.finflow_android

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.data.repository.GoalRepository
import com.softdevcode.finflow_android.data.repository.PlannedExpenseRepository
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.ui.screens.goals.GoalsViewModel
import com.softdevcode.finflow_android.ui.screens.modules.FinFlowModulesViewModel
import com.softdevcode.finflow_android.ui.screens.plannedexpenses.PlannedExpensesViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException
import java.lang.reflect.Proxy
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.startCoroutineUninterceptedOrReturn

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceRefreshStabilityTest {
    @Suppress("UNCHECKED_CAST")
    private fun api(response: suspend (String, List<Any?>) -> Any): FinFlowApi =
        Proxy.newProxyInstance(FinFlowApi::class.java.classLoader, arrayOf(FinFlowApi::class.java)) { _, method, args ->
            val work: suspend () -> Any = { response(method.name, args!!.dropLast(1)) }
            work.startCoroutineUninterceptedOrReturn(args!!.last() as Continuation<Any>)
        } as FinFlowApi

    @Test fun pollingDoesNotOverlapAnActiveRead() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var reads = 0
            val release = CompletableDeferred<Unit>()
            val vm = FinFlowModulesViewModel(api { name, _ ->
                check(name == "getCategories")
                reads++
                release.await()
                emptyList<Category>()
            })
            vm.refresh("categories")
            runCurrent()
            vm.refresh("categories", silent = true)
            runCurrent()
            assertEquals(1, reads)
            assertTrue(vm.state.value.busy)
            release.complete(Unit)
            advanceUntilIdle()
            assertFalse(vm.state.value.busy)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun anOldMonthCannotOverwriteTheNewSelectedMonth() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val oldResponse = CompletableDeferred<Unit>()
            val vm = FinFlowModulesViewModel(api { name, args ->
                when (name) {
                    "getCategories" -> emptyList<Category>()
                    "getBudgets" -> {
                        val month = args[1] as Int
                        if (month == 11) withContext(NonCancellable) { oldResponse.await() }
                        listOf(MonthlyBudget(month, 1, year = 2026, month = month, limitAmount = 500000.0))
                    }
                    else -> error("Unexpected API call $name")
                }
            })
            vm.refresh("budgets", 2026, 11)
            runCurrent()
            vm.refresh("budgets", 2026, 12)
            runCurrent()
            assertEquals(12, vm.state.value.budgets.single().month)
            oldResponse.complete(Unit)
            advanceUntilIdle()
            assertEquals(12, vm.state.value.budgets.single().month)
            assertFalse(vm.state.value.busy)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aDoubleTapAndASlowReloadCreateOnlyOneWallet() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            var writes = 0
            val reload = CompletableDeferred<Unit>()
            val account = FinancialAccount(1, "Ahorro", "bank")
            val vm = FinFlowModulesViewModel(api { name, _ ->
                when (name) {
                    "createAccount" -> { writes++; account }
                    "getAccounts" -> { reload.await(); listOf(account) }
                    "getTransfers" -> emptyList<AccountTransfer>()
                    else -> error("Unexpected API call $name")
                }
            })
            vm.createAccount("Ahorro", "bank", 0.0)
            vm.createAccount("Ahorro", "bank", 0.0)
            runCurrent()
            assertTrue(vm.state.value.busy)
            assertEquals("Cuenta creada", vm.state.value.notice)
            vm.createAccount("Ahorro", "bank", 0.0)
            vm.refresh("accounts", silent = true)
            runCurrent()
            assertEquals(1, writes)
            reload.complete(Unit)
            advanceUntilIdle()
            assertEquals(listOf(account), vm.state.value.accounts)
            assertFalse(vm.state.value.busy)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aSuccessfulWriteIsPreservedWhenItsReloadFails() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val vm = FinFlowModulesViewModel(api { name, _ ->
                when (name) {
                    "createAccount" -> FinancialAccount(1, "Ahorro", "bank")
                    "getAccounts" -> throw IOException("Offline after saving")
                    else -> error("Unexpected API call $name")
                }
            })
            vm.createAccount("Ahorro", "bank", 0.0)
            advanceUntilIdle()
            assertEquals("Cuenta creada", vm.state.value.notice)
            assertTrue(vm.state.value.error.orEmpty().contains("se guardó"))
            assertFalse(vm.state.value.busy)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aGoalSavedDuringPollingIsNotRemovedByTheOldResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val oldResponse = CompletableDeferred<Unit>()
            var writes = 0
            val goal = Goal(1, 1, "Fondo", 1000000.0, 0.0, status = "active")
            val vm = GoalsViewModel(GoalRepository(api { name, _ ->
                when (name) {
                    "getGoals" -> { withContext(NonCancellable) { oldResponse.await() }; emptyList<Goal>() }
                    "createGoal" -> { writes++; goal }
                    else -> error("Unexpected API call $name")
                }
            }))
            runCurrent()
            vm.createGoal(GoalRequest("Fondo", 1000000.0))
            vm.createGoal(GoalRequest("Fondo", 1000000.0))
            runCurrent()
            oldResponse.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, writes)
            assertEquals(listOf(goal), vm.uiState.value.goals)
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.operationLoading)
        } finally { Dispatchers.resetMain() }
    }

    @Test fun aPlannedExpenseSavedDuringPollingSurvivesItsOldResponse() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val oldResponse = CompletableDeferred<Unit>()
            var writes = 0
            val expense = PlannedExpense(1, 1, "Internet", 90000.0, status = "pending")
            val vm = PlannedExpensesViewModel(PlannedExpenseRepository(api { name, _ ->
                when (name) {
                    "getPlannedExpenses" -> { withContext(NonCancellable) { oldResponse.await() }; emptyList<PlannedExpense>() }
                    "createPlannedExpense" -> { writes++; expense }
                    else -> error("Unexpected API call $name")
                }
            }))
            runCurrent()
            vm.createPlannedExpense(PlannedExpenseRequest("Internet", 90000.0))
            vm.createPlannedExpense(PlannedExpenseRequest("Internet", 90000.0))
            runCurrent()
            oldResponse.complete(Unit)
            advanceUntilIdle()
            assertEquals(1, writes)
            assertEquals(listOf(expense), vm.uiState.value.expenses)
            assertFalse(vm.uiState.value.isLoading)
            assertFalse(vm.uiState.value.operationLoading)
        } finally { Dispatchers.resetMain() }
    }
}
