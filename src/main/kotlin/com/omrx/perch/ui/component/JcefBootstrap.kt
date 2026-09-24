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

    private val installDir = File(System.getProperty("user.home"), ".perch/jcef-bundle")
    private val cacheDir = File(System.getProperty("user.home"), ".perch/jcef-cache")

    fun initialize(
        onProgress: (Float) -> Unit,
        onReady: () -> Unit,
        onError: (Throwable) -> Unit = { throw it }
    ) {
        if (isReady) {
            onReady(); return
        }
        Thread({
            try {
                lock.withLock {
                    if (!isReady) {
                        val builder = CefAppBuilder()
                        builder.setInstallDir(installDir)
                        builder.setProgressHandler { state, percent -> onProgress(percent) }
                        builder.cefSettings.cache_path = cacheDir.absolutePath
                        builder.addJcefArgs("--disable-gpu", "--disable-extensions")
                        builder.setAppHandler(object : MavenCefAppHandlerAdapter() {})
                        cefApp = builder.build()
                        isReady = true
                    }
                }
                onReady()
            } catch (t: Throwable) {
                onError(t)
            }
        }, "JCEF Bootstrap Thread").apply { isDaemon = true }.start()
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