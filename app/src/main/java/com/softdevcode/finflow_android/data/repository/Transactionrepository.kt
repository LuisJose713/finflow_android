package com.softdevcode.finflow_android.data.repository

import android.util.Log
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.*
import com.softdevcode.finflow_android.util.Result
import javax.inject.Inject
import javax.inject.Singleton

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
            Log.d("API", "getTransactions OK: ${it.size}")
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            Log.e("API", "getTransactions ERROR: ${it.message}")
            Result.Error(it.message ?: "Error al cargar transacciones")
        }
    )

    suspend fun getStats(): Result<TransactionStats> = runCatching {
        api.getStats()
    }.fold(
        onSuccess = {
            Log.d("API", "getStats OK: $it")
            if (it != null) Result.Success(it)
            else Result.Error("Stats vacías")
        },
        onFailure = {
            Log.e("API", "getStats ERROR: ${it.message}")
            Result.Error(it.message ?: "Error al cargar estadísticas")
        }
    )

    suspend fun getCategories(): Result<List<Category>> = runCatching {
        api.getCategories()
    }.fold(
        onSuccess = {
            Log.d("API", "getCategories OK: ${it.size}")
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            Log.e("API", "getCategories ERROR: ${it.message}")
            Result.Error(it.message ?: "Error al cargar categorías")
        }
    )

    suspend fun createTransaction(req: CreateTransactionRequest): Result<Transaction> = runCatching {
        api.createTransaction(req)
    }.fold(
        onSuccess = {
            Log.d("API", "createTransaction OK")
            Result.Success(it)
        },
        onFailure = {
            Log.e("API", "createTransaction ERROR: ${it.message}")
            Result.Error(it.message ?: "Error al crear transacción")
        }
    )

    suspend fun deleteTransaction(id: Int): Result<Unit> = runCatching {
        api.deleteTransaction(id)
    }.fold(
        onSuccess = {
            Log.d("API", "deleteTransaction OK")
            Result.Success(Unit)
        },
        onFailure = {
            Log.e("API", "deleteTransaction ERROR: ${it.message}")
            Result.Error(it.message ?: "Error al eliminar transacción")
        }
    )

    suspend fun getAvailableYears(): Result<List<Int>> = runCatching {
        api.getAvailableYears()
    }.fold(
        onSuccess = {
            Log.d("API", "getYears OK: ${it.size}")
            Result.Success(it ?: emptyList())
        },
        onFailure = {
            Log.e("API", "getYears ERROR: ${it.message}")
            Result.Error(it.message ?: "Error")
        }
    )
}