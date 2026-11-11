package com.softdevcode.finflow_android

import com.google.gson.Gson
import com.softdevcode.finflow_android.domain.model.*
import org.junit.Assert.*
import org.junit.Test

/** API contract regressions. No credentials, payments or network calls. */
class BillingRegistrationContractTest {
    private val gson = Gson()

    @Test fun publicCatalogIsInformativeAndPreservesPrices() {
        val data = gson.fromJson(
            """{"currency":"COP","checkoutEnabled":false,
              "offers":[{"code":"MONTHLY","title":"FinFlow Pro","description":"Plan",
              "originalCop":18000,"priceCop":14000,"promoCode":"WELCOME",
              "promoTitle":"Bienvenida","savingsPercent":22,"checkoutEnabled":false}]}""",
            BillingCatalog::class.java,
        )
        assertEquals("COP", data.currency)
        assertFalse(data.checkoutEnabled)
        assertEquals(1, data.offers.size)
        assertEquals(14_000L, data.offers.first().priceCop)
        assertFalse(data.offers.first().checkoutEnabled)
    }

    @Test fun authenticatedEntitlementDoesNotPretendAQuoteIsPayment() {
        val entitlement = gson.fromJson(
            """{"planCode":"TRIAL","pro":true,"source":"FREE_TRIAL",
                "validUntil":"2026-12-10T00:00:00Z"}""",
            BillingEntitlement::class.java,
        )
        assertEquals("TRIAL", entitlement.planCode)
        assertEquals("FREE_TRIAL", entitlement.source)
        val offer = gson.fromJson(
            """{"code":"MONTHLY","title":"Pro","description":"Paid",
                "originalCop":20000,"priceCop":15000,"savingsPercent":25,
                "checkoutEnabled":false}""",
            BillingOffer::class.java,
        )
        assertNotEquals(offer.code, entitlement.planCode)
        assertFalse(offer.checkoutEnabled)
    }

    @Test fun adminDashboardIsAggregatedRatherThanExposingUserFinancialDetails() {
        val overview = gson.fromJson(
            """{"users":63,"newUsers30Days":9,"activeUsers30Days":23,
                "transactions":918,"transactions30Days":122,"goals":51,
                "plannedExpenses":36,"pendingExpenses":12,"customCategories":18,
                "systemCategories":8,"monthlyActivity":[]}""",
            AdminOverview::class.java,
        )
        assertEquals(63L, overview.users)
        assertEquals(122L, overview.transactions30Days)
        assertEquals(0, overview.monthlyActivity.size)
        val text = gson.toJson(overview)
        assertFalse(text.contains("accountNumber"))
        assertFalse(text.contains("password"))
    }

    @Test fun registrationIncludesVersionedConsentOnlyWhenTheUserAcceptsPublishedPolicy() {
        val authorized = RegisterRequest(
            email = "test@example.com", password = "aLongPassword#5", name = "Ana Test",
            confirmPassword = "aLongPassword#5",
            privacyAccepted = true, privacyVersion = "2026-11",
            marketingConsent = false,
        )
        val json = gson.toJson(authorized)
        assertTrue(json.contains("\"privacyAccepted\":true"))
        assertTrue(json.contains("\"privacyVersion\":\"2026-11\""))
        assertTrue(json.contains("\"marketingConsent\":false"))

        val unpublished = RegisterRequest(
            email = "test@example.com", password = "aLongPassword#5", name = "Ana Test",
            confirmPassword = "aLongPassword#5",
        )
        val fallback = gson.toJson(unpublished)
        assertFalse(fallback.contains("privacyAccepted"))
        assertFalse(fallback.contains("privacyVersion"))
        assertFalse(fallback.contains("marketingConsent"))
    }

    @Test fun roleComesFromBackendAndDefaultsToUnprivilegedUser() {
        val admin = gson.fromJson(
            """{"id":1,"name":"Admin","email":"admin@example.com","role":"ADMIN"}""",
            User::class.java,
        )
        assertEquals("ADMIN", admin.role)
        val clientFallback = User(1, "admin@example.com", "Admin")
        assertEquals("USER", clientFallback.role)
    }
}
