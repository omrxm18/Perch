package com.omrx.perch.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.network.CefRequest
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Safelist
import java.awt.Desktop
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import javax.imageio.ImageIO

/**
 * Sanitizes email HTML using jsoup's allowlist-based cleaner instead of regex.
 * Regex can't reliably parse (deliberately malformed) HTML; a real parser can.
 *
 * @param allowRemoteImages if false, strips http(s) <img> sources (tracking-pixel
 *   protection). cid: sources are left alone — resolve those separately before
 *   calling this, or handle them in a custom scheme handler.
 */
fun sanitizeHtml(html: String, allowRemoteImages: Boolean = false): String {
    // relaxed() permits the common structural/formatting tags mail HTML relies on
    // (tables, spans, basic styling) while dropping <script>, event handlers,
    // <iframe>, <object>, javascript: URLs, forms, etc. by default.
    val safelist = Safelist.relaxed()
        .addTags("style") // inline <style> blocks are extremely common in mail HTML
        .addAttributes(":all", "style", "class")
        .addAttributes("table", "cellpadding", "cellspacing", "border", "width")
        .addAttributes("td", "width", "height", "colspan", "rowspan", "align", "valign")
        .addAttributes("img", "width", "height", "alt")
        .addProtocols("img", "src", "cid", "data") // allow cid:/data: alongside http/https

    val cleaned: Document = Jsoup.clean(html, "", safelist, Document.OutputSettings().prettyPrint(false))
        .let { Jsoup.parse(it) }

    if (!allowRemoteImages) {
        cleaned.select("img[src~=^https?://]").forEach { img ->
            img.attr("data-blocked-src", img.attr("src"))
            img.removeAttr("src")
        }
    }

    // Force all non-cid/data links and any leftover navigation to open externally by
    // marking them; actual interception happens in onBeforeBrowse below.
    cleaned.select("a[href]").forEach { it.attr("target", "_blank") }

    return cleaned.outerHtml()
}

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
    // TODO: resolve cid: references against content's attachment parts before
    // sanitizing, e.g. by inlining them as data: URIs:
    //   val resolved = resolveCidReferences(content.html, content.attachments)
    // Left as a stub since it depends on how MailContent.Html carries attachments.
    val sanitizedHtml = remember(content.html) { sanitizeHtml(content.html, allowRemoteImages = false) }

    // JcefBootstrap.initialize() runs on a background thread (it may need to
    // download/locate the JCEF native bundle on first launch, per its progress
    // callback), so cefApp can still be null the moment this composable enters
    // composition. Calling newClient() eagerly before that finishes silently
    // returns null and nothing ever renders — no exception, no browser, no error.
    // This state flips to true only once initialize()'s onReady actually fires.
    var jcefReady by remember { mutableStateOf(JcefBootstrap.isReady) }

    DisposableEffect(Unit) {
        if (!jcefReady) {
            JcefBootstrap.initialize(
                onProgress = { /* surface a progress bar here if desired */ },
                onReady = { jcefReady = true }
            )
        }
        onDispose { }
    }

    if (!jcefReady) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val browser = remember(sanitizedHtml, jcefReady) {
        val client = JcefBootstrap.newClient()
        client?.addRequestHandler(object : CefRequestHandlerAdapter() {
            override fun onBeforeBrowse(
                browser: CefBrowser?, frame: CefFrame?,
                request: CefRequest?, userGesture: Boolean, isRedirect: Boolean
            ): Boolean {
                val url = request?.url ?: return false
                // Always allow the initial navigation to our own temp file.
                if (url.startsWith("file:")) return false
                if (userGesture && !url.startsWith("data:") && !url.startsWith("cid:")) {
                    Desktop.getDesktop().browse(URI(url))
                    return true
                }
                // Block any non-user-gesture navigation away from the message
                // (e.g. a meta-refresh or script-triggered redirect that slipped through).
                return !userGesture
            }
        })

        // Load via a temp file instead of a data: URL: data: URLs are subject to
        // per-platform length limits (this was silently truncating message bodies,
        // which is why only <title> was ever rendering) and URLEncoder.encode is
        // the wrong encoding for URLs in the first place (it's form-encoding).
        val tempFile = Files.createTempFile("perch-mail-", ".html").toFile()
        tempFile.writeText(sanitizedHtml, Charsets.UTF_8)
        tempFile.deleteOnExit()

        // isOffscreenRendered = true: on Linux, JCEF's windowed (non-OSR) mode does not
        // reliably paint at all — it's a documented gap, not a config mistake — and shows
        // up exactly as a correctly-sized but solid black/blank rectangle.
        // OSR renders into a buffer that JCEF composites via its own
        // CefRenderPanel, which is still returned from getUIComponent() and still embeds
        // fine in SwingPanel — no other code here needs to change.
        val createdBrowser = client?.createBrowser(tempFile.toURI().toString(), true, false)
        createdBrowser to tempFile
    }

    DisposableEffect(browser) {
        onDispose {
            browser?.first?.close(true)
            browser?.second?.delete()
        }
    }

    val cefBrowser = browser?.first
    if (cefBrowser == null) {
        // newClient()/createBrowser() failed even though jcefReady is true — a real
        // failure (missing natives, sandbox issue, etc.), not just a timing issue.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Couldn't render this message.")
        }
        return
    }

    // fillMaxSize is required: without an explicit size, Compose gives SwingPanel
    // no layout bounds and the embedded AWT component can end up with zero area,
    // which looks identical to "nothing rendered" even if CEF itself is fine.
    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = { cefBrowser.uiComponent as java.awt.Component }
    )
}