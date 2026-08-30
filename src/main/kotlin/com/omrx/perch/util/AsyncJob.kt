package com.omrx.perch.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class AsyncJob<T>(
    val isLoading: MutableState<Boolean> = mutableStateOf(false),
    val error: MutableState<AppError?> = mutableStateOf(null),
    val data: MutableState<T?> = mutableStateOf(null),
    private var currentJob: Job? = null
) {
    fun launch(scope: CoroutineScope, block: suspend () -> T) {
        currentJob?.cancel()
        currentJob = scope.launch {
            isLoading.value = true
            error.value = null
            try {
                data.value = block()
            } catch (e: CancellationException) {
                // Job was cancelled, don't update state
                throw e
            } catch (e: Exception) {
                error.value = AppError(
                    title = "Operation Failed",
                    message = e.message ?: "Unknown error",
                    throwable = e
                )
            } finally {
                isLoading.value = false
            }
        }
    }

    fun cancel() {
        currentJob?.cancel()
        currentJob = null
    }
}

@Composable
fun <T> rememberAsyncJob(): AsyncJob<T> {
    return remember { AsyncJob() }
}
