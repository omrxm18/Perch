package com.omrx.perch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omrx.perch.theme.PerchColors
import com.omrx.perch.theme.PerchType

@Composable
fun LoadingPane(message: String = "Loading...") {
    Column(
        modifier = Modifier.fillMaxSize().background(PerchColors.bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = PerchType.preview)
    }
}

@Composable
fun ErrorPane(
    title: String = "Error",
    message: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize().background(PerchColors.bg).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = "Error",
            modifier = Modifier.size(48.dp),
            tint = Color.Red
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = PerchType.subjectUnread.copy(fontSize = 18.sp))
        Spacer(Modifier.height(8.dp))
        Text(
            message,
            style = PerchType.preview,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            androidx.compose.material.Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

@Composable
fun EmptyPane(message: String = "No messages found") {
    Column(
        modifier = Modifier.fillMaxSize().background(PerchColors.bg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, style = PerchType.preview)
    }
}

@Composable
fun SimpleLoadingIndicator(
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        CircularProgressIndicator(modifier = modifier.size(24.dp))
    }
}
