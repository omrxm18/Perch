package com.omrx.perch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.omrx.perch.auth.getGmailCredential
import com.omrx.perch.auth.getUserEmail
import com.omrx.perch.config.AccountConfig
import com.omrx.perch.config.AccountType
import com.omrx.perch.logging.Logger
import com.omrx.perch.ui.component.ErrorDialog
import com.omrx.perch.ui.component.ImapWatcher
import com.omrx.perch.ui.component.JcefBootstrap
import com.omrx.perch.ui.component.setupTray
import com.omrx.perch.ui.component.updateUnreadCount
import com.omrx.perch.ui.component.AppState
import com.omrx.perch.ui.nav.NavHost
import com.omrx.perch.ui.nav.Navigator
import com.omrx.perch.ui.screens.LoginScreen
import com.omrx.perch.util.AppError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val isWindowVisible = mutableStateOf(true)
private val logger = Logger.get("Main")

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
        val windowState = rememberWindowState(
            size = DpSize(1100.dp, 750.dp),
            position = WindowPosition(Alignment.Center)
        )
        val appState = remember { AppState() }
        val navHost = remember { NavHost() }
        val tokenFile = File(System.getProperty("user.home"), ".config/perch/StoredTokens/StoredCredential")
        var loggedIn by remember { mutableStateOf(tokenFile.isFile && tokenFile.length() > 0) }

        LaunchedEffect(appState.unreadCount) {
            updateUnreadCount(appState.unreadCount)
        }

        LaunchedEffect(Unit) {
            JcefBootstrap.initialize(
                onProgress = { logger.info("JCEF initialization progress: $it%") },
                onReady = { appState.browserReady = true }
            )
        }

        LaunchedEffect(loggedIn) {
            if (!loggedIn) return@LaunchedEffect

            // Initialize primary Gmail account in background
            withContext(Dispatchers.IO) {
                try {
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
                        onNewMail = { newMessages -> appState.mailList.addAll(0, newMessages) }
                    )
                    watcher.connect()
                    appState.watcherRef = watcher
                    appState.mailList.addAll(watcher.fetchRecentSummaries())
                    watcher.startIdleLoop()
                } catch (e: Exception) {
                    logger.error("Failed to initialize account", e)
                    appState.currentError = AppError(
                        title = "Failed to Connect",
                        message = e.message ?: "Unable to connect to Gmail",
                        throwable = e
                    )
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                appState.watcherRef?.stop()
            }
        }

        Window(
            onCloseRequest = { isWindowVisible.value = false },
            state = windowState,
            visible = isWindowVisible.value,
            title = "Perch"
        ) {
            LaunchedEffect(isWindowVisible.value) {
                window.minimumSize = java.awt.Dimension(800, 500)
                if (isWindowVisible.value) {
                    windowState.isMinimized = false
                    window.toFront()
                    window.requestFocus()
                }
            }
            when {
                !loggedIn -> LoginScreen(onSuccess = { loggedIn = true })
                !appState.browserReady -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                else -> Navigator(
                    appState = appState,
                    navHost = navHost
                )
            }

            ErrorDialog(
                error = appState.currentError,
                onDismiss = { appState.currentError = null }
            )
        }
    }
}