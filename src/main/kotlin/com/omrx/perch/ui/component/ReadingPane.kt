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
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.ByteArrayInputStream
import java.net.URI
import java.nio.file.Files
import java.util.Base64
import javax.imageio.ImageIO

/**
 * Sanitizes email HTML using jsoup's allowlist-based cleaner instead of regex.
 * Regex can't reliably parse (deliberately malformed) HTML; a real parser can.
 *
 * @param allowRemoteImages if false, strips http(s) <img> sources (tracking-pixel
 *   protection).
 * @param inlineImages inline attachment bytes keyed by Content-ID (without angle
 *   brackets). Matching cid: image sources are rewritten to base64 data: URIs
 *   directly in the HTML, rather than resolved at request-time by the browser —
 *   this avoids a custom CEF scheme handler entirely, which turned out to crash
 *   natively on this JCEF build regardless of registration timing.
 */
fun sanitizeHtml(html: String, allowRemoteImages: Boolean = false, inlineImages: Map<String, ByteArray> = emptyMap()): String {
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

    if (inlineImages.isNotEmpty()) {
        cleaned.select("img[src~=^cid:]").forEach { img ->
            val contentId = img.attr("src").removePrefix("cid:").trim('<', '>')
            val bytes = inlineImages[contentId]
            if (bytes != null) {
                val base64 = Base64.getEncoder().encodeToString(bytes)
                img.attr("src", "data:${guessImageMimeType(bytes)};base64,$base64")
            }
            // If no match, leave the cid: src as-is — it'll just fail to load,
            // same as a real browser would with an unresolved inline reference.
        }
    }

    // Force all non-cid/data links and any leftover navigation to open externally by
    // marking them; actual interception happens in onBeforeBrowse below.
    cleaned.select("a[href]").forEach { it.attr("target", "_blank") }

    return cleaned.outerHtml()
}

/** Sniffs common image formats by magic bytes since inline attachments carry no reliable extension here. */
private fun guessImageMimeType(bytes: ByteArray): String = when {
    bytes.size >= 8 && bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() -> "image/png"
    bytes.size >= 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "image/jpeg"
    bytes.size >= 6 && bytes[0] == 'G'.code.toByte() && bytes[1] == 'I'.code.toByte() -> "image/gif"
    bytes.size >= 12 && bytes[8] == 'W'.code.toByte() && bytes[9] == 'E'.code.toByte() -> "image/webp"
    else -> "application/octet-stream"
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
    // ASSUMPTION: MailContent.Html exposes inline attachment bytes keyed by
    // Content-ID (without angle brackets), e.g. `val inlineImages: Map<String, ByteArray>`.
    // Adjust this line to match your actual field/shape if it differs.
    val inlineImages = content.inlineImages

    val sanitizedHtml = remember(content.html, inlineImages) {
        sanitizeHtml(content.html, allowRemoteImages = false, inlineImages = inlineImages)
    }

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
        // up exactly as a correctly-sized but solid black/blank rectangle, which is what
        // we were seeing. OSR renders into a buffer that JCEF composites via its own
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
    val uiComponent = remember(cefBrowser) { cefBrowser.uiComponent as java.awt.Component }

    DisposableEffect(uiComponent, cefBrowser) {
        // OSR browsers track focus internally, separately from AWT focus. Until CEF
        // is explicitly told it has focus, input routing into the renderer doesn't
        // work properly — this is what "renders but is unclickable" looks like.
        // Nothing wires this up automatically when the component is handed to
        // SwingPanel via a bare factory lambda, so it has to be done here.
        uiComponent.isFocusable = true

        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent?) { cefBrowser.setFocus(true) }
            override fun focusLost(e: FocusEvent?) { cefBrowser.setFocus(false) }
        }
        val mouseListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent?) { uiComponent.requestFocusInWindow() }
        }
        uiComponent.addFocusListener(focusListener)
        uiComponent.addMouseListener(mouseListener)

        onDispose {
            uiComponent.removeFocusListener(focusListener)
            uiComponent.removeMouseListener(mouseListener)
        }
    }

    SwingPanel(
        modifier = Modifier.fillMaxSize(),
        factory = { uiComponent }
    )
}