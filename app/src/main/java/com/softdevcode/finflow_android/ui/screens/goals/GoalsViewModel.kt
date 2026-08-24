package com.softdevcode.finflow_android.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softdevcode.finflow_android.data.repository.GoalRepository
import com.softdevcode.finflow_android.domain.model.Goal
import com.softdevcode.finflow_android.domain.model.GoalRequest
import com.softdevcode.finflow_android.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GoalsUiState(
    val isLoading: Boolean = false,
    val goals: List<Goal> = emptyList(),
    val error: String? = null,

    val operationLoading: Boolean = false,
    val operationSuccess: Boolean = false,
    val operationError: String? = null,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val repository: GoalRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GoalsUiState())

    val uiState: StateFlow<GoalsUiState> =
        _uiState.asStateFlow()

    init {
        loadGoals()
    }

    // ============================================================
    // CARGAR METAS
    // ============================================================

    fun loadGoals() {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null
                )
            }

            when (val result = repository.getGoals()) {

                is Result.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            goals = result.data,
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
    // CREAR META
    // ============================================================

    fun createGoal(request: GoalRequest) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            try {

                when (val result = repository.createGoal(request)) {

                    is Result.Success -> {

                        _uiState.update { state ->
                            state.copy(
                                operationLoading = false,
                                operationSuccess = true,
                                operationError = null,
                                goals = listOf(result.data) + state.goals
                            )
                        }
                    }

                    is Result.Error -> {

                        _uiState.update {
                            it.copy(
                                operationLoading = false,
                                operationSuccess = false,
                                operationError =
                                    result.message
                                        ?: "No se pudo crear la meta"
                            )
                        }
                    }

                    else -> {

                        _uiState.update {
                            it.copy(
                                operationLoading = false,
                                operationSuccess = false,
                                operationError =
                                    "Respuesta inesperada al crear la meta"
                            )
                        }
                    }
                }

            } catch (e: Exception) {

                _uiState.update {
                    it.copy(
                        operationLoading = false,
                        operationSuccess = false,
                        operationError =
                            e.message
                                ?: "Error inesperado al crear la meta"
                    )
                }
            }
        }
    }

    // ============================================================
    // AGREGAR DINERO
    // ============================================================

    fun addContribution(
        goalId: Int,
        amount: Double,
        note: String?
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
                val result = repository.addContribution(
                    goalId = goalId,
                    amount = amount,
                    note = note
                )
            ) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,

                            goals = state.goals.map { goal ->

                                if (goal.id == result.data.id) {
                                    result.data
                                } else {
                                    goal
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
    // ACTUALIZAR META
    // ============================================================

    fun updateGoal(
        id: Int,
        request: GoalRequest
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
                val result = repository.updateGoal(
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

                            goals = state.goals.map { goal ->

                                if (goal.id == result.data.id) {
                                    result.data
                                } else {
                                    goal
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
    // ELIMINAR META
    // ============================================================

    fun deleteGoal(goalId: Int) {
        viewModelScope.launch {

            _uiState.update {
                it.copy(
                    operationLoading = true,
                    operationSuccess = false,
                    operationError = null
                )
            }

            when (val result = repository.deleteGoal(goalId)) {

                is Result.Success -> {

                    _uiState.update { state ->

                        state.copy(
                            operationLoading = false,
                            operationSuccess = true,
                            operationError = null,

                            goals = state.goals.filter {
                                it.id != goalId
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
    // LIMPIAR ESTADO DE OPERACIÓN
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

    // ============================================================
    // LIMPIAR ERROR
    // ============================================================

    fun clearError() {
        _uiState.update {
            it.copy(
                error = null,
                operationError = null
            )
        }
    }
}