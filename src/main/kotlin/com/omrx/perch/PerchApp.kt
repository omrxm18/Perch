package com.omrx.perch

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omrx.perch.theme.PerchColors
import com.omrx.perch.theme.PerchType
import com.omrx.perch.ui.component.MailContent
import com.omrx.perch.ui.component.MailReadingPane
import com.omrx.perch.ui.component.MailSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Composable
fun PerchApp(
    mailList: List<MailSummary>,
    fetchBody: (Long) -> MailContent,
    onOpenAccounts: () -> Unit
) {
    var selectedMail by remember { mutableStateOf<MailSummary?>(null) }
    var mailContent by remember { mutableStateOf<MailContent?>(null) }
    var isLoadingBody by remember { mutableStateOf(false) }

    LaunchedEffect(selectedMail?.uid) {
        val mail = selectedMail
        if (mail == null) {
            mailContent = null
            return@LaunchedEffect
        }
        isLoadingBody = true
        mailContent = withContext(Dispatchers.IO) { fetchBody(mail.uid) }
        isLoadingBody = false
    }

    Row(modifier = Modifier.fillMaxSize().background(PerchColors.bg)) {

        Column(
            modifier = Modifier.width(64.dp).fillMaxHeight().background(PerchColors.bgElevated),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            NavIcon(Icons.Default.Inbox, selected = true)
            NavIcon(Icons.Default.Star, selected = false)
            NavIcon(Icons.AutoMirrored.Filled.Send, selected = false)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .padding(vertical = 6.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onOpenAccounts() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Accounts", tint = PerchColors.textSecondary)
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = PerchColors.divider)

        Box(modifier = Modifier.width(340.dp).fillMaxHeight()) {
            LazyColumn(contentPadding = PaddingValues(vertical = 4.dp)) {
                items(mailList) { mail ->
                    MailRow(
                        mail = mail,
                        selected = mail == selectedMail,
                        onClick = { selectedMail = mail }
                    )
                }
            }
        }

        Divider(modifier = Modifier.fillMaxHeight().width(1.dp), color = PerchColors.divider)

        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(24.dp)) {
            when {
                selectedMail == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select a message", style = PerchType.preview)
                }
                isLoadingBody -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(selectedMail!!.subject, style = PerchType.subjectUnread.copy(fontSize = 20.sp))
                    Spacer(Modifier.height(8.dp))
                    Text(selectedMail!!.from, style = PerchType.sender)
                    Spacer(Modifier.height(20.dp))
                    mailContent?.let { MailReadingPane(it) }
                }
            }
        }
    }
}

@Composable
fun NavIcon(icon: ImageVector, selected: Boolean) {
    Box(
        modifier = Modifier
            .padding(vertical = 6.dp)
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) PerchColors.bgSelected else Color.Transparent)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) PerchColors.accent else PerchColors.textSecondary)
    }
}

@Composable
fun MailRow(mail: MailSummary, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) PerchColors.bgSelected else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.padding(top = 6.dp, end = 8.dp).size(6.dp)
                .clip(CircleShape)
                .background(if (!mail.isRead) PerchColors.unreadDot else Color.Transparent)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(mail.from, style = PerchType.sender, maxLines = 1)
                Text(relativeTime(mail.date), style = PerchType.timestamp)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                mail.subject,
                style = if (!mail.isRead) PerchType.subjectUnread else PerchType.subjectRead,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun relativeTime(date: Date): String {
    val diffMin = (System.currentTimeMillis() - date.time) / 60000
    return when {
        diffMin < 60 -> "${diffMin}m"
        diffMin < 1440 -> "${diffMin / 60}h"
        else -> "${diffMin / 1440}d"
    }
}