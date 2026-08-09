package com.softdevcode.finflow_android.util


sealed class Result<out T> {

    data class Success<out T>(
        val data: T
    ) : Result<T>()

    data class Error(
        val message: String
    ) : Result<Nothing>()

    data object Loading : Result<Nothing>()

    val isSuccess: Boolean
        get() = this is Success

    val isError: Boolean
        get() = this is Error

    val isLoading: Boolean
        get() = this is Loading

    fun getOrNull(): T? =
        if (this is Success) data else null
}

inline fun <T> Result<T>.onSuccess(
    action: (T) -> Unit
): Result<T> {
    if (this is Result.Success) {
        action(data)
    }
    return this
}

inline fun <T> Result<T>.onError(
    action: (String) -> Unit
): Result<T> {
    if (this is Result.Error) {
        action(message)
    }
    return this
}