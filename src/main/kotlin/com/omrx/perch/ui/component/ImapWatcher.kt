package com.omrx.perch.ui.component

import com.omrx.perch.config.AccountConfig
import com.omrx.perch.logging.Logger
import com.omrx.perch.util.MailCache
import jakarta.mail.*
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import org.eclipse.angus.mail.imap.IMAPFolder
import jakarta.mail.UIDFolder
import java.util.Properties
import java.io.ByteArrayOutputStream
import kotlin.math.min

data class MailSummary(
    val uid: Long,
    val subject: String,
    val from: String,
    val date: java.util.Date,
    val isRead: Boolean
)

class ImapWatcher(
    private val accountConfig: AccountConfig,
    private val accessToken: String,
    private val onNewMail: (List<MailSummary>) -> Unit,
    private val cache: MailCache = MailCache()
) {
    private var store: Store? = null
    private var folder: IMAPFolder? = null
    private var _isRunning = false
    val isRunning: Boolean get() = _isRunning
    private val logger = Logger.get("ImapWatcher[${accountConfig.email}]")

    fun connect() {
        logger.info("Connecting to ${accountConfig.imapHost}:${accountConfig.imapPort}...")
        try {
            val props = Properties().apply {
                put("mail.store.protocol", "imaps")
                put("mail.imaps.host", accountConfig.imapHost)
                put("mail.imaps.port", accountConfig.imapPort.toString())
                put("mail.imaps.auth.mechanisms", "XOAUTH2")
                put("mail.imaps.ssl.enable", "true")
                put("mail.imaps.ssl.checkserveridentity", "true")
            }
            val session = Session.getInstance(props)
            store = session.getStore("imaps").apply {
                connect(accountConfig.imapHost, accountConfig.email, accessToken)
            }
            folder = try {
                val inbox = store!!.getFolder("INBOX") as IMAPFolder
                if (!inbox.exists()) {
                    logger.error("INBOX folder does not exist")
                    throw MessagingException("INBOX folder does not exist")
                }
                inbox.open(Folder.READ_WRITE)
                logger.info("Connected successfully. Total messages: ${inbox.messageCount}")
                inbox
            } catch (e: MessagingException) {
                logger.error("Failed to open INBOX folder", e)
                throw e
            } catch (e: ClassCastException) {
                logger.error("INBOX folder is not an IMAPFolder", e)
                throw e
            }
        } catch (e: Exception) {
            logger.error("Connection failed", e)
            throw e
        }
    }

    fun fetchRecentSummaries(limit: Int = 50): List<MailSummary> {
        return try {
            val f = folder ?: error("Call connect() first")
            val total = f.messageCount
            val start = maxOf(1, total - min(limit, 1000) + 1) // Cap at 1000 messages
            val messages = f.getMessages(start, total)
            val summaries = toSummaries(messages).reversed()
            cache.cacheSummaries(summaries)
            logger.debug("Fetched ${summaries.size} recent messages. ${cache.getStats()}")
            summaries
        } catch (e: Exception) {
            logger.error("Failed to fetch recent summaries", e)
            emptyList()
        }
    }

    fun startIdleLoop() {
        if (_isRunning) {
            logger.warn("IDLE loop already running")
            return
        }
        _isRunning = true
        var retryCount = 0
        val maxRetries = 5
        val baseDelayMs = 2000L

        while (_isRunning && retryCount < maxRetries) {
            try {
                val f = folder ?: error("Call connect() first")
                f.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(e: MessageCountEvent) {
                        logger.info("New mail received: ${e.messages.size} message(s)")
                        try {
                            onNewMail(toSummaries(e.messages))
                        } catch (ex: Exception) {
                            logger.error("Error processing new mail", ex)
                        }
                    }
                })
                while (_isRunning) {
                    try {
                        f.idle()
                    } catch (e: FolderClosedException) {
                        logger.warn("Folder closed, reconnecting...")
                        break
                    }
                }
                retryCount = 0 // Reset on successful connection
            } catch (e: Exception) {
                retryCount++
                val delay = baseDelayMs * (1L shl (retryCount - 1)) // Exponential backoff
                logger.warn("IDLE error (retry $retryCount/$maxRetries): ${e.message}. Retrying in ${delay}ms...")
                Thread.sleep(delay)
                try {
                    if (_isRunning) {
                        connect()
                    }
                } catch (e: Exception) {
                    logger.error("Reconnect failed", e)
                }
            }
        }
        logger.info("IDLE loop stopped after $retryCount retries.")
        stop() // Ensure resources are cleaned up
    }

    fun stop() {
        logger.info("Stopping IMAP watcher...")
        _isRunning = false
        try {
            folder?.close(false)
            logger.debug("Folder closed")
        } catch (e: Exception) {
            logger.warn("Error closing folder", e)
        }
        try {
            store?.close()
            logger.debug("Store closed")
        } catch (e: Exception) {
            logger.warn("Error closing store", e)
        }
        logger.info("IMAP watcher stopped")
    }

    private fun toSummaries(messages: Array<Message>): List<MailSummary> {
        val f = folder ?: error("Call connect() first")
        val fp = FetchProfile().apply {
            add(FetchProfile.Item.ENVELOPE)
            add(FetchProfile.Item.FLAGS)
            add(UIDFolder.FetchProfileItem.UID)
        }
        f.fetch(messages, fp)
        return messages.map {
            MailSummary(
                uid = f.getUID(it),
                subject = it.subject ?: "(no subject)",
                from = it.from?.firstOrNull()?.toString() ?: "(unknown)",
                date = it.sentDate ?: java.util.Date(),
                isRead = it.isSet(Flags.Flag.SEEN)
            )
        }
    }

    fun fetchBody(uid: Long): MailContent {
        // Check cache first
        cache.getContent(uid)?.let {
            logger.debug("Retrieved cached content for message $uid")
            return it
        }
        
        return try {
            val f = folder ?: error("Call connect() first")
            val message = f.getMessageByUID(uid) ?: return MailContent.Empty
            val inlineImages = mutableMapOf<String, ByteArray>()
            val html = extractContent(message, inlineImages)
            val content = html ?: MailContent.Empty
            cache.cacheContent(uid, content)
            content
        } catch (e: Exception) {
            logger.error("Failed to fetch body for message $uid", e)
            MailContent.Empty
        }
    }

    fun markAsRead(uid: Long) {
        try {
            val f = folder ?: return
            val message = f.getMessageByUID(uid) ?: return
            f.setFlags(arrayOf(message), Flags(Flags.Flag.SEEN), true)
            logger.debug("Marked message $uid as read")
        } catch (e: Exception) {
            logger.warn("Error marking message $uid as read", e)
        }
    }

    private fun extractContent(part: Part, inlineImages: MutableMap<String, ByteArray>): MailContent? {
        return when {
            part.isMimeType("text/html") -> {
                val html = part.content as? String ?: return null
                MailContent.Html(embedInlineImages(html, inlineImages))
            }

            part.isMimeType("text/plain") -> {
                val text = part.content as? String ?: return null
                MailContent.PlainText(text)
            }

            part.isMimeType("image/*") -> {
                val bytes = readBytes(part.inputStream)
                MailContent.ImageOnly(bytes, part.contentType.substringBefore(";").trim())
            }

            part.isMimeType("multipart/*") -> {
                val mp = part.content as Multipart
                var htmlResult: MailContent.Html? = null
                var plainResult: MailContent.PlainText? = null

                for (i in 0 until mp.count) {
                    val bp = mp.getBodyPart(i)
                    val disposition = bp.disposition

                    if (bp.isMimeType("image/*") &&
                        (disposition == Part.INLINE || bp.getHeader("Content-ID") != null)
                    ) {
                        val cid = bp.getHeader("Content-ID")?.firstOrNull()
                            ?.trim('<', '>')
                        if (cid != null) {
                            inlineImages[cid] = readBytes(bp.inputStream)
                        }
                        continue
                    }

                    when (val extracted = extractContent(bp, inlineImages)) {
                        is MailContent.Html -> htmlResult = extracted
                        is MailContent.PlainText -> plainResult = extracted
                        else -> {}
                    }
                }

                htmlResult ?: plainResult
            }

            else -> null
        }
    }

    private fun embedInlineImages(html: String, inlineImages: Map<String, ByteArray>): String {
        var result = html
        for ((cid, bytes) in inlineImages) {
            val base64 = java.util.Base64.getEncoder().encodeToString(bytes)
            val dataUri = "data:image/png;base64,$base64"
            result = result.replace("cid:$cid", dataUri)
        }
        return result
    }

    private fun readBytes(input: java.io.InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        input.copyTo(buffer)
        return buffer.toByteArray()
    }
}
