package com.omrx.perch.ui.component

import me.friwi.jcefmaven.CefAppBuilder
import me.friwi.jcefmaven.EnumProgress
import me.friwi.jcefmaven.MavenCefAppHandlerAdapter
import org.cef.CefApp
import org.cef.CefClient
import org.cef.callback.CefSchemeRegistrar
import java.io.File

object JcefBootstrap {
    private var cefApp: CefApp? = null
    var isReady: Boolean = false
        private set

    val currentInlineImages: MutableMap<String, ByteArray> = mutableMapOf()

    fun initialize(onProgress: (Float) -> Unit, onReady: () -> Unit) {
        Thread {
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
        }.start()
    }

    fun newClient(): CefClient? = cefApp?.createClient()

    fun dispose() {
        cefApp?.dispose()
    }
}