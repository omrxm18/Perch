package com.omrx.perch.ui.component

import androidx.compose.foundation.Image
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.URLEncoder
import javax.imageio.ImageIO

@Composable
fun MailReadingPane(content: MailContent) {
    when (content) {
        is MailContent.Html -> HtmlReadingPane(content)
        is MailContent.PlainText -> Text(content.text)
        is MailContent.ImageOnly -> {
            val bitmap = remember(content.bytes) {
                ImageIO.read(ByteArrayInputStream(content.bytes))?.toComposeImageBitmap()
            }
            bitmap?.let { Image(bitmap = it, contentDescription = null) }
        }
        MailContent.Empty -> Text("(no content)")
    }
}

@Composable
private fun HtmlReadingPane(content: MailContent.Html) {
    val browser = remember(content) {

        val client = JcefBootstrap.newClient()
        client?.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?, frame: CefFrame?,
                request: CefRequest?, userGesture: Boolean, isRedirect: Boolean
            ): Boolean {
                val url = request?.url ?: return false
                if (userGesture && !url.startsWith("data:") && !url.startsWith("cid:")) {
                    Desktop.getDesktop().browse(URI(url))
                    return true
                }
                return false
            }
        })

        val dataUrl = "data:text/html;charset=utf-8," + URLEncoder.encode(content.html, "UTF-8")
        client?.createBrowser(dataUrl, false, false)
    }

    browser?.let {
        SwingPanel(factory = { it.uiComponent as java.awt.Component })
    }
}