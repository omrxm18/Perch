package com.omrx.perch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omrx.perch.theme.PerchColors
import com.omrx.perch.theme.PerchType

@Composable
fun SettingsScreen(
    onAccounts: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(PerchColors.bg).padding(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Accounts", style = PerchType.subjectUnread.copy(fontSize = 22.sp))
            Button(onClick = onAccounts, enabled = true) {
                Icon(Icons.Default.ManageAccounts, contentDescription = null)
            }
            Text("Go Back", style = PerchType.subjectUnread.copy(fontSize = 22.sp))
            Button(onClick = onBack, enabled = true) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}