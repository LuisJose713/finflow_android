package com.softdevcode.finflow_android.ui.screens.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingAdminState(
    val catalog: BillingCatalog? = null,
    val entitlement: BillingEntitlement? = null,
    val quote: BillingOffer? = null,
    val overview: AdminOverview? = null,
    val health: AdminPlatformHealth? = null,
    val plans: List<BillingPlan> = emptyList(),
    val promotions: List<BillingPromotion> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

/**
 * Loads pricing without any payment activation.
 * ADMIN-only requests are both UI-gated and server-authorized (ROLE_ADMIN).
 */
@HiltViewModel
class BillingAdminViewModel @Inject constructor(
    private val api: FinFlowApi,
) : ViewModel() {
    private val _state = MutableStateFlow(BillingAdminState())
    val state: StateFlow<BillingAdminState> = _state.asStateFlow()

    fun refreshBilling() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, quote = null) }
            try {
                val catalog = api.getBillingCatalog()
                // Some users may inspect prices before login; /billing/me needs auth.
                val entitlement = try { api.getMyBillingStatus() } catch (_: Exception) { null }
                _state.update { it.copy(catalog = catalog, entitlement = entitlement) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "No se pudieron cargar los planes.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun quote(planCode: String, promoCode: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, quote = null) }
            try {
                val result = api.getBillingQuote(planCode, promoCode.trim().ifBlank { null })
                _state.update { it.copy(quote = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "El código promocional es inválido o venció.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun refreshAdmin() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null) }
            try {
                // All four endpoints are guarded by the backend's ROLE_ADMIN rule.
                val overview = api.getAdminOverview()
                val health = api.getAdminHealth()
                val plans = api.getAdminPlans()
                val promotions = api.getAdminPromotions()
                _state.update {
                    it.copy(overview = overview, health = health, plans = plans,
                        promotions = promotions)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "No tienes autorización o el panel no está disponible.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun savePlan(plan: BillingPlan) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                api.updateAdminPlan(plan.code, plan)
                _state.update { it.copy(notice = "Plan actualizado correctamente.") }
                refreshAdmin()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "No se pudo actualizar el plan.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun savePromotion(promotion: BillingPromotion, isNew: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                if (isNew) api.createAdminPromotion(promotion)
                else api.updateAdminPromotion(promotion.code, promotion)
                _state.update { it.copy(notice = "Promoción actualizada.") }
                refreshAdmin()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "No se pudo guardar la promoción.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    fun grantPlan(request: BillingGrantRequest) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, error = null, notice = null) }
            try {
                val result = api.grantAdminPlan(request)
                if (!result.isSuccessful) error("Grant denied")
                _state.update { it.copy(notice = "Acceso asignado por el administrador.") }
                refreshAdmin()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(error = "No se pudo asignar acceso. Revisa usuario y permisos.") }
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }
}
