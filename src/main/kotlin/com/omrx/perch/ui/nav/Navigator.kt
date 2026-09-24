package com.omrx.perch.ui.nav

import androidx.compose.runtime.Composable
import com.omrx.perch.auth.getGmailCredential
import com.omrx.perch.auth.getUserEmail
import com.omrx.perch.config.AccountConfig
import com.omrx.perch.config.AccountType
import com.omrx.perch.logging.Logger
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.AppState
import com.omrx.perch.ui.screens.AccountsScreen
import com.omrx.perch.ui.screens.LoginScreen
import com.omrx.perch.ui.screens.SettingsScreen
import com.omrx.perch.util.AppError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class Screen { Inbox, Accounts, Login, Settings }

@Suppress("FunctionName")
@Composable
fun Navigator(appState: AppState, navHost: NavHost) {
    val logger = Logger.get("Navigator")

        when (appState.currentScreen) {
            Screen.Inbox -> com.omrx.perch.PerchApp(
                appState.mailList,
                fetchBody = { uid ->
                    val index = appState.mailList.indexOfFirst { it.uid == uid }
                    if (index >= 0) appState.mailList[index] =
                        appState.mailList[index].copy(isRead = true)
                    appState.watcherRef?.markAsRead(uid)
                    appState.watcherRef?.fetchBody(uid) ?: MailContent.Empty
                },
                onOpenAccounts = { navHost.push(Screen.Accounts) }
            )

            Screen.Accounts -> AccountsScreen(
                accounts = appState.accounts,
                isLoading = appState.isInitializingAccounts,
                onAddAccount = {
                    appState.isInitializingAccounts = true
                    // Launch in background coroutine using CoroutineScope
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            val credential = getGmailCredential()
                            credential.refreshToken()
                            val email = getUserEmail(credential)
                            val newAccount = AccountConfig(
                                id = email,
                                displayName = email,
                                email = email,
                                type = AccountType.GMAIL,
                                imapHost = "imap.gmail.com"
                            )
                            if (!appState.accounts.any { it.id == email }) {
                                appState.accounts += newAccount
                                logger.info("Account added: $email")
                            } else {
                                appState.currentError = AppError(
                                    title = "Account Exists",
                                    message = "This account is already configured",
                                    recoverable = true
                                )
                            }
                        } catch (e: Exception) {
                            logger.error("Failed to add account", e)
                            appState.currentError = AppError(
                                title = "Failed to Add Account",
                                message = e.message ?: "Unable to add account",
                                throwable = e
                            )
                        } finally {
                            appState.isInitializingAccounts = false
                        }
                    }
                },
                onRemoveAccount = { id ->
                    appState.accounts = appState.accounts.filter { it.id != id }
                    logger.info("Account removed: $id")
                },
                onBack = { navHost.popBackStack() }
            )
            Screen.Settings -> SettingsScreen(
                onAccounts = { navHost.push(Screen.Accounts) },
                onBack = { navHost.popBackStack() },
            )
            Screen.Login -> LoginScreen(
                onSuccess = { navHost.replaceAll(Screen.Inbox) }
            )
        }
    }