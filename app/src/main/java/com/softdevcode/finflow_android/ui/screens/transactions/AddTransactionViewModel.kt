package com.softdevcode.finflow_android.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.CategoryRepository
import com.softdevcode.finflow_android.data.repository.TransactionRepository
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.util.onError
import com.softdevcode.finflow_android.util.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(

    val isLoading: Boolean = false,

    val isLoadingCategories: Boolean = false,

    val categories: List<Category> = emptyList(),

    val error: String? = null,

    val saved: Boolean = false,
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(

    private val transactionRepository: TransactionRepository,

    private val categoryRepository: CategoryRepository,

    ) : ViewModel() {

    private val _uiState =
        MutableStateFlow(
            AddTransactionUiState()
        )

    val uiState: StateFlow<AddTransactionUiState> =
        _uiState.asStateFlow()

    init {
        loadCategories()
    }

    // ─────────────────────────────────────────────────────────────
    // CATEGORÍAS
    // ─────────────────────────────────────────────────────────────

    private fun loadCategories() {

        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoadingCategories = true,
                    error = null,
                )
            }

            try {

                val categories =
                    categoryRepository.getCategories()

                _uiState.update {

                    it.copy(
                        isLoadingCategories = false,
                        categories = categories,
                    )
                }

            } catch (e: Exception) {

                _uiState.update {

                    it.copy(
                        isLoadingCategories = false,
                        error =
                            e.message
                                ?: "No se pudieron cargar las categorías",
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CREAR TRANSACCIÓN
    // ─────────────────────────────────────────────────────────────

    fun createTransaction(
        request: CreateTransactionRequest
    ) {

        viewModelScope.launch {

            _uiState.update {

                it.copy(
                    isLoading = true,
                    error = null,
                    saved = false,
                )
            }

            transactionRepository
                .createTransaction(request)

                .onSuccess {

                    _uiState.update {

                        it.copy(
                            isLoading = false,
                            saved = true,
                        )
                    }
                }

                .onError { message ->

                    _uiState.update {

                        it.copy(
                            isLoading = false,
                            error = message,
                            saved = false,
                        )
                    }
                }
        }
    }

    fun clearError() {

        _uiState.update {

            it.copy(
                error = null
            )
        }
    }
}