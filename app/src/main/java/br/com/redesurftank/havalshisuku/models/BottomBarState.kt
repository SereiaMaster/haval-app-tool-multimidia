package br.com.redesurftank.havalshisuku.models

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
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
    enum class SliderType {
        DRIVER_TEMP,
        PASS_TEMP,
        FAN,
        VOLUME
    }

    var activeSliderType by mutableStateOf<SliderType?>(null)
    var sliderPositionX by mutableStateOf(0f)
    var sliderInteractionTrigger by mutableStateOf(0)
    var isSliderDragging by mutableStateOf(false)
    var isVisible by mutableStateOf(false)
    var isDashboardExpanded by mutableStateOf(false)
    var radialSubMenu by mutableStateOf(RadialSubMenu.None)
    var isMenuExpanded by mutableStateOf(false)
    var isSettingsMenuExpanded by mutableStateOf(false)
    var isOverrideMenuExpanded by mutableStateOf(false)
    var selectedPackage by mutableStateOf("")
    var currentPackage by mutableStateOf("")
    var activeClusterProjectionPackage by mutableStateOf("")
    var mediaTitle by mutableStateOf<String?>(null)
    var mediaArtist by mutableStateOf<String?>(null)
    var mediaAlbum by mutableStateOf<String?>(null)
    var mediaPackageName by mutableStateOf<String?>(null)
    var mediaArtwork by mutableStateOf<Bitmap?>(null)
    var mediaIsPlaying by mutableStateOf(false)
    var mediaIsMuted by mutableStateOf(false)
    var mediaDurationMs by mutableLongStateOf(0L)
    var mediaElapsedMs by mutableLongStateOf(0L)
    var mediaProgressUpdatedAtMs by mutableLongStateOf(0L)
    var mediaCanSeek by mutableStateOf(false)
    var autoHideEnabled by mutableStateOf(false)
    /** Quando true, renderiza a barra inferior antiga (horizontal) em vez da nova dock. */
    var useLegacyBottomBar by mutableStateOf(false)

    /**
     * Multiplicador de escala da dock definido pelo utilizador (1.0 = padrão).
     * Carregado das preferências no arranque do serviço e persistido ao alterar no
     * painel "Avançado". Multiplica todo o tamanho/conteúdo da dock.
     */
    var dockUiScale by mutableFloatStateOf(1.0f)
    var isFridaRunning by mutableStateOf(false)
    var isDeleteModeEnabled by mutableStateOf(false)
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
