package com.omrx.perch.util

import java.util.Date
import java.text.SimpleDateFormat

/**
 * Extension functions for common operations
 */

fun Date.toRelativeTime(): String {
    val diffMin = (System.currentTimeMillis() - this.time) / 60000
    return when {
        diffMin < 1 -> "just now"
        diffMin < 60 -> "${diffMin}m"
        diffMin < 1440 -> "${diffMin / 60}h"
        else -> "${diffMin / 1440}d"
    }
}

fun Date.toFormattedDate(): String {
    val format = SimpleDateFormat("MMM d, yyyy")
    return format.format(this)
}

fun String.truncate(maxLength: Int): String {
    return if (this.length > maxLength) {
        this.take(maxLength - 3) + "..."
    } else {
        this
    }
}

fun <T> List<T>.paginate(page: Int, pageSize: Int): List<T> {
    val start = page * pageSize
    val end = minOf(start + pageSize, size)
    return if (start >= size) emptyList() else subList(start, end)
}
