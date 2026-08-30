package com.omrx.perch.ui.component

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object JcefBootstrap {
    private var cefApp: CefApp? = null
    private val lock = ReentrantLock()
    @Volatile
    var isReady: Boolean = false
        private set

    fun initialize(onProgress: (Float) -> Unit, onReady: () -> Unit) {
        if (isReady) {
            onReady()
            return
        }
        Thread {
            lock.withLock {
                if (isReady) {
                    onReady()
                    return@withLock
                }
                val builder = CefAppBuilder()
                builder.setInstallDir(File(System.getProperty("user.home"), ".perch/jcef-bundle"))
                builder.setProgressHandler { state, percent ->
                    if (state == EnumProgress.LOCATING || state == EnumProgress.DOWNLOADING) {
                        onProgress(percent)
                    }
                }
                // No custom scheme / scheme handler factory registration here anymore:
                // that path crashed natively regardless of registration timing (a "CToCpp
                // called with invalid version -1" fatal deep in CEF's marshaling layer).
                // cid: images are now resolved by rewriting them to data: URIs in
                // sanitizeHtml() before the browser ever navigates, so this class no
                // longer needs to know about inline attachments at all.
                builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
                cefApp = builder.build()
                isReady = true
                onReady()
            }
        }.start()
    }

    fun newClient(): CefClient? = lock.withLock { cefApp?.createClient() }

    fun dispose() {
        lock.withLock {
            cefApp?.dispose()
            cefApp = null
            isReady = false
        }
    }
}