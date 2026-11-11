
package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val isLoading: Boolean = true,
    val transactions: List<Transaction> = emptyList(),
    val error: String? = null,
    val isSaving: Boolean = false,
    val categories: List<Category> = emptyList(),
    val notice: String? = null,
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())

    val uiState: StateFlow<TransactionsUiState> =
        _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            val result = transactionRepository.getCategories()
            if (result is Result.Success) {
                _uiState.update { it.copy(categories = result.data) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                )
            }

            when (val result = transactionRepository.getTransactions()) {

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transactions = result.data,
                            error = null,
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message,
                        )
                    }
                }

                else -> {}
            }
        }
    }

    fun updateTransaction(id: Int, request: CreateTransactionRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            when (val result = transactionRepository.updateTransaction(id, request)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, notice = "Movimiento actualizado") }
                    load()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false,
                            error = result.message.ifBlank { "No se pudo actualizar" })
                    }
                }
                else -> {}
            }
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, notice = null) }
            when (val result = transactionRepository.deleteTransaction(id)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isSaving = false, notice = "Movimiento eliminado") }
                    load()
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(isSaving = false,
                            error = result.message.ifBlank { "No se pudo eliminar" })
                    }
                }
                else -> {}
            }
        }
    }
}
