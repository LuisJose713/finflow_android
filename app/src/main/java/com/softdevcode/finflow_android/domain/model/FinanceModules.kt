package com.softdevcode.finflow_android.domain.model

/** Contracts shared with the production FinFlow Spring Boot API. */
data class FinancialAccount(
    val id: Int,
    val name: String,
    val type: String,
    val currency: String = "COP",
    val openingBalance: Double = 0.0,
    val balance: Double = 0.0,
    val archived: Boolean = false,
)

data class FinancialAccountRequest(
    val name: String,
    val type: String,
    val currency: String = "COP",
    val openingBalance: Double = 0.0,
)

data class FinancialAccountUpdate(val name: String, val type: String)
data class ArchiveAccountRequest(val archived: Boolean)

data class AccountTransfer(
    val id: Int,
    val fromAccountId: Int,
    val toAccountId: Int,
    val amount: Double,
    val date: String,
    val note: String? = null,
)

data class AccountTransferRequest(
    val fromAccountId: Int,
    val toAccountId: Int,
    val amount: Double,
    val date: String,
    val note: String? = null,
)

data class MonthlyBudget(
    val id: Int,
    val categoryId: Int,
    val categoryName: String = "",
    val year: Int,
    val month: Int,
    val limitAmount: Double,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentUsed: Int = 0,
    val status: String = "ok",
    val currency: String = "COP",
)

data class MonthlyBudgetRequest(
    val categoryId: Int,
    val year: Int,
    val month: Int,
    val amount: Double,
)
data class BudgetAmountRequest(val amount: Double)

data class RecurringRule(
    val id: Int,
    val title: String,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val accountId: Int? = null,
    val dayOfMonth: Int,
    val nextDueDate: String? = null,
    val active: Boolean = true,
)

data class RecurringRuleRequest(
    val title: String,
    val amount: Double,
    val type: String,
    val categoryId: Int,
    val accountId: Int? = null,
    val dayOfMonth: Int,
)

data class ToggleRecurringRequest(val active: Boolean)
data class RecurringExecutionResult(val created: Int)
data class CategoryPayload(val name: String, val type: String)

// The AI endpoint uses 'mensaje'; unlike the legacy web /advisor path, this
// route is present in the deployed API controller.
data class FinancialChatRequest(val mensaje: String)
