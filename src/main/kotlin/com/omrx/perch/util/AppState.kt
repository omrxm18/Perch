package com.omrx.perch.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.MailSummary

data class AppState(
    val mailCache: MailCache = MailCache(),
    val selectedMailUid: Long? = null,
    val loadingContentUid: Long? = null,
    val contentError: AppError? = null
)

@Composable
fun rememberAppState(): MutableState<AppState> {
    return remember { mutableStateOf(AppState()) }
}

fun AppState.isLoadingContent(uid: Long?): Boolean = loadingContentUid == uid
