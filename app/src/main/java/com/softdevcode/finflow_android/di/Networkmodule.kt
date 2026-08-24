package com.softdevcode.finflow_android.di

import com.softdevcode.finflow_android.data.local.TokenManager
import com.softdevcode.finflow_android.data.remote.api.FinFlowApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        tokenManager: TokenManager
    ): Interceptor =
        Interceptor { chain ->

            val token = runBlocking {
                tokenManager.getTokenOnce()
            }

            val request = chain.request()
                .newBuilder()
                .apply {
                    if (token != null) {
                        addHeader("Authorization", "Bearer $token")
                        addHeader("X-Client-Type", "mobile")
                    }
                }
                .build()

            chain.proceed(request)
        }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: Interceptor
    ): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl("http://192.168.101.11:8081")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideFinFlowApi(
        retrofit: Retrofit
    ): FinFlowApi =
        retrofit.create(FinFlowApi::class.java)
}