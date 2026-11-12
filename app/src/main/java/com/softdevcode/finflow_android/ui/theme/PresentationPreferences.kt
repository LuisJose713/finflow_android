package com.softdevcode.finflow_android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class UiPreferencesResponse(val hiddenCards: List<String> = emptyList())
data class PresentationState(
    val enabled: Boolean = false,
    val hidden: Set<String> = emptySet(),
    val saving: Set<String> = emptySet(),
    val errors: Map<String, String> = emptyMap(),
)
data class PresentationControls(val state: PresentationState = PresentationState(), val setHidden: (String, Boolean) -> Unit = { _, _ -> })
val LocalPresentationControls = staticCompositionLocalOf { PresentationControls() }

@HiltViewModel
class UiPreferencesViewModel @Inject constructor(private val api: FinFlowApi) : ViewModel() {
    private val _state = MutableStateFlow(PresentationState())
    val state = _state.asStateFlow()
    private var userId: Int? = null
    private var loadJob: Job? = null
    private val edited = mutableMapOf<String, Boolean>()

    fun load(id: Int?) {
        if (id == userId) return
        userId = id; loadJob?.cancel(); edited.clear()
        _state.value = PresentationState(enabled = id != null)
        if (id == null) return
        loadJob = viewModelScope.launch {
            try {
                val remote = api.getUiPreferences()
                if (userId != id) return@launch
                val hidden = remote.hiddenCards.toMutableSet()
                edited.forEach { (card, hide) -> if (hide) hidden.add(card) else hidden.remove(card) }
                _state.update { it.copy(hidden = hidden) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { /* Headers remain usable while offline. */ }
        }
    }

    fun setHidden(card: String, hidden: Boolean) {
        val id = userId ?: return
        if (card in _state.value.saving) return
        val previous = card in _state.value.hidden
        fun apply(value: Boolean) {
            edited[card] = value
            _state.update { it.copy(hidden = if (value) it.hidden + card else it.hidden - card) }
        }
        apply(hidden)
        _state.update { it.copy(saving = it.saving + card, errors = it.errors - card) }
        viewModelScope.launch {
            try {
                val response = api.setCardVisibility(card, mapOf("hidden" to hidden))
                if (!response.isSuccessful) throw IllegalStateException("Preference not saved")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (userId == id) {
                    apply(previous)
                    _state.update { it.copy(errors = it.errors + (card to "No pudimos guardar este cambio. Reintenta.")) }
                }
            } finally { if (userId == id) _state.update { it.copy(saving = it.saving - card) } }
        }
    }
}
