package com.softdevcode.finflow_android.ui.theme

import android.content.Context
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.DisplayCurrencyQuote
import com.softdevcode.finflow_android.domain.model.MoneyDisplayQuote
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Per-user, server-synchronized display currency.
 * Original COP amounts and inputs are NEVER converted for persistence.
 */
object DisplayCurrencyManager {
    val supported = listOf("COP", "USD", "EUR", "GBP", "MXN", "BRL", "CAD")
    private val mutableQuote = MutableStateFlow(DisplayCurrencyQuote())
    val quote = mutableQuote.asStateFlow()
    private val mutableBusy = MutableStateFlow(false)
    val busy = mutableBusy.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error = mutableError.asStateFlow()
    private val generation = AtomicInteger()
    private var owner: Int? = null
    private var api: FinFlowApi? = null
    private const val PREF_PREFIX = "finflow.displayCurrency.user."

    suspend fun initialize(context: Context, client: FinFlowApi, userId: Int?) {
        val id = generation.incrementAndGet()
        owner = userId
        api = client
        mutableError.value = null
        mutableBusy.value = false
        publish(DisplayCurrencyQuote())  // Never leak the previous account's preference.
        if (userId == null) return
        try {
            val selected = client.getPreferredCurrency().currency
            if (id != generation.get() || owner != userId) return
            if (selected !in supported || selected == "COP") return
            val cached = cachedQuote(context, userId, selected)
            if (cached != null) publish(cached)
            try {
                val quote = fetchQuote(selected)
                if (id != generation.get() || owner != userId) return
                persistCache(context, userId, quote)
                publish(quote)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (cached == null && id == generation.get()) mutableError.value =
                    "Sin tasa reciente para $selected. Los importes se muestran en COP."
            }
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (id == generation.get()) mutableError.value =
                "No pudimos sincronizar tu moneda. Los importes siguen en COP."
        }
    }

    suspend fun select(context: Context, code: String): Boolean {
        if (code !in supported) return false
        val client = api ?: return false
        val userId = owner ?: return false
        val id = generation.incrementAndGet()
        mutableBusy.value = true
        mutableError.value = null
        return try {
            val quote = if (code == "COP") DisplayCurrencyQuote() else fetchQuote(code)
            if (id != generation.get() || owner != userId) return false
            val saved = client.setPreferredCurrency(mapOf("currency" to code))
            if (saved.currency != code) throw IllegalStateException("Inconsistent default currency")
            if (id != generation.get() || owner != userId) return false
            persistCache(context, userId, quote)
            publish(quote)
            true
        } catch (e: CancellationException) { throw e }
        catch (_: Exception) {
            if (id == generation.get()) mutableError.value =
                "No se guardó tu moneda predeterminada. Se conserva la anterior."
            false
        } finally {
            if (id == generation.get()) mutableBusy.value = false
        }
    }

    private suspend fun fetchQuote(code: String): DisplayCurrencyQuote = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.frankfurter.dev/v2/rate/cop/" + code.lowercase())
            .openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("Accept", "application/json")
        }
        try {
            if (connection.responseCode != 200) throw IllegalStateException("FX unavailable")
            val body = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            if (!body.getString("base").equals("COP", ignoreCase = true) ||
                !body.getString("quote").equals(code, ignoreCase = true)) throw IllegalStateException("Invalid pair")
            val rate = body.getDouble("rate")
            val date = body.getString("date")
            if (!valid(rate, date)) throw IllegalStateException("Invalid quote")
            DisplayCurrencyQuote(code, rate, date)
        } finally { connection.disconnect() }
    }

    private fun cachedQuote(context: Context, userId: Int, code: String): DisplayCurrencyQuote? {
        val prefs = context.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE)
        val rate = Double.fromBits(prefs.getLong("rateBits", 0))
        val date = prefs.getString("date", "") ?: ""
        val saved = prefs.getLong("fetchedAt", 0)
        if (prefs.getString("code", "") != code || !valid(rate, date) ||
            System.currentTimeMillis() - saved !in 0..72L * 3600000L) return null
        return DisplayCurrencyQuote(code, rate, date)
    }

    private fun persistCache(context: Context, userId: Int, value: DisplayCurrencyQuote) {
        context.getSharedPreferences(PREF_PREFIX + userId, Context.MODE_PRIVATE).edit()
            .putString("code", value.code)
            .putLong("rateBits", value.rate.toBits())
            .putString("date", value.date)
            .putLong("fetchedAt", System.currentTimeMillis())
            .apply()
    }

    private fun publish(value: DisplayCurrencyQuote) {
        MoneyDisplayQuote.active = value
        mutableQuote.value = value
    }

    private fun valid(rate: Double, date: String): Boolean {
        if (!rate.isFinite() || rate <= 0 || rate >= 1000) return false
        return try {
            ChronoUnit.DAYS.between(LocalDate.parse(date), LocalDate.now(ZoneOffset.UTC)) in -1L..14L
        } catch (_: Exception) { false }
    }
}
