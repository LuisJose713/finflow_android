package com.softdevcode.finflow_android.data.repository

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.util.Result
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException

@Singleton
class TransactionRepository @Inject constructor(
    private val api: FinFlowApi,
) {

    suspend fun getTransactions(
        type: String? = null,
        month: Int? = null,
        year: Int? = null,
        categoryId: Int? = null,
        search: String? = null,
    ): Result<List<Transaction>> = runCatching {
        api.getTransactions(type, month, year, categoryId, search)
    }.fold(
        onSuccess = {
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            if (it is CancellationException) throw it
            Result.Error(it.message ?: "Error al cargar transacciones")
        }
    )

    suspend fun getStats(): Result<TransactionStats> = runCatching {
        api.getStats()
    }.fold(
        onSuccess = {
            if (it != null) Result.Success(it)
            else Result.Error("Stats vacías")
        },
        onFailure = {
            if (it is CancellationException) throw it
            Result.Error(it.message ?: "Error al cargar estadísticas")
        }
    )

    suspend fun getCategories(): Result<List<Category>> = runCatching {
        api.getCategories()
    }.fold(
        onSuccess = {
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            if (it is CancellationException) throw it
            Result.Error(it.message ?: "Error al cargar categorías")
        }
    )

    suspend fun createTransaction(req: CreateTransactionRequest): Result<Transaction> = runCatching {
        api.createTransaction(req)
    }.fold(
        onSuccess = {
            Result.Success(it)
        },
        onFailure = {
            Result.Error(it.message ?: "Error al crear transacción")
        }
    )

    suspend fun updateTransaction(id: Int, req: CreateTransactionRequest): Result<Transaction> =
        runCatching { api.updateTransaction(id, req) }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error("No fue posible editar la transacción") }
        )

    suspend fun deleteTransaction(id: Int): Result<Unit> = runCatching {
        api.deleteTransaction(id)
    }.fold(
        onSuccess = {
            Result.Success(Unit)
        },
        onFailure = {
            Result.Error(it.message ?: "Error al eliminar transacción")
        }
    )

    suspend fun getAvailableYears(): Result<List<Int>> = runCatching {
        api.getAvailableYears()
    }.fold(
        onSuccess = {
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            Result.Error(it.message ?: "Error")
        }
    )
}
