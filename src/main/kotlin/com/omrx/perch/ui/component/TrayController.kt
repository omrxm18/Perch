package com.omrx.perch.ui.component

import dorkbox.systemTray.SystemTray
import dorkbox.systemTray.MenuItem

private var trayInstance: SystemTray? = null

fun setupTray(onShow: () -> Unit, onHide: () -> Unit, onQuit: () -> Unit) {
    SystemTray.FORCE_TRAY_TYPE = SystemTray.TrayType.AppIndicator
    val tray = SystemTray.get() ?: error("Unable to load SystemTray")
    tray.setImage(ClassLoader.getSystemResource("tray-icon.png"))

    val menu = tray.menu
    menu.add(MenuItem("Show") { onShow() })
    menu.add(MenuItem("Hide") { onHide() })
    menu.add(MenuItem("Quit") { onQuit() })

    trayInstance = tray
}

fun updateUnreadCount(count: Int) {
    trayInstance?.setImage(
        ClassLoader.getSystemResource(
            if (count > 0) "tray-icon-unread.png" else "tray-icon.png"
        )
    )
}