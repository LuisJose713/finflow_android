package com.softdevcode.finflow_android.ui.screens.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONObject
import javax.inject.Inject

data class ChatLine(val user: Boolean, val text: String)

data class ModulesState(
    val accounts: List<FinancialAccount> = emptyList(),
    val transfers: List<AccountTransfer> = emptyList(),
    val budgets: List<MonthlyBudget> = emptyList(),
    val rules: List<RecurringRule> = emptyList(),
    val categories: List<Category> = emptyList(),
    val chat: List<ChatLine> = emptyList(),
    val privacyPolicy: FinFlowPrivacyPolicy? = null,
    val privacyStatus: FinFlowPrivacyStatus? = null,
    val privacyRequests: List<FinFlowRightsEntry> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

@HiltViewModel
class FinFlowModulesViewModel @Inject constructor(private val api: FinFlowApi) : ViewModel() {
    private val _state = MutableStateFlow(ModulesState())
    val state: StateFlow<ModulesState> = _state.asStateFlow()

    private var selectedMonth = 1
    private var selectedYear = 2026

    fun clearMessages() = _state.update { it.copy(error = null, notice = null) }

    fun refresh(section: String, year: Int = selectedYear, month: Int = selectedMonth) {
        selectedYear = year
        selectedMonth = month
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                when (section) {
                    "accounts" -> {
                        val accounts = api.getAccounts()
                        val transfers = api.getTransfers()
                        _state.update { it.copy(accounts = accounts, transfers = transfers) }
                    }
                    "budgets" -> {
                        val categories = api.getCategories()
                        val budgets = api.getBudgets(year, month)
                        _state.update { it.copy(budgets = budgets, categories = categories) }
                    }
                    "recurring" -> {
                        val categories = api.getCategories()
                        val accounts = api.getAccounts()
                        val rules = api.getRecurringRules()
                        _state.update {
                            it.copy(rules = rules, categories = categories, accounts = accounts)
                        }
                    }
                    "categories" -> {
                        _state.update { it.copy(categories = api.getCategories()) }
                    }
                    "privacy" -> {
                        val policy = api.getPrivacyPolicy()
                        val status = api.getPrivacyStatus()
                        val requests = api.getPrivacyRequests()
                        _state.update {
                            it.copy(privacyPolicy = policy, privacyStatus = status, privacyRequests = requests)
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo cargar esta sección. Comprueba la conexión.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    private fun runAction(section: String, success: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                block()
                _state.update { it.copy(notice = success) }
                refresh(section)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo guardar. Revisa los datos y vuelve a intentarlo.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun createAccount(name: String, type: String, balance: Double) =
        runAction("accounts", "Cuenta creada") {
            api.createAccount(FinancialAccountRequest(name.trim(), type, openingBalance = balance))
        }

    fun updateAccount(id: Int, name: String, type: String) =
        runAction("accounts", "Cuenta actualizada") {
            api.updateAccount(id, FinancialAccountUpdate(name.trim(), type))
        }

    fun archiveAccount(id: Int, archived: Boolean) =
        runAction("accounts", if (archived) "Cuenta archivada" else "Cuenta activada") {
            api.archiveAccount(id, ArchiveAccountRequest(archived))
        }

    fun transfer(from: Int, to: Int, amount: Double, date: String, note: String) =
        runAction("accounts", "Transferencia registrada") {
            api.createTransfer(AccountTransferRequest(from, to, amount, date, note.take(255)))
        }

    fun createBudget(category: Int, year: Int, month: Int, amount: Double) =
        runAction("budgets", "Presupuesto creado") {
            val response = api.createBudget(MonthlyBudgetRequest(category, year, month, amount))
            if (!response.isSuccessful) error("No se pudo crear el presupuesto")
        }

    fun changeBudget(id: Int, amount: Double) =
        runAction("budgets", "Presupuesto actualizado") {
            val response = api.updateBudget(id, BudgetAmountRequest(amount))
            if (!response.isSuccessful) error("No se pudo actualizar el presupuesto")
        }

    fun deleteBudget(id: Int) =
        runAction("budgets", "Presupuesto eliminado") {
            if (!api.deleteBudget(id).isSuccessful) error("No se pudo eliminar")
        }

    fun createRecurringRule(
        title: String, amount: Double, type: String, categoryId: Int,
        accountId: Int?, day: Int,
    ) = runAction("recurring", "Movimiento recurrente creado") {
        api.createRecurringRule(
            RecurringRuleRequest(title.trim(), amount, type, categoryId, accountId, day)
        )
    }

    fun setRecurringActive(id: Int, active: Boolean) =
        runAction("recurring", if (active) "Regla activada" else "Regla pausada") {
            api.setRecurringActive(id, ToggleRecurringRequest(active))
        }

    fun deleteRecurringRule(id: Int) =
        runAction("recurring", "Regla eliminada") {
            if (!api.deleteRecurringRule(id).isSuccessful) error("No se pudo eliminar")
        }

    fun processDue() = runAction("recurring", "Movimientos pendientes procesados") {
        api.processRecurringDue()
    }

    fun createCategory(name: String, type: String) =
        runAction("categories", "Categoría creada") {
            api.createCategory(CategoryPayload(name.trim(), type))
        }

    fun updateCategory(id: Int, name: String, type: String) =
        runAction("categories", "Categoría actualizada") {
            api.updateCategory(id, CategoryPayload(name.trim(), type))
        }

    fun deleteCategory(id: Int) =
        runAction("categories", "Categoría eliminada") {
            if (!api.deleteCategory(id).isSuccessful) error("No se pudo eliminar")
        }

    fun changePassword(
        current: String, next: String, confirmation: String, onReauthenticate: () -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val response = api.changeFinFlowPassword(
                    ChangeFinFlowPasswordRequest(current, next, confirmation)
                )
                if (!response.isSuccessful) error("Contraseña actual o nueva inválida")
                onReauthenticate() // New token version invalidates this and other sessions.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo cambiar la contraseña.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun revokeSessions(password: String, onReauthenticate: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val response = api.revokeFinFlowSessions(
                    ConfirmFinFlowPasswordRequest(password)
                )
                if (!response.isSuccessful) error("Contraseña incorrecta")
                onReauthenticate() // Never keep using a revoked JWT.
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "No fue posible cerrar las sesiones.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun setMarketingChoice(marketingConsent: Boolean) =
        runAction("privacy", "Preferencia de comunicaciones actualizada") {
            if (!api.setMarketing(FinFlowMarketingChoice(marketingConsent)).isSuccessful) {
                error("No se pudo cambiar la autorización")
            }
        }

    fun requestPrivacyRight(type: String, description: String) =
        runAction("privacy", "Solicitud registrada para revisión") {
            val response = api.createPrivacyRequest(
                FinFlowRightsRequest(type, description.trim().take(500))
            )
            if (!response.isSuccessful) error("No se pudo registrar la solicitud")
        }

    fun exportReport(
        format: String, type: String?, month: Int?, year: Int?, search: String?,
        onReady: (ByteArray) -> Unit,
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                val response = if (format == "pdf") {
                    api.exportPdf(type, month, year, null, search)
                } else {
                    api.exportXlsx(type, month, year, null, search)
                }
                if (!response.isSuccessful) error("Exportación no disponible")
                val bytes = withContext(Dispatchers.IO) { response.body()?.bytes() }
                    ?: error("Archivo vacío")
                if (bytes.isEmpty()) error("Archivo vacío")
                onReady(bytes)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(error = "No se pudo generar el informe. Intenta de nuevo.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun sendChat(text: String) {
        val message = text.trim().take(4096)
        if (message.isEmpty()) return
        _state.update { it.copy(chat = it.chat + ChatLine(user = true, text = message)) }
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                val response = api.financialChat(FinancialChatRequest(message))
                if (!response.isSuccessful) error("El asesor no está disponible")
                val body = response.body()?.string().orEmpty()
                // Accept known FastAPI response shapes; never display raw JSON or tokens.
                val data = JSONObject(body.ifBlank { "{}" })
                val answer = listOf("respuesta", "response", "reply", "answer", "mensaje")
                    .map { data.optString(it) }
                    .firstOrNull { it.isNotBlank() } ?: "El asesor no devolvió una respuesta."
                _state.update { it.copy(chat = it.chat + ChatLine(user = false, text = answer)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(error = "El asesor no pudo responder. Intenta de nuevo.")
                }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
