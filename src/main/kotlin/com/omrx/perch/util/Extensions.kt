package com.omrx.perch.util

import java.util.Date

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