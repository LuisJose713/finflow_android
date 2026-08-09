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

data class DashboardUiState(
    val isLoading: Boolean = true,
    val stats: TransactionStats? = null,
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val error: String? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
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

                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        stats = statsResult.getOrNull(),
                        recentTransactions = (txResult.getOrNull() ?: emptyList()).take(5),
                        categories = catsResult.getOrNull() ?: emptyList(),
                        error = when {
                            statsResult is Result.Error -> statsResult.message
                            txResult is Result.Error -> txResult.message
                            catsResult is Result.Error -> catsResult.message
                            else -> null
                        }
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Error cargando dashboard"
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