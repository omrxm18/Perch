package com.omrx.perch.util

data class AppError(
    val title: String,
    val message: String,
    val throwable: Throwable? = null,
    val recoverable: Boolean = true
)