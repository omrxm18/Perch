package com.omrx.perch.util

import com.omrx.perch.logging.Logger
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.MailSummary
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory cache for mail summaries and content
 */
class MailCache {
    private val summaryCache = ConcurrentHashMap<Long, MailSummary>()
    private val contentCache = ConcurrentHashMap<Long, MailContent>()
    private val logger = Logger.get("MailCache")

    fun cacheSummary(summary: MailSummary) {
        summaryCache[summary.uid] = summary
    }

    fun cacheSummaries(summaries: List<MailSummary>) {
        summaries.forEach { cacheSummary(it) }
    }

    fun getSummary(uid: Long): MailSummary? = summaryCache[uid]

    fun getSummaries(): List<MailSummary> = summaryCache.values.sortedByDescending { it.date }

    fun cacheContent(uid: Long, content: MailContent) {
        if (content !is MailContent.Empty) {
            contentCache[uid] = content
            logger.debug("Cached content for message $uid")
        }
    }

    fun getContent(uid: Long): MailContent? = contentCache[uid]

    fun updateSummary(uid: Long, updated: MailSummary) {
        summaryCache[uid] = updated
    }

    fun removeSummary(uid: Long) {
        summaryCache.remove(uid)
        contentCache.remove(uid)
    }

    fun clear() {
        logger.info("Clearing mail cache")
        summaryCache.clear()
        contentCache.clear()
    }

    fun getStats(): String {
        return "MailCache: ${summaryCache.size} summaries, ${contentCache.size} content entries"
    }
}
