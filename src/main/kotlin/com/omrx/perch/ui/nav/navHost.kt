package com.omrx.perch.ui.nav

import androidx.compose.runtime.mutableStateListOf

class NavHost {
    private val navigateStack = mutableStateListOf(Screen.Inbox)

    val currentScreen: Screen
        get() = navigateStack.lastOrNull() ?: Screen.Inbox

    fun push(screen: Screen) {
        if (currentScreen != screen) {
            navigateStack.add(screen)
        }
    }

    fun popBackStack(): Boolean {
        return if (navigateStack.size > 1) {
            navigateStack.removeAt(navigateStack.lastIndex)
            true
        } else
            false
    }
}
