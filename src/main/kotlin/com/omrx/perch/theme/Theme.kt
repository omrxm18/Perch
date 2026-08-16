package com.omrx.perch.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object PerchColors {
    val bg = Color(0xFF1C1C1E)
    val bgElevated = Color(0xFF232326)
    val bgSelected = Color(0xFF2D2D6B)
    val accent = Color(0xFF6C7CFF)
    val textPrimary = Color(0xFFECECEC)
    val textSecondary = Color(0xFF9A9AA0)
    val unreadDot = Color(0xFF6C7CFF)
    val divider = Color(0xFF2E2E31)
}

object PerchType {
    val subjectUnread = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = PerchColors.textPrimary
    )
    val subjectRead = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, color = PerchColors.textSecondary)
    val sender = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = PerchColors.textPrimary)
    val preview = TextStyle(fontSize = 12.sp, color = PerchColors.textSecondary)
    val timestamp = TextStyle(fontSize = 11.sp, color = PerchColors.textSecondary)
}