package com.softdevcode.finflow_android.data.repository

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.ContributionRequest
import com.softdevcode.finflow_android.domain.model.Goal
import com.softdevcode.finflow_android.domain.model.GoalRequest
import com.softdevcode.finflow_android.util.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val api: FinFlowApi,
) {

    suspend fun getGoals(): Result<List<Goal>> =
        runCatching {
            api.getGoals()
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Result.Error(
                    it.message ?: "Error al cargar las metas"
                )
            }
        )

    suspend fun createGoal(
        request: GoalRequest,
    ): Result<Goal> =
        runCatching {
            api.createGoal(request)
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Result.Error(
                    it.message ?: "Error al crear la meta"
                )
            }
        )

    suspend fun updateGoal(
        id: Int,
        request: GoalRequest,
    ): Result<Goal> =
        runCatching {
            api.updateGoal(id, request)
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Result.Error(
                    it.message ?: "Error al actualizar la meta"
                )
            }
        )

    suspend fun addContribution(
        goalId: Int,
        amount: Double,
        note: String?,
    ): Result<Goal> =
        runCatching {
            api.addContribution(
                id = goalId,
                request = ContributionRequest(
                    amount = amount,
                    note = note,
                )
            )
        }.fold(
            onSuccess = {
                Result.Success(it)
            },
            onFailure = {
                Result.Error(
                    it.message ?: "Error al agregar dinero"
                )
            }
        )

    suspend fun deleteGoal(
        id: Int,
    ): Result<Unit> =
        runCatching {
            api.deleteGoal(id)
        }.fold(
            onSuccess = {
                Result.Success(Unit)
            },
            onFailure = {
                Result.Error(
                    it.message ?: "Error al eliminar la meta"
                )
            }
        )
}