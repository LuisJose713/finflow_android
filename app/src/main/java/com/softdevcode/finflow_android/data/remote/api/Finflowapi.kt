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

import com.softdevcode.finflow_android.domain.model.FinancialAccount
import com.softdevcode.finflow_android.domain.model.FinancialAccountRequest
import com.softdevcode.finflow_android.domain.model.FinancialAccountUpdate
import com.softdevcode.finflow_android.domain.model.ArchiveAccountRequest
import com.softdevcode.finflow_android.domain.model.AccountTransfer
import com.softdevcode.finflow_android.domain.model.AccountTransferRequest
import com.softdevcode.finflow_android.domain.model.MonthlyBudget
import com.softdevcode.finflow_android.domain.model.MonthlyBudgetRequest
import com.softdevcode.finflow_android.domain.model.BudgetAmountRequest
import com.softdevcode.finflow_android.domain.model.RecurringRule
import com.softdevcode.finflow_android.domain.model.RecurringRuleRequest
import com.softdevcode.finflow_android.domain.model.ToggleRecurringRequest
import com.softdevcode.finflow_android.domain.model.RecurringExecutionResult
import com.softdevcode.finflow_android.domain.model.CategoryPayload
import com.softdevcode.finflow_android.domain.model.FinancialChatRequest

import com.softdevcode.finflow_android.domain.model.FinFlowPrivacyPolicy
import com.softdevcode.finflow_android.domain.model.FinFlowPrivacyStatus
import com.softdevcode.finflow_android.domain.model.FinFlowMarketingChoice
import com.softdevcode.finflow_android.domain.model.FinFlowRightsRequest
import com.softdevcode.finflow_android.domain.model.FinFlowRightsEntry

interface FinFlowApi {
    // Consent is NEVER assumed; these endpoints only read and update
    // explicit choices made by the authenticated account owner.
    @GET("privacy/policy")
    suspend fun getPrivacyPolicy(): FinFlowPrivacyPolicy

    @GET("privacy/status")
    suspend fun getPrivacyStatus(): FinFlowPrivacyStatus

    @POST("privacy/marketing")
    suspend fun setMarketing(@Body choice: FinFlowMarketingChoice): retrofit2.Response<okhttp3.ResponseBody>

    @GET("privacy/rights-requests")
    suspend fun getPrivacyRequests(): List<FinFlowRightsEntry>

    @POST("privacy/rights-request")
    suspend fun createPrivacyRequest(@Body request: FinFlowRightsRequest): retrofit2.Response<okhttp3.ResponseBody>


    // Accounts and transfers: owned by the authenticated user on the server.
    @GET("accounts")
    suspend fun getAccounts(): List<FinancialAccount>

    @POST("accounts")
    suspend fun createAccount(@Body input: FinancialAccountRequest): FinancialAccount

    @PUT("accounts/{id}")
    suspend fun updateAccount(@Path("id") id: Int, @Body input: FinancialAccountUpdate): FinancialAccount

    @PATCH("accounts/{id}/archive")
    suspend fun archiveAccount(@Path("id") id: Int, @Body input: ArchiveAccountRequest): FinancialAccount

    @GET("accounts/transfers")
    suspend fun getTransfers(): List<AccountTransfer>

    @POST("accounts/transfers")
    suspend fun createTransfer(@Body input: AccountTransferRequest): AccountTransfer

    // Monthly budgets.
    @GET("budgets")
    suspend fun getBudgets(@Query("year") year: Int, @Query("month") month: Int): List<MonthlyBudget>

    @POST("budgets")
    suspend fun createBudget(@Body input: MonthlyBudgetRequest): retrofit2.Response<okhttp3.ResponseBody>

    @PUT("budgets/{id}")
    suspend fun updateBudget(@Path("id") id: Int, @Body input: BudgetAmountRequest): retrofit2.Response<okhttp3.ResponseBody>

    @DELETE("budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: Int): retrofit2.Response<Void>

    // Recurring rules.
    @GET("recurring")
    suspend fun getRecurringRules(): List<RecurringRule>

    @POST("recurring")
    suspend fun createRecurringRule(@Body input: RecurringRuleRequest): RecurringRule

    @PATCH("recurring/{id}/active")
    suspend fun setRecurringActive(@Path("id") id: Int, @Body input: ToggleRecurringRequest): RecurringRule

    @DELETE("recurring/{id}")
    suspend fun deleteRecurringRule(@Path("id") id: Int): retrofit2.Response<Void>

    @POST("recurring/process-due")
    suspend fun processRecurringDue(@Body empty: Map<String, String> = emptyMap()): RecurringExecutionResult

    // Owned custom categories.
    @POST("categories")
    suspend fun createCategory(@Body input: CategoryPayload): Category

    @PUT("categories/{id}")
    suspend fun updateCategory(@Path("id") id: Int, @Body input: CategoryPayload): Category

    @DELETE("categories/{id}")
    suspend fun deleteCategory(@Path("id") id: Int): retrofit2.Response<Void>

    // FinFlow AI: real route defined by the production /ai controller.
    @POST("ai/chat")
    suspend fun financialChat(@Body input: FinancialChatRequest): retrofit2.Response<okhttp3.ResponseBody>

    // PDF and Excel: authentic output from the same backend as FinFlow Web.
    @GET("transactions/export/xlsx")
    suspend fun exportXlsx(
        @Query("type") type: String? = null,
        @Query("month") month: Int? = null,
        @Query("year") year: Int? = null,
        @Query("categoryId") categoryId: Int? = null,
        @Query("search") search: String? = null,
    ): retrofit2.Response<okhttp3.ResponseBody>

    @PUT("transactions/{id}")
    suspend fun updateTransaction(@Path("id") id: Int, @Body input: CreateTransactionRequest): Transaction



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
        @Query("search") search: String? = null,
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