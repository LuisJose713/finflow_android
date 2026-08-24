package com.softdevcode.finflow_android.data.repository

import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import com.softdevcode.finflow_android.domain.model.Category
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepository @Inject constructor(
    private val api: FinFlowApi,
) {

    suspend fun getCategories(): List<Category> {
        return api.getCategories()
    }
}