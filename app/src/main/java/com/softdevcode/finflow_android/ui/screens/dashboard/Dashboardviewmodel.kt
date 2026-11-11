package com.softdevcode.finflow_android.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        // Initial composition and ON_RESUME can arrive together. Keep one request in flight.
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val (statsResult, txResult, catsResult) = coroutineScope {

                    val statsDeferred = async(Dispatchers.IO) {
                        transactionRepository.getStats()
                    }

                    val txDeferred = async(Dispatchers.IO) {
                        transactionRepository.getTransactions()
                    }

                    val catsDeferred = async(Dispatchers.IO) {
                        transactionRepository.getCategories()
                    }

                    Triple(
                        statsDeferred.await(),
                        txDeferred.await(),
                        catsDeferred.await()
                    )
                }

                ensureActive()
                _uiState.update { state ->
                    state.withResults(statsResult, txResult, catsResult, System.currentTimeMillis())
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Revisa tu conexión y vuelve a intentarlo."
                    )
                }
            }
        }
    }

    fun refresh() = load()

    // 🔥 Optimizado (evita buscar en lista cada vez)
    private val categoryMap: Map<Int, String>
        get() = _uiState.value.categories.associate { it.id to it.name }

    fun categoryName(categoryId: Int?): String {
        return categoryId?.let { categoryMap[it] } ?: "Otro"
    }
}
