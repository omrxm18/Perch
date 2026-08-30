package com.omrx.perch.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omrx.perch.theme.PerchColors
import com.omrx.perch.theme.PerchType
import com.omrx.perch.util.AppError

@Composable
fun ErrorDialog(
    error: AppError?,
    onDismiss: () -> Unit
) {
    if (error != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(error.title, style = PerchType.subjectUnread)
            },
            text = {
                Column {
                    Text(
                        error.message,
                        style = PerchType.preview,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (error.throwable != null) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Details: ${error.throwable::class.simpleName}",
                            style = TextStyle(fontSize = 11.sp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = onDismiss) {
                    Text("OK")
                }
            }
        )
    }
}
