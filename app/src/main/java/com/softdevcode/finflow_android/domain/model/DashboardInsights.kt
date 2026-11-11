package com.softdevcode.finflow_android.domain.model

/** Shares of registered activity; this does not represent spendable cash. */
fun activityShare(amount: Double, income: Double, expenses: Double): Float {
    val total = income.coerceAtLeast(0.0) + expenses.coerceAtLeast(0.0)
    if (!total.isFinite() || total <= 0.0 || !amount.isFinite()) return 0f
    return (amount.coerceAtLeast(0.0) / total).toFloat().coerceIn(0f, 1f)
}

fun List<Transaction>.mostRecent(limit: Int = 5): List<Transaction> =
    sortedWith(compareByDescending<Transaction> { it.date ?: it.createdAt ?: "" }
        .thenByDescending { it.id }).take(limit.coerceAtLeast(0))
