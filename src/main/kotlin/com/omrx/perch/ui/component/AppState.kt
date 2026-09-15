package com.omrx.perch.ui.component

import androidx.compose.runtime.*
import com.omrx.perch.config.AccountConfig
import com.omrx.perch.ui.nav.Screen
import com.omrx.perch.util.AppError

class AppState {
    var watcherRef by mutableStateOf<ImapWatcher?>(null)
    var browserReady by mutableStateOf(false)
    var accounts by mutableStateOf<List<AccountConfig>>(emptyList())
    var currentScreen by mutableStateOf(Screen.Inbox)
    var currentError by mutableStateOf<AppError?>(null)
    var isInitializingAccounts by mutableStateOf(false)
    val mailList = mutableStateListOf<MailSummary>()
    val unreadCount by derivedStateOf { mailList.count { !it.isRead } }

}