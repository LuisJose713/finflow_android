package com.softdevcode.finflow_android.domain.model

/** Read-only public billing catalog. A quote is not a payment or activated plan. */
data class BillingOffer(
    val code: String,
    val title: String,
    val description: String,
    val originalCop: Long,
    val priceCop: Long,
    val promoCode: String? = null,
    val promoTitle: String? = null,
    val savingsPercent: Int = 0,
    val checkoutEnabled: Boolean = false,
)

data class BillingCatalog(
    val currency: String = "COP",
    val checkoutEnabled: Boolean = false,
    val offers: List<BillingOffer> = emptyList(),
)

data class BillingEntitlement(
    val planCode: String = "FREE",
    val pro: Boolean = false,
    val validUntil: String? = null,
    val source: String? = null,
)

data class BillingPlan(
    val code: String,
    val title: String,
    val description: String,
    val priceCop: Long,
    val active: Boolean,
    val sortOrder: Int,
)

data class BillingPromotion(
    val code: String,
    val title: String,
    val planCode: String,
    val discountType: String,
    val discountValue: Long,
    val startsAt: String,
    val endsAt: String,
    val maxRedemptions: Int? = null,
    val redemptionCount: Int = 0,
    val active: Boolean,
    val published: Boolean,
)
data class BillingGrantRequest(val userId: Long, val planCode: String, val reason: String)

data class AdminMonthlyActivity(
    val month: String,
    val signups: Long,
    val transactions: Long,
)

data class AdminOverview(
    val users: Long = 0,
    val newUsers30Days: Long = 0,
    val activeUsers30Days: Long = 0,
    val transactions: Long = 0,
    val transactions30Days: Long = 0,
    val incomeTransactions: Long = 0,
    val expenseTransactions: Long = 0,
    val goals: Long = 0,
    val plannedExpenses: Long = 0,
    val pendingExpenses: Long = 0,
    val customCategories: Long = 0,
    val systemCategories: Long = 0,
    val monthlyActivity: List<AdminMonthlyActivity> = emptyList(),
)

data class AdminServiceHealth(
    val key: String,
    val name: String,
    val status: String,
    val latencyMs: Long? = null,
    val detail: String = "",
)

data class AdminPlatformHealth(
    val overall: String = "",
    val checkedAt: String = "",
    val services: List<AdminServiceHealth> = emptyList(),
)
