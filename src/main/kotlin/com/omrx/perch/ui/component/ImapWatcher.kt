package com.omrx.perch.ui.component

import jakarta.mail.*
import jakarta.mail.event.MessageCountAdapter
import jakarta.mail.event.MessageCountEvent
import org.eclipse.angus.mail.imap.IMAPFolder
import java.util.Properties
import java.io.ByteArrayOutputStream

data class MailSummary(
    val uid: Long,
    val subject: String,
    val from: String,
    val date: java.util.Date,
    val isRead: Boolean
)

sealed class MailContent {
    data class Html(val html: String) : MailContent()
    data class PlainText(val text: String) : MailContent()
    data class ImageOnly(val bytes: ByteArray, val mimeType: String) : MailContent()
    object Empty : MailContent()
}

class ImapWatcher(
    private val host: String,
    private val user: String,
    private val accessToken: String,
    private val onNewMail: (List<MailSummary>) -> Unit
) {
    private var store: Store? = null
    private var folder: IMAPFolder? = null

    fun connect() {
        val props = Properties().apply {
            put("mail.store.protocol", "imaps")
            put("mail.imaps.host", "imap.gmail.com")
            put("mail.imaps.port", "993")
            put("mail.imaps.auth.mechanisms", "XOAUTH2")
        }
        val session = Session.getInstance(props)
        store = session.getStore("imaps").apply {
            connect(host, user, accessToken)
        }
        folder = (store!!.getFolder("INBOX") as IMAPFolder).apply {
            open(Folder.READ_WRITE)
        }
        println("Connected. Total messages: ${folder!!.messageCount}")
    }

    fun fetchRecentSummaries(limit: Int = 50): List<MailSummary> {
        val f = folder ?: error("Call connect() first")
        val total = f.messageCount
        val start = maxOf(1, total - limit + 1)
        val messages = f.getMessages(start, total)
        return toSummaries(messages).reversed()
    }

    fun startIdleLoop() {
        while (true) {
            try {
                val f = folder ?: error("Call connect() first")
                f.addMessageCountListener(object : MessageCountAdapter() {
                    override fun messagesAdded(e: MessageCountEvent) {
                        println("New mail! Count: ${e.messages.size}")
                        onNewMail(toSummaries(e.messages))
                    }
                })
                while (true) { f.idle() }
            } catch (e: Exception) {
                println("IDLE dropped, reconnecting: ${e.message}")
                Thread.sleep(2000)
                connect()
            }
        }
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
        val f = folder ?: error("Call connect() first")
        val message = f.getMessageByUID(uid) ?: return MailContent.Empty
        val inlineImages = mutableMapOf<String, ByteArray>()
        val html = extractContent(message, inlineImages)
        return html ?: MailContent.Empty
    }

    fun markAsRead(uid: Long) {
        val f = folder ?: return
        val message = f.getMessageByUID(uid) ?: return
        f.setFlags(arrayOf(message), Flags(Flags.Flag.SEEN), true)
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