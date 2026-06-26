package br.com.redesurftank.havalshisuku.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue

enum class RadialSubMenu {
    None,
    Apps,
    Driving,
    Advanced
}

object BottomBarState {
    var isVisible by mutableStateOf(false)
    var radialSubMenu by mutableStateOf(RadialSubMenu.None)
    var isMenuExpanded by mutableStateOf(false)
    var isSettingsMenuExpanded by mutableStateOf(false)
    var isOverrideMenuExpanded by mutableStateOf(false)
    var selectedPackage by mutableStateOf("")
    var currentPackage by mutableStateOf("")
    var autoHideEnabled by mutableStateOf(false)
    /** Quando true, renderiza a barra inferior antiga (horizontal) em vez da nova dock. */
    var useLegacyBottomBar by mutableStateOf(false)
    var isFridaRunning by mutableStateOf(false)
    /** Incremented on user interaction; used for idle auto-close. */
    var radialActivityEpoch by mutableStateOf(0L)
    val restoredApps = mutableStateListOf<String>()

    fun bumpRadialActivity() {
        radialActivityEpoch = System.currentTimeMillis()
    }

    fun closeRadialSubMenu() {
        radialSubMenu = RadialSubMenu.None
        isMenuExpanded = false
        isSettingsMenuExpanded = false
        isOverrideMenuExpanded = false
    }

    fun hideRadialMenu() {
        isVisible = false
        closeRadialSubMenu()
    }

    fun openRadialMenu() {
        bumpRadialActivity()
        isVisible = true
        closeRadialSubMenu()
    }
}
