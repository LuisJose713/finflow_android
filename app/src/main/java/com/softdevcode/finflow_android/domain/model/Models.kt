package com.softdevcode.finflow_android.domain.model


// ── Auth ──────────────────────────────────────────────────────────────────────
data class User(
    val id: Int,
    val email: String,
    val name: String,
)

data class AuthResponse(
    val token: String,
    val user: User,
)

data class LoginRequest(
    val email: String,
    val password: String,
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
)

// ── Transaction ───────────────────────────────────────────────────────────────
data class Transaction(
    val id: Int,
    val description: String,
    val amount: Double,
    val type: String,           // "income" | "expense"
    val categoryId: Int = 0,
    val date: String? = null,
    val createdAt: String? = null,
    val currency: String? = null,
    val status: String? = null,
    val userId: Int? = null,
)

data class CreateTransactionRequest(
    val description: String,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val date: String,
    val notes: String? = null,
)

data class MonthlyData(
    val month: String,
    val income: Double,
    val expenses: Double,
)

data class TransactionStats(
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val balance: Double = 0.0,
    val savingsRate: Double = 0.0,
    val monthlyData: List<MonthlyData> = emptyList(),
)

// ── Category ──────────────────────────────────────────────────────────────────
data class Category(
    val id: Int,
    val name: String,
    val type: String,
)

// ── Goal ──────────────────────────────────────────────────────────────────────
data class Goal(
    val id: Int,
    val userId: Int,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val deadline: String? = null,
    val description: String? = null,
    val status: String,     // "active" | "completed"
    val createdAt: String? = null,
) {
    val progress: Float get() =
        if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}

data class GoalRequest(
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double? = null,
    val deadline: String? = null,
    val description: String? = null,
)

data class ContributionRequest(
    val amount: Double,
    val note: String? = null,
)

// ── Planned Expense ───────────────────────────────────────────────────────────
data class PlannedExpense(
    val id: Int,
    val userId: Int,
    val title: String,
    val amount: Double,
    val categoryId: Int? = null,
    val dueDate: String? = null,
    val remindAt: String? = null,
    val notes: String? = null,
    val status: String,     // "pending" | "done" | "cancelled"
    val executedTransactionId: Int? = null,
    val createdAt: String? = null,
)

data class PlannedExpenseRequest(
    val title: String,
    val amount: Double,
    val categoryId: Int? = null,
    val dueDate: String? = null,
    val remindAt: String? = null,
    val notes: String? = null,
)