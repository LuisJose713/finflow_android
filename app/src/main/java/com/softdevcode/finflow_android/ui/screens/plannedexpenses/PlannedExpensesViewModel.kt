package com.softdevcode.finflow_android.ui.screens.plannedexpenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.PlannedExpenseRepository
import com.softdevcode.finflow_android.domain.model.PlannedExpense
import com.softdevcode.finflow_android.domain.model.PlannedExpenseRequest
import com.softdevcode.finflow_android.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlannedExpensesUiState(
    val isLoading: Boolean = false,
    val expenses: List<PlannedExpense> = emptyList(),
    val error: String? = null,

    val operationLoading: Boolean = false,
    val operationSuccess: Boolean = false,
    val operationError: String? = null,
)

@HiltViewModel
class PlannedExpensesViewModel @Inject constructor(
    private val repository: PlannedExpenseRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PlannedExpensesUiState()
    )

    val uiState: StateFlow<PlannedExpensesUiState> =
        _uiState.asStateFlow()

    init {
        loadPlannedExpenses()
    }

    // ============================================================
    // CARGAR
    // ============================================================

    fun loadPlannedExpenses() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            when (
                val result = repository.getPlannedExpenses()
            ) {

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            expenses = result.data,
                            error = null
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // CREAR
    // ============================================================

    fun createPlannedExpense(
        request: PlannedExpenseRequest
    ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (
                val result =
                    repository.createPlannedExpense(request)
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,
                            expenses =
                                listOf(result.data) +
                                        state.expenses
                        )
                    }
                }

                is Result.Error -> {

                    _uiState.update {
                        it.copy(
                            operationLoading = false,
                            operationSuccess = false,
                            operationError = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            operationLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // ACTUALIZAR
    // ============================================================

    fun updatePlannedExpense(
        id: Int,
        request: PlannedExpenseRequest
    ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (
                val result =
                    repository.updatePlannedExpense(
                        id = id,
                        request = request
                    )
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,
                            expenses =
                                state.expenses.map { expense ->

                                    if (expense.id == result.data.id) {
                                        result.data
                                    } else {
                                        expense
                                    }
                                }
                        )
                    }
                }

                is Result.Error -> {

                    _uiState.update {
                        it.copy(
                            operationLoading = false,
                            operationSuccess = false,
                            operationError = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            operationLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // EJECUTAR
    // ============================================================

    fun executePlannedExpense(
        id: Int
    ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (
                val result =
                    repository.executePlannedExpense(id)
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,

                            expenses =
                                state.expenses.map { expense ->

                                    if (expense.id == result.data.id) {
                                        result.data
                                    } else {
                                        expense
                                    }
                                }
                        )
                    }
                }

                is Result.Error -> {

                    _uiState.update {
                        it.copy(
                            operationLoading = false,
                            operationSuccess = false,
                            operationError = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            operationLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // CANCELAR
    // ============================================================

    fun cancelPlannedExpense(
        id: Int
    ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (
                val result =
                    repository.cancelPlannedExpense(id)
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,

                            expenses =
                                state.expenses.map { expense ->

                                    if (expense.id == result.data.id) {
                                        result.data
                                    } else {
                                        expense
                                    }
                                }
                        )
                    }
                }

                is Result.Error -> {

                    _uiState.update {
                        it.copy(
                            operationLoading = false,
                            operationSuccess = false,
                            operationError = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            operationLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // ELIMINAR
    // ============================================================

    fun deletePlannedExpense(
        id: Int
    ) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (
                val result =
                    repository.deletePlannedExpense(id)
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,

                            expenses =
                                state.expenses.filter {
                                    it.id != id
                                }
                        )
                    }
                }

                is Result.Error -> {

                    _uiState.update {
                        it.copy(
                            operationLoading = false,
                            operationSuccess = false,
                            operationError = result.message
                        )
                    }
                }

                else -> {
                    _uiState.update {
                        it.copy(
                            operationLoading = false
                        )
                    }
                }
            }
        }
    }

    // ============================================================
    // LIMPIAR ESTADO
    // ============================================================

    fun clearOperationState() {
        _uiState.update {
            it.copy(
                operationLoading = false,
                operationSuccess = false,
                operationError = null
            )
        }
    }

    fun clearError() {
        _uiState.update {
            it.copy(
                error = null,
                operationError = null
            )
        }
    }
}