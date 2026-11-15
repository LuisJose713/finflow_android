package com.softdevcode.finflow_android.ui.theme

import android.content.Context
import com.softdevcode.finflow_android.domain.model.DisplayCurrencyQuote
import com.softdevcode.finflow_android.domain.model.MoneyDisplayQuote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * Only a visual estimate. All inputs and finance data remain in source COP.
 * Never fabricate a rate or silently relabel a COP balance as USD/EUR.
 */
object DisplayCurrencyManager {
    val supported = listOf("COP", "USD", "EUR", "GBP", "MXN", "BRL", "CAD")
    private val mutableQuote = MutableStateFlow(DisplayCurrencyQuote())
    val quote = mutableQuote.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val generation = AtomicInteger(0)
    private const val PREF = "finflow.displayCurrency"

    suspend fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val selected = prefs.getString("code", "COP") ?: "COP"
        if (selected !in supported || selected == "COP") return
        val rate = Double.fromBits(prefs.getLong("rateBits", 0L))
        val date = prefs.getString("date", "") ?: ""
        val fetchedAt = prefs.getLong("fetchedAt", 0L)
        if (valid(rate, date) && System.currentTimeMillis() - fetchedAt in 0 until 72L * 3600000L) {
            publish(DisplayCurrencyQuote(selected, rate, date))
        }
        select(context, selected)
    }

    suspend fun select(context: Context, code: String): Boolean {
        if (code !in supported) return false
        val request = generation.incrementAndGet()
        mutableError.value = null
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        if (code == "COP") {
            prefs.edit().putString("code", "COP").remove("date").remove("rateBits").apply()
            publish(DisplayCurrencyQuote())
            mutableBusy.value = false
            return true
        }
        mutableBusy.value = true
        return try {
            val received = withContext(Dispatchers.IO) {
                val connection = (URL("https://api.frankfurter.dev/v2/rate/cop/" + code.lowercase())
                    .openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    if (connection.responseCode != 200) error("FX service unavailable")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val body = JSONObject(response)
                    if (!body.getString("base").equals("COP", ignoreCase = true) ||
                        !body.getString("quote").equals(code, ignoreCase = true)) error("Invalid FX pair")
                    val rate = body.getDouble("rate")
                    val date = body.getString("date")
                    if (!valid(rate, date)) error("Invalid FX quote")
                    DisplayCurrencyQuote(code, rate, date)
                } finally {
                    connection.disconnect()
                }
            }
            if (request != generation.get()) return false
            prefs.edit()
                .putString("code", received.code)
                .putLong("rateBits", received.rate.toBits())
                .putString("date", received.date)
                .putLong("fetchedAt", System.currentTimeMillis())
                .apply()
            publish(received)
            true
        } catch (_: Exception) {
            if (request == generation.get()) {
                mutableError.value = "No pudimos consultar la tasa. Se conserva la moneda anterior."
            }
            false
        } finally {
            if (request == generation.get()) mutableBusy.value = false
        }
    }

    private fun publish(value: DisplayCurrencyQuote) {
        MoneyDisplayQuote.active = value
        mutableQuote.value = value
    }

    private fun valid(rate: Double, date: String): Boolean {
        if (!rate.isFinite() || rate <= 0 || rate >= 1000) return false
        return try {
            val day = LocalDate.parse(date)
            val delta = java.time.temporal.ChronoUnit.DAYS.between(day, LocalDate.now(ZoneOffset.UTC))
            delta in -1..14
        } catch (_: Exception) { false }
    }
}
