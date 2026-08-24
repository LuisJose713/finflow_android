package com.softdevcode.finflow_android.data.repository

import android.util.Log
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.PlannedExpense
import com.softdevcode.finflow_android.domain.model.PlannedExpenseRequest
import com.softdevcode.finflow_android.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlannedExpenseRepository @Inject constructor(
    private val api: FinFlowApi,
) {

    suspend fun getPlannedExpenses(): Result<List<PlannedExpense>> =
        runCatching {
            api.getPlannedExpenses()
        }.fold(
            onSuccess = {
                Log.d("PLANNED_EXPENSES", "get OK: ${it.size}")
                Result.Success(it)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "get ERROR: ${it.message}", it)
                Result.Error(
                    it.message ?: "Error al cargar los gastos planeados"
                )
            }
        )

    suspend fun createPlannedExpense(
        request: PlannedExpenseRequest,
    ): Result<PlannedExpense> =
        runCatching {
            api.createPlannedExpense(request)
        }.fold(
            onSuccess = {
                Log.d("PLANNED_EXPENSES", "create OK: ${it.id}")
                Result.Success(it)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "create ERROR: ${it.message}", it)
                Result.Error(
                    it.message ?: "Error al crear el gasto planeado"
                )
            }
        )

    suspend fun updatePlannedExpense(
        id: Int,
        request: PlannedExpenseRequest,
    ): Result<PlannedExpense> =
        runCatching {
            api.updatePlannedExpense(id, request)
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "update ERROR", it)
                Result.Error(
                    it.message ?: "Error al actualizar el gasto planeado"
                )
            }
        )

    suspend fun executePlannedExpense(
        id: Int,
    ): Result<PlannedExpense> =
        runCatching {
            api.executePlannedExpense(id)
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "execute ERROR", it)
                Result.Error(
                    it.message ?: "Error al ejecutar el gasto planeado"
                )
            }
        )

    suspend fun cancelPlannedExpense(
        id: Int,
    ): Result<PlannedExpense> =
        runCatching {
            api.cancelPlannedExpense(id)
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "cancel ERROR", it)
                Result.Error(
                    it.message ?: "Error al cancelar el gasto planeado"
                )
            }
        )

    suspend fun deletePlannedExpense(
        id: Int,
    ): Result<Unit> =
        runCatching {
            api.deletePlannedExpense(id)
        }.fold(
            onSuccess = {
                Result.Success(Unit)
            },
            onFailure = {
                Log.e("PLANNED_EXPENSES", "delete ERROR", it)
                Result.Error(
                    it.message ?: "Error al eliminar el gasto planeado"
                )
            }
        )
}