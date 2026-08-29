package com.omrx.perch.ui.component

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import org.cef.callback.CefSchemeRegistrar
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object JcefBootstrap {
    private var cefApp: CefApp? = null
    private val lock = ReentrantLock()
    @Volatile
    var isReady: Boolean = false
        private set

    val currentInlineImages: MutableMap<String, ByteArray> = mutableMapOf()

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
                builder.setAppHandler(object : MavenCefAppHandlerAdapter() {
                    override fun onRegisterCustomSchemes(registrar: CefSchemeRegistrar?) {
                        registrar?.addCustomScheme(
                            "cid", false, true, false, true, false, false, false
                        )
                    }
                })
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
            currentInlineImages.clear()
        }
    }
}
