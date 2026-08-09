package com.softdevcode.finflow_android.data.remote.api


import com.softdevcode.finflow_android.domain.model.AuthResponse
import com.softdevcode.finflow_android.domain.model.Category
import com.softdevcode.finflow_android.domain.model.ContributionRequest
import com.softdevcode.finflow_android.domain.model.CreateTransactionRequest
import com.softdevcode.finflow_android.domain.model.Goal
import com.softdevcode.finflow_android.domain.model.GoalRequest
import com.softdevcode.finflow_android.domain.model.LoginRequest
import com.softdevcode.finflow_android.domain.model.PlannedExpense
import com.softdevcode.finflow_android.domain.model.PlannedExpenseRequest
import com.softdevcode.finflow_android.domain.model.RegisterRequest
import com.softdevcode.finflow_android.domain.model.Transaction
import com.softdevcode.finflow_android.domain.model.TransactionStats
import com.softdevcode.finflow_android.domain.model.User
import retrofit2.http.*

interface FinFlowApi {

    // ── Auth ──────────────────────────────────────────────────────────────────
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("auth/me")
    suspend fun getMe(): User

    // ── Transactions ──────────────────────────────────────────────────────────
    @GET("transactions")
    suspend fun getTransactions(
        @Query("type") type: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("search") search: String? = null,
    ): List<Transaction>

    @GET("transactions/stats")
    suspend fun getStats(): TransactionStats

    @GET("transactions/years")
    suspend fun getAvailableYears(): List<Int>

    @POST("transactions")
    suspend fun createTransaction(@Body request: CreateTransactionRequest): Transaction

    @DELETE("transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: Int)

    @GET("transactions/export/csv")
    suspend fun exportCsv(
        @Query("type") type: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("categoryId") categoryId: Int? = null,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @GET("transactions/export/pdf")
    suspend fun exportPdf(
        @Query("type") type: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("categoryId") categoryId: Int? = null,
    ): retrofit2.Response<okhttp3.ResponseBody>

    // ── Categories ────────────────────────────────────────────────────────────
    @GET("categories")
    suspend fun getCategories(): List<Category>

    // ── Goals ─────────────────────────────────────────────────────────────────
    @GET("goals")
    suspend fun getGoals(): List<Goal>

    @POST("goals")
    suspend fun createGoal(@Body request: GoalRequest): Goal

    @PUT("goals/{id}")
    suspend fun updateGoal(@Path("id") id: Int, @Body request: GoalRequest): Goal

    @PATCH("goals/{id}/contribute")
    suspend fun addContribution(
        @Path("id") id: Int,
        @Body request: ContributionRequest
    ): Goal

    @DELETE("goals/{id}")
    suspend fun deleteGoal(@Path("id") id: Int)

    // ── Planned Expenses ──────────────────────────────────────────────────────
    @GET("planned-expenses")
    suspend fun getPlannedExpenses(): List<PlannedExpense>

    @POST("planned-expenses")
    suspend fun createPlannedExpense(@Body request: PlannedExpenseRequest): PlannedExpense

    @PUT("planned-expenses/{id}")
    suspend fun updatePlannedExpense(
        @Path("id") id: Int,
        @Body request: PlannedExpenseRequest
    ): PlannedExpense

    @PATCH("planned-expenses/{id}/execute")
    suspend fun executePlannedExpense(@Path("id") id: Int): PlannedExpense

    @PATCH("planned-expenses/{id}/cancel")
    suspend fun cancelPlannedExpense(@Path("id") id: Int): PlannedExpense

    @DELETE("planned-expenses/{id}")
    suspend fun deletePlannedExpense(@Path("id") id: Int)
}