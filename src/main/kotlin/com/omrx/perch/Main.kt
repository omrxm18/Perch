package com.omrx.perch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.*
import com.omrx.perch.auth.getGmailCredential
import com.omrx.perch.auth.getUserEmail
import com.omrx.perch.config.AccountConfig
import com.omrx.perch.config.AccountType
import com.omrx.perch.ui.screens.AccountsScreen
import com.omrx.perch.PerchApp
import com.omrx.perch.ui.component.ImapWatcher
import com.omrx.perch.ui.component.JcefBootstrap
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.MailSummary
import com.omrx.perch.ui.component.setupTray
import com.omrx.perch.ui.component.updateUnreadCount

private val isWindowVisible = mutableStateOf(true)

enum class Screen { Inbox, Accounts }

fun main() {
    setupTray(
        onShow = { isWindowVisible.value = true },
        onHide = { isWindowVisible.value = false },
        onQuit = {
            JcefBootstrap.dispose()
            kotlin.system.exitProcess(0)
        }
    )

    application {
        var watcherRef by remember { mutableStateOf<ImapWatcher?>(null) }
        val windowState = rememberWindowState()
        var browserReady by remember { mutableStateOf(false) }
        var accounts by remember { mutableStateOf<List<AccountConfig>>(emptyList()) }
        var currentScreen by remember { mutableStateOf(Screen.Inbox) }
        val mailList = remember { mutableStateListOf<MailSummary>() }
        val unreadCount by remember { derivedStateOf { mailList.count { !it.isRead } } }

        LaunchedEffect(unreadCount) {
            updateUnreadCount(unreadCount)
        }

        LaunchedEffect(Unit) {
            JcefBootstrap.initialize(
                onProgress = { },
                onReady = { browserReady = true }
            )
            Thread {
                val credential = getGmailCredential()
                credential.refreshToken()
                val email = getUserEmail(credential)
                val account = AccountConfig(
                    id = email,
                    displayName = email,
                    email = email,
                    type = AccountType.GMAIL,
                    imapHost = "imap.gmail.com"

                )
                val watcher = ImapWatcher(
                    accountConfig = account,
                    accessToken = credential.accessToken,
                    onNewMail = { newMessages -> mailList.addAll(0, newMessages) }
                )
                watcher.connect()
                watcherRef = watcher
                mailList.addAll(watcher.fetchRecentSummaries())
                watcher.startIdleLoop()
            }.start()
        }
        DisposableEffect(Unit) {
            onDispose {
                watcherRef?.stop()
            }
        }

        Window(
            onCloseRequest = { isWindowVisible.value = false },
            state = windowState,
            visible = isWindowVisible.value,
            title = "Perch"
        ) {
            LaunchedEffect(isWindowVisible.value) {
                if (isWindowVisible.value) {
                    windowState.isMinimized = false
                    window.toFront()
                    window.requestFocus()
                }
            }

            if (!browserReady) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {

                    when (currentScreen) {
                        Screen.Inbox -> PerchApp(
                            mailList,
                            fetchBody = { uid ->
                                val index = mailList.indexOfFirst { it.uid == uid }
                                if (index >= 0) mailList[index] =
                                    mailList[index].copy(isRead = true)
                                watcherRef?.markAsRead(uid)
                                watcherRef?.fetchBody(uid) ?: MailContent.Empty
                            },
                            onOpenAccounts = { currentScreen = Screen.Accounts }
                        )

                        Screen.Accounts -> AccountsScreen(
                            accounts = accounts,
                            onAddAccount = {
                                Thread {
                                    val credential = getGmailCredential()
                                    credential.refreshToken()
                                    val email = getUserEmail(credential)
                                    accounts += AccountConfig(
                                        id = email, displayName = email, email = email,
                                        type = AccountType.GMAIL, imapHost = "imap.gmail.com"
                                    )
                                }.start()
                            },
                            onRemoveAccount = { id -> accounts = accounts.filter { it.id != id } },
                            onBack = { currentScreen = Screen.Inbox }
                        )
                    }
                }
            }
        }
    }