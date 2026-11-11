package com.softdevcode.finflow_android

import com.google.gson.Gson
import com.softdevcode.finflow_android.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/** Regression checks against the authenticated Spring Boot /accounts, /budgets,
 * /recurring and /privacy JSON contracts. They never access real user data. */
class FinanceModulesContractTest {
    private val gson = Gson()

    @Test fun financialAccountPayloadUsesTheProductionApiFieldNames() {
        val json = gson.toJson(FinancialAccountRequest(
            name = "Ahorros", type = "savings", currency = "COP", openingBalance = 125000.0))
        assertTrue(json.contains("\"openingBalance\":125000.0"))
        assertTrue(json.contains("\"currency\":\"COP\""))
        assertTrue(json.contains("\"type\":\"savings\""))
        val account = gson.fromJson(
            """{"id":3,"name":"Ahorros","type":"savings","currency":"COP",
                "openingBalance":125000,"balance":130000,"archived":false}""",
            FinancialAccount::class.java,
        )
        assertEquals(130000.0, account.balance, 0.001)
    }

    @Test fun recurringAndBudgetContractsRetainTheirAmounts() {
        val budget = gson.fromJson(
            """{"id":4,"categoryId":2,"categoryName":"Alimentación","year":2026,
                "month":11,"limitAmount":500000,"spent":150000,"remaining":350000,
                "percentUsed":30,"status":"ok","currency":"COP"}""",
            MonthlyBudget::class.java,
        )
        assertEquals("Alimentación", budget.categoryName)
        assertEquals(30, budget.percentUsed)
        assertEquals(350000.0, budget.remaining, 0.001)
        val recurring = RecurringRuleRequest(
            title = "Arriendo", amount = 800000.0, type = "expense",
            categoryId = 2, accountId = null, dayOfMonth = 5,
        )
        val json = gson.toJson(recurring)
        assertTrue(json.contains("\"dayOfMonth\":5"))
        assertTrue(json.contains("\"categoryId\":2"))
    }

    @Test fun privacyRequestUsesRequiredSnakeCaseResponseAndExplicitOptIn() {
        val json = gson.toJson(FinFlowMarketingChoice(false))
        assertTrue(json.contains("\"marketingConsent\":false"))
        val entry = gson.fromJson(
            """{"id":11,"request_type":"ACCESS","description":"Mis datos",
                "status":"PENDING","created_at":"2026-11-11"}""",
            FinFlowRightsEntry::class.java,
        )
        assertEquals("ACCESS", entry.requestType)
        assertEquals("PENDING", entry.status)
    }

    @Test fun globalCategoryHasNoOwnerAndCannotBeAccidentallyTreatedAsCustom() {
        val global = gson.fromJson(
            """{"id":1,"name":"Alimentos","type":"Expense","userId":null}""",
            Category::class.java,
        )
        val owned = gson.fromJson(
            """{"id":2,"name":"Ocio","type":"Expense","userId":42}""",
            Category::class.java,
        )
        assertNull(global.userId)
        assertEquals(42, owned.userId)
    }
}
