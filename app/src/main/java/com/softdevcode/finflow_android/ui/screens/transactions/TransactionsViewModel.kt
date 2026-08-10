
package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.Transaction
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
}
