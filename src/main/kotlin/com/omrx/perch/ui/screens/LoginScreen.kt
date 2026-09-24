package com.omrx.perch.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.omrx.perch.auth.getGmailCredential
import com.omrx.perch.auth.getUserEmail
import com.omrx.perch.logging.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val logger = Logger.get("LoginScreen")

@Suppress("FunctionName")
@Composable
fun LoginScreen(onSuccess: () -> Unit) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                enabled = !loading,
                onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val credential = getGmailCredential()
                                getUserEmail(credential)
                            }
                            onSuccess()
                        } catch (e: Exception) {
                            logger.error("Login failed", e)
                            error = e.message
                        } finally {
                            loading = false
                        }
                    }
                }
            ) {
                Text(if (loading) "Loading..." else "Login with Gmail")
            }
            error?.let { Text(it) }
        }
    }
}