package com.omrx.perch.util

data class AppError(
    val title: String,
    val message: String,
    val throwable: Throwable? = null,
    val recoverable: Boolean = true
)

sealed class Result<T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error<T>(val error: AppError) : Result<T>()
}

suspend inline fun <T> tryAsync(
    errorTitle: String = "Error",
    crossinline block: suspend () -> T
): Result<T> {
    return try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(AppError(
            title = errorTitle,
            message = e.message ?: "Unknown error occurred",
            throwable = e
        ))
    }
}
