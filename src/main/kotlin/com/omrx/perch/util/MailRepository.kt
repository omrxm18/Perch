package com.omrx.perch.util

import com.omrx.perch.config.AccountConfig
import com.omrx.perch.logging.Logger
import com.omrx.perch.ui.component.ImapWatcher
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.MailSummary

/**
 * Repository pattern for mail operations.
 * Provides high-level API for mail operations with error handling and logging.
 */
class MailRepository(
    private val account: AccountConfig,
    private val accessToken: String,
    private val onNewMail: (List<MailSummary>) -> Unit
) {
    private val watcher = ImapWatcher(account, accessToken, onNewMail)
    private val logger = Logger.get("MailRepository[${account.email}]")

    fun initialize(): Result<Unit> {
        return try {
            watcher.connect()
            Result.Success(Unit)
        } catch (e: Exception) {
            logger.error("Failed to initialize mail repository", e)
            Result.Error(AppError(
                title = "Connection Failed",
                message = "Unable to connect to mail server: ${e.message}",
                throwable = e
            ))
        }
    }

    fun loadRecentMails(limit: Int = 50): List<MailSummary> {
        return watcher.fetchRecentSummaries(limit)
    }

    fun getMailContent(uid: Long): MailContent {
        return watcher.fetchBody(uid)
    }

    fun markAsRead(uid: Long) {
        watcher.markAsRead(uid)
    }

    fun startMonitoring() {
        watcher.startIdleLoop()
    }

    fun stop() {
        watcher.stop()
    }

    fun isRunning(): Boolean = watcher.isRunning

    fun getAccount(): AccountConfig = account
}
