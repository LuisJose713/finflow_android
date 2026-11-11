package com.softdevcode.finflow_android.ui.screens.dashboard

import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.domain.model.TransactionStats
import com.softdevcode.finflow_android.domain.model.mostRecent
import com.softdevcode.finflow_android.util.Result

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: TransactionStats? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null,
    val hasLoadedTransactions: Boolean = false,
    val lastUpdatedAt: Long? = null,
)

/** A failed refresh must never turn previously loaded financial data into an empty account. */
internal fun DashboardUiState.withResults(
    statsResult: Result<TransactionStats>,
    transactionsResult: Result<List<Transaction>>,
    categoriesResult: Result<List<Category>>,
    updatedAt: Long,
): DashboardUiState {
    val complete = statsResult is Result.Success && transactionsResult is Result.Success && categoriesResult is Result.Success
    return copy(
        isLoading = false,
        stats = statsResult.getOrNull() ?: stats,
        recentTransactions = transactionsResult.getOrNull()?.mostRecent() ?: recentTransactions,
        categories = categoriesResult.getOrNull() ?: categories,
        hasLoadedTransactions = hasLoadedTransactions || transactionsResult is Result.Success,
        lastUpdatedAt = if (complete) updatedAt else lastUpdatedAt,
        error = if (complete) null else "Revisa tu conexión y vuelve a intentarlo.",
    )
}
