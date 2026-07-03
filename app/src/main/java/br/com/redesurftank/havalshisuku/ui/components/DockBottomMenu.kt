package br.com.redesurftank.havalshisuku.ui.components

import android.view.SoundEffectConstants
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.redesurftank.havalshisuku.models.BottomBarState
import br.com.redesurftank.havalshisuku.models.RadialSubMenu
import br.com.redesurftank.havalshisuku.ui.theme.HavalShisukuTheme
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Mesma identidade visual do Impulse Dashboard (DashboardPanel/DashboardToggleButton).
private val radialAccent = Color(0xFF66E3FF)
private val glassBase = Color(0xFF0E1115)
private val glassEdge = Color.White.copy(alpha = 0.12f)

// Fonte unificada com o dashboard.
private val dockFont = androidx.compose.ui.text.font.FontFamily.SansSerif

// --- Escala tipográfica/cores consistente da dock ---
private val radialTextPrimary = Color.White
private val radialTextSecondary = Color.White.copy(alpha = 0.62f)
private val radialTextMuted = Color.White.copy(alpha = 0.45f)
private val dockLabelStyle = labelStyle.copy(fontFamily = dockFont)

private val radialTitleSize = 23.sp // títulos de secção (Clima / Som)
private val radialValueSize = 46.sp // valores numéricos do clima (temp / ventilação) — grandes p/ tocar e arrastar
private val radialVolumeValueSize = 37.sp // número do volume (linha arrastável)
private val radialVolumeLabelSize = 22.sp // título de cada volume (linha arrastável)
private val radialLabelSize = 17.sp // rótulos (Motorista / Ventilação / etc.)
private val radialChipSize = 20.sp // texto de chips/toggles
private val radialNavLabelSize = 16.sp // rótulos da navegação inferior

/** Fração da largura do ecrã ocupada pela dock (responsivo head unit/emulador). */
internal const val DOCK_WIDTH_FRACTION = 0.72f

/** Limites de temperatura do A/C, iguais aos usados no cluster (AcControlScreen). */
private const val TEMP_MIN = 16f
private const val TEMP_MAX = 32f

/** Fundo da dock — mesma paleta escura de fundo do Impulse Dashboard. */
private fun Modifier.dockSurface(): Modifier =
        this.background(
                brush =
                        Brush.verticalGradient(
                                colors =
                                        listOf(
                                                Color(0xFF12161B).copy(alpha = 0.98f),
                                                Color(0xFF0B0E11).copy(alpha = 0.99f),
                                        ),
                        ),
                shape = RoundedCornerShape(16.dp),
        ).border(1.dp, glassEdge, RoundedCornerShape(16.dp))

/** Cartão interno (Clima / Som) — idêntico ao DashboardPanel do dashboard. */
private fun Modifier.innerCard(): Modifier =
        this.background(
                brush =
                        Brush.linearGradient(
                                colors =
                                        listOf(
                                                Color(0xFF171D22).copy(alpha = 0.96f),
                                                Color(0xFF0E1115).copy(alpha = 0.98f),
                                        ),
                        ),
                shape = RoundedCornerShape(8.dp),
        ).border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))

private data class OuterRingItem(
        val label: String,
        val icon: ImageVector,
        val angleDeg: Float,
        val subMenu: RadialSubMenu?,
)

private val outerRingItems =
        listOf(
                OuterRingItem("Condução", Icons.Default.DirectionsCar, 255f, RadialSubMenu.Driving),
                OuterRingItem("Apps", Icons.Default.Apps, 217f, RadialSubMenu.Apps),
                OuterRingItem("Avançado", Icons.Default.Tune, 331f, RadialSubMenu.Advanced),
        )

private fun bumpAnd(action: () -> Unit): () -> Unit = {
    BottomBarState.bumpRadialActivity()
    action()
}

@Composable
fun RadialMenuContent(
        scope: CoroutineScope,
        driverTemp: String,
        passTemp: String,
        volume: Int,
        navVolume: Int,
        alertVolume: Int,
        phoneVolume: Int,
        voiceVolume: Int,
        fanSpeed: Int,
        isACEnabled: Boolean,
        acSync: String,
        acAuto: String,
        driveMode: String,
        powerModel: String,
        energyRecovery: String,
        steeringMode: String,
        onDriverTempChange: (Float) -> Unit,
        onPassTempChange: (Float) -> Unit,
        onFanChange: (Int) -> Unit,
        onVolumeChange: (Int) -> Unit,
        onNavVolumeChange: (Int) -> Unit,
        onAlertVolumeChange: (Int) -> Unit,
        onPhoneVolumeChange: (Int) -> Unit,
        onVoiceVolumeChange: (Int) -> Unit,
        onSyncToggle: () -> Unit,
        onAutoToggle: () -> Unit,
        onACPowerToggle: () -> Unit,
) {
    val subMenu = BottomBarState.radialSubMenu
    val bodyScroll = rememberScrollState()

    BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.BottomStart,
    ) {
        // Dock no lado do motorista (esquerda), ocupando ~50% da largura.
        // Responsiva: cresce/encolhe conforme a largura real (emulador x carro).
        val dockWidth = (maxWidth * DOCK_WIDTH_FRACTION).coerceIn(604.dp, 1296.dp)
        val uiScale = (dockWidth / 620.dp).coerceIn(1.0f, 1.7f)
        val pad = (14 * uiScale).dp
        val gap = (8 * uiScale).dp
        val cardPad = (10 * uiScale).dp
        val handleClose = bumpAnd { BottomBarState.hideRadialMenu() }

        // Escala definida pelo utilizador (painel Avançado): aumenta a densidade
        // efetiva, fazendo TODO o conteúdo da dock (larguras, paddings, fontes,
        // botões) multiplicar por este fator e refluir naturalmente.
        val userScale = BottomBarState.dockUiScale
        val baseDensity = LocalDensity.current

        // Altura máxima do dock = altura disponível na tela (descontando a escala do
        // utilizador, pois o conteúdo é renderizado numa densidade maior). Garante que
        // a barra de botões inferior fique sempre visível e o corpo role internamente.
        val maxDockHeightDp = (maxHeight / userScale) - 12.dp

        CompositionLocalProvider(
                LocalDensity provides
                        Density(baseDensity.density * userScale, baseDensity.fontScale)
        ) {
        Column(
                modifier =
                        Modifier.width(dockWidth)
                                .heightIn(max = maxDockHeightDp)
                                .padding(start = (10 * uiScale).dp, bottom = (10 * uiScale).dp)
                                .dockSurface()
                                .padding(pad),
                verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // Corpo rolável: ocupa só o espaço que sobra acima da barra de botões
            // (weight + fill=false). Quando o conteúdo (Clima/Som, Apps, Condução,
            // Avançado) é maior que o disponível, ele rola em vez de empurrar/ocultar
            // a barra inferior, que fica fixa.
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(bodyScroll),
            ) {
                when (subMenu) {
                    RadialSubMenu.None ->
                            Row(
                                    modifier =
                                            Modifier.fillMaxWidth()
                                                    .height(IntrinsicSize.Min),
                                    horizontalArrangement = Arrangement.spacedBy(gap),
                            ) {
                                RadialClimatePanel(
                                        modifier =
                                                Modifier.weight(2f)
                                                        .fillMaxHeight()
                                                        .innerCard()
                                                        .padding(cardPad),
                                        driverTemp = driverTemp,
                                        passTemp = passTemp,
                                        fanSpeed = fanSpeed,
                                        isACEnabled = isACEnabled,
                                        acSync = acSync,
                                        acAuto = acAuto,
                                        onDriverTempChange = onDriverTempChange,
                                        onPassTempChange = onPassTempChange,
                                        onFanChange = onFanChange,
                                        onSyncToggle = onSyncToggle,
                                        onAutoToggle = onAutoToggle,
                                        onACPowerToggle = onACPowerToggle,
                                )
                                RadialSoundPanel(
                                        modifier =
                                                Modifier.weight(1f)
                                                        .fillMaxHeight()
                                                        .innerCard()
                                                        .padding(cardPad),
                                        volume = volume,
                                        navVolume = navVolume,
                                        alertVolume = alertVolume,
                                        phoneVolume = phoneVolume,
                                        voiceVolume = voiceVolume,
                                        onVolumeChange = onVolumeChange,
                                        onNavVolumeChange = onNavVolumeChange,
                                        onAlertVolumeChange = onAlertVolumeChange,
                                        onPhoneVolumeChange = onPhoneVolumeChange,
                                        onVoiceVolumeChange = onVoiceVolumeChange,
                                )
                            }
                    RadialSubMenu.Apps ->
                            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                                Box(modifier = Modifier.height(92.dp)) { AppSwitcherSection() }
                                DockAppsPanel(modifier = Modifier.fillMaxWidth())
                            }
                    RadialSubMenu.Driving ->
                            DockDrivingPanel(
                                    driveMode,
                                    powerModel,
                                    energyRecovery,
                                    steeringMode,
                            )
                    RadialSubMenu.Advanced -> DockAdvancedPanel()
                }
            }

            // Navegação fixa: Fechar | Painel | Condução | Apps | Avançado | Voltar.
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                RadialOuterRingButton(
                        label = "Fechar",
                        icon = Icons.Default.Close,
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick = handleClose,
                )
                RadialOuterRingButton(
                        label = "Painel",
                        icon = Icons.Default.Dashboard,
                        selected = BottomBarState.isDashboardExpanded,
                        modifier = Modifier.weight(1f),
                        onClick =
                                bumpAnd {
                                    // Abre o Impulse Dashboard (tela cheia). Ele é renderizado
                                    // dentro da janela overlay fullscreen do BottomBarService
                                    // (BottomBarMenus -> ExpandedImpulseDashboard), igual ao dock,
                                    // então o painel nativo de HVAC cai atrás e não rouba o foco.
                                    //
                                    // Fecha o dock (isVisible=false): o conteúdo desce e a alça
                                    // some (isDashboardExpanded=true também a oculta e zera a
                                    // região de toque da barra, então ela não recebe clique).
                                    // Ao fechar o dashboard (collapseDashboard), o dock permanece
                                    // fechado e só a alça reaparece.
                                    BottomBarState.closeRadialSubMenu()
                                    BottomBarState.isVisible = false
                                    BottomBarState.isDashboardExpanded = true
                                },
                )
                outerRingItems.forEach { item ->
                    val isSelected = item.subMenu != null && subMenu == item.subMenu
                    RadialOuterRingButton(
                            label = item.label,
                            icon = item.icon,
                            selected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick =
                                    bumpAnd {
                                        if (item.subMenu != null) {
                                            BottomBarState.radialSubMenu =
                                                    if (subMenu == item.subMenu) RadialSubMenu.None
                                                    else item.subMenu
                                        }
                                    },
                    )
                }
                RadialOuterRingButton(
                        label = "Voltar",
                        icon = Icons.AutoMirrored.Filled.Undo,
                        selected = false,
                        modifier = Modifier.weight(1f),
                        onClick =
                                bumpAnd {
                                    scope.launch(Dispatchers.IO) {
                                        ShizukuUtils.runCommandAndGetOutput(
                                                arrayOf("input", "keyevent", "4")
                                        )
                                    }
                                },
                )
            }
        }
        }
    }
}

@Composable
private fun RadialOuterRingButton(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
) {
    // Mesmo formato/aparência dos botões do dashboard (DashboardToggleButton):
    // retângulo arredondado (8dp), ícone + rótulo, acento ciano quando ativo.
    Surface(
            onClick = onClick,
            shape = RoundedCornerShape(8.dp),
            color =
                    if (selected) radialAccent.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.055f),
            border =
                    BorderStroke(
                            1.dp,
                            if (selected) radialAccent.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.08f),
                    ),
            modifier = modifier.height(76.dp),
    ) {
        Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                    icon,
                    label,
                    tint = if (selected) radialAccent else Color.White,
                    modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                    label,
                    color = if (selected) radialAccent else radialTextSecondary,
                    fontSize = radialNavLabelSize,
                    fontFamily = dockFont,
                    maxLines = 1,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Composable
private fun RadialClimatePanel(
        modifier: Modifier,
        driverTemp: String,
        passTemp: String,
        fanSpeed: Int,
        isACEnabled: Boolean,
        acSync: String,
        acAuto: String,
        onDriverTempChange: (Float) -> Unit,
        onPassTempChange: (Float) -> Unit,
        onFanChange: (Int) -> Unit,
        onSyncToggle: () -> Unit,
        onAutoToggle: () -> Unit,
        onACPowerToggle: () -> Unit,
) {
    val alpha = if (isACEnabled) 1f else 0.45f
    // Recuo lateral comum às linhas de temperatura e de ventilação, para os
    // controles dos extremos (Motorista/Passageiro e A/C/Auto) ficarem alinhados.
    val climateEdgePad = 8.dp
    Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Ar-condicionado", color = radialAccent, fontSize = radialTitleSize, fontFamily = dockFont, fontWeight = FontWeight.Bold)

        // Temperaturas (motorista / passageiro) com o Sync (ícone) centralizado entre elas.
        Row(
                modifier =
                        Modifier.fillMaxWidth()
                                .padding(horizontal = climateEdgePad)
                                .alpha(alpha),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            RadialTempStepper("Motorista", driverTemp, isACEnabled, onDriverTempChange)
            RadialIconToggle(
                    label = "Sync",
                    active = acSync == "1",
                    enabled = isACEnabled,
                    icon =
                            if (acSync == "1") Icons.Default.Link
                            else Icons.Default.LinkOff,
                    contentDescription = "Sincronizar temperaturas",
                    onClick = onSyncToggle,
            )
            RadialTempStepper("Passageiro", passTemp, isACEnabled, onPassTempChange)
        }

        RadialDivider()

        // A/C no extremo esquerdo, ventilação ao centro e Auto no extremo direito.
        // Mesmo recuo da borda usado pelos +/- das temperaturas acima.
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = climateEdgePad),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            RadialIconToggle(
                    label = "A/C",
                    active = isACEnabled,
                    enabled = true,
                    icon = Icons.Default.PowerSettingsNew,
                    contentDescription = "Ligar/desligar ar-condicionado",
                    onClick = onACPowerToggle,
            )
            Box(modifier = Modifier.alpha(alpha)) {
                RadialIntStepper(
                        "Ventilação",
                        fanSpeed,
                        true,
                        onFanChange,
                        deferToEnd = true,
                        range = 0..7,
                        pxPerStepDp = 34.dp,
                )
            }
            Box(modifier = Modifier.alpha(alpha)) {
                RadialToggleChip("Auto", acAuto == "1", isACEnabled, bumpAnd(onAutoToggle))
            }
        }
    }
}

@Composable
private fun RadialSoundPanel(
        modifier: Modifier,
        volume: Int,
        navVolume: Int,
        alertVolume: Int,
        phoneVolume: Int,
        voiceVolume: Int,
        onVolumeChange: (Int) -> Unit,
        onNavVolumeChange: (Int) -> Unit,
        onAlertVolumeChange: (Int) -> Unit,
        onPhoneVolumeChange: (Int) -> Unit,
        onVoiceVolumeChange: (Int) -> Unit,
) {
    Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text("Volume", color = radialAccent, fontSize = radialTitleSize, fontFamily = dockFont, fontWeight = FontWeight.Bold)
        RadialVolumeRow("Mídia", volume, onVolumeChange)
        RadialDivider()
        RadialVolumeRow("Navegação", navVolume, onNavVolumeChange, 14.sp, 22.sp, 34.dp)
        RadialDivider()
        RadialVolumeRow("Alertas", alertVolume, onAlertVolumeChange, 14.sp, 22.sp, 34.dp)
        RadialDivider()
        RadialVolumeRow("Telefone", phoneVolume, onPhoneVolumeChange, 14.sp, 22.sp, 34.dp)
        RadialDivider()
        RadialVolumeRow("Voz", voiceVolume, onVoiceVolumeChange, 14.sp, 22.sp, 34.dp)
    }
}

/** Linha de volume arrastável: título à esquerda, número à direita; arrasta-se a linha inteira. */
@Composable
private fun RadialVolumeRow(
        label: String,
        value: Int,
        onDelta: (Int) -> Unit,
        labelSize: TextUnit = radialVolumeLabelSize,
        valueSize: TextUnit = radialVolumeValueSize,
        minHeight: Dp = 46.dp,
) {
    RadialDraggableValue(
            enabled = true,
            onIncrement = { onDelta(1) },
            onDecrement = { onDelta(-1) },
            modifier = Modifier.fillMaxWidth(),
            minWidth = 0.dp,
            minHeight = minHeight,
    ) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                    label,
                    fontSize = labelSize,
                    fontFamily = dockFont,
                    color = radialTextSecondary,
            )
            Text(
                    value.toString(),
                    fontSize = valueSize,
                    fontFamily = dockFont,
                    fontWeight = FontWeight.Bold,
                    color = radialTextPrimary,
            )
        }
    }
}

@Composable
private fun RadialIconToggle(
        label: String,
        active: Boolean,
        enabled: Boolean,
        icon: ImageVector,
        contentDescription: String,
        onClick: () -> Unit,
) {
    val accentActive = active && enabled
    val tint =
            when {
                !enabled -> Color.White.copy(alpha = 0.3f)
                active -> radialAccent
                else -> Color.White
            }
    // Mesmo formato/aparência do DashboardToggleButton: retângulo arredondado (8dp),
    // ícone + rótulo, acento ciano quando ativo.
    Surface(
            onClick = if (enabled) bumpAnd(onClick) else ({}),
            shape = RoundedCornerShape(8.dp),
            color =
                    if (accentActive) radialAccent.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.055f),
            border =
                    BorderStroke(
                            1.dp,
                            if (accentActive) radialAccent.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.08f),
                    ),
            modifier = Modifier.height(76.dp).widthIn(min = 76.dp),
    ) {
        Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                    label,
                    style =
                            dockLabelStyle.copy(
                                    fontSize = radialLabelSize,
                                    color =
                                            if (accentActive) radialAccent
                                            else radialTextSecondary,
                            ),
            )
        }
    }
}

@Composable
private fun RadialDivider() {
    Box(
            modifier =
                    Modifier.fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.1f))
    )
}

@Composable
private fun RadialTempStepper(
        label: String,
        temp: String,
        isEnabled: Boolean,
        onDelta: (Float) -> Unit,
) {
    val floatTemp = temp.toFloatOrNull() ?: -200f
    val isAbnormal = floatTemp >= 85f || floatTemp <= -40f || floatTemp == -1f
    val displayTemp = if (!isEnabled || isAbnormal) "--" else temp
    val dragEnabled = isEnabled && displayTemp != "--"
    // Passos de pré-visualização durante o arrasto (cada passo = 0.5°). Só aplica ao soltar.
    // Limites iguais aos do cluster (AcControlScreen): 16°C a 32°C.
    var previewSteps by remember(temp) { mutableIntStateOf(0) }
    val baseTemp = temp.toFloatOrNull() ?: 0f
    val previewTemp = (baseTemp + previewSteps * 0.5f).coerceIn(TEMP_MIN, TEMP_MAX)
    val shownColorTemp = if (previewSteps != 0) previewTemp else floatTemp
    val shownText =
            when {
                displayTemp == "--" -> "--"
                previewSteps != 0 -> String.format(java.util.Locale.US, "%.1f", previewTemp)
                else -> displayTemp
            }
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = dockLabelStyle.copy(fontSize = radialLabelSize, color = radialTextSecondary))
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RadialMiniButton(dragEnabled, Icons.Default.Remove) { onDelta(-0.5f) }
            RadialDraggableValue(
                    enabled = dragEnabled,
                    onIncrement = { onDelta(0.5f) },
                    onDecrement = { onDelta(-0.5f) },
                    deferToEnd = true,
                    onPreviewSteps = { previewSteps = it },
                    onCommitSteps = { steps ->
                        // Aplica o delta já limitado a 16–32 (1 comando ao carro).
                        val target = (baseTemp + steps * 0.5f).coerceIn(TEMP_MIN, TEMP_MAX)
                        val appliedDelta = target - baseTemp
                        if (appliedDelta != 0f) onDelta(appliedDelta)
                        previewSteps = 0
                    },
            ) {
                Text(
                        buildAnnotatedString {
                            withStyle(
                                    SpanStyle(
                                            color =
                                                    if (shownColorTemp > 30f) Color.Red
                                                    else Color.White
                                    )
                            ) { append(shownText) }
                            if (shownText != "--") append("°")
                        },
                        fontSize = radialValueSize,
                        fontFamily = dockFont,
                        fontWeight = FontWeight.Bold,
                        color = radialTextPrimary
                )
            }
            RadialMiniButton(dragEnabled, Icons.Default.Add) { onDelta(0.5f) }
        }
    }
}

@Composable
private fun RadialIntStepper(
        label: String,
        value: Int,
        enabled: Boolean,
        onDelta: (Int) -> Unit,
        labelSize: TextUnit = radialLabelSize,
        valueSize: TextUnit = radialValueSize,
        buttonSize: Dp = 50.dp,
        valueMinWidth: Dp = 100.dp,
        valueMinHeight: Dp = 78.dp,
        rowSpacing: Dp = 6.dp,
        labelSpacing: Dp = 2.dp,
        deferToEnd: Boolean = false,
        range: IntRange? = null,
        pxPerStepDp: Dp = 9.dp,
        trailing: @Composable (() -> Unit)? = null,
) {
    // Pré-visualização durante o arrasto; só aplica ao soltar (1 comando ao carro).
    var previewSteps by remember(value) { mutableIntStateOf(0) }
    val shownValue =
            if (range != null) (value + previewSteps).coerceIn(range.first, range.last)
            else value + previewSteps
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(labelSpacing),
    ) {
        Text(label, style = dockLabelStyle.copy(fontSize = labelSize, color = radialTextSecondary))
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
        ) {
            RadialMiniButton(enabled, Icons.Default.Remove, size = buttonSize, iconSize = buttonSize * 0.5f) { onDelta(-1) }
            RadialDraggableValue(
                    enabled = enabled,
                    onIncrement = { onDelta(1) },
                    onDecrement = { onDelta(-1) },
                    minWidth = valueMinWidth,
                    minHeight = valueMinHeight,
                    deferToEnd = deferToEnd,
                    pxPerStepDp = pxPerStepDp,
                    onPreviewSteps = { previewSteps = it },
                    onCommitSteps = { steps ->
                        val target =
                                if (range != null)
                                        (value + steps).coerceIn(range.first, range.last)
                                else value + steps
                        if (target != value) onDelta(target - value)
                        previewSteps = 0
                    },
            ) {
                Text(
                        shownValue.toString(),
                        fontSize = valueSize,
                        fontFamily = dockFont,
                        fontWeight = FontWeight.Bold,
                        color = radialTextPrimary
                )
            }
            RadialMiniButton(enabled, Icons.Default.Add, size = buttonSize, iconSize = buttonSize * 0.5f) { onDelta(1) }
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                trailing()
            }
        }
    }
}

@Composable
private fun RadialMiniButton(
        enabled: Boolean,
        icon: ImageVector,
        size: Dp = 50.dp,
        iconSize: Dp = 26.dp,
        onClick: () -> Unit,
) {
    // Mesmo formato/aparência do DashboardIconButton: quadrado arredondado (8dp),
    // fundo translúcido claro e borda sutil.
    Surface(
            onClick = if (enabled) bumpAnd(onClick) else ({}),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.08f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
            modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                    icon,
                    null,
                    tint = Color.White.copy(alpha = if (enabled) 1f else 0.3f),
                    modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun RadialDraggableValue(
        enabled: Boolean,
        onIncrement: () -> Unit,
        onDecrement: () -> Unit,
        modifier: Modifier = Modifier,
        minWidth: Dp = 100.dp,
        minHeight: Dp = 78.dp,
        deferToEnd: Boolean = false,
        pxPerStepDp: Dp = 9.dp,
        onPreviewSteps: (Int) -> Unit = {},
        onCommitSteps: (Int) -> Unit = {},
        content: @Composable () -> Unit,
) {
    val playSound = rememberRadialAdjustSound()
    var dragging by remember { mutableStateOf(false) }
    val pxPerStep = with(LocalDensity.current) { pxPerStepDp.toPx() }
    val onInc by rememberUpdatedState(onIncrement)
    val onDec by rememberUpdatedState(onDecrement)
    val onPreview by rememberUpdatedState(onPreviewSteps)
    val onCommit by rememberUpdatedState(onCommitSteps)
    val sound by rememberUpdatedState(playSound)

    Box(
            modifier =
                    modifier
                            .sizeIn(minWidth = minWidth, minHeight = minHeight)
                            .alpha(if (dragging) 0.88f else 1f)
                            .graphicsLayer {
                                transformOrigin = TransformOrigin(0.5f, 0.5f)
                                scaleX = if (dragging) 1.05f else 1f
                                scaleY = if (dragging) 1.05f else 1f
                            }
                            .pointerInput(enabled, pxPerStep, deferToEnd) {
                                if (!enabled) return@pointerInput
                                var pending = 0f
                                var netSteps = 0
                                val velocityTracker = VelocityTracker()
                                detectHorizontalDragGestures(
                                        onDragStart = {
                                            BottomBarState.bumpRadialActivity()
                                            pending = 0f
                                            netSteps = 0
                                            velocityTracker.resetTracking()
                                            dragging = true
                                            if (deferToEnd) onPreview(0)
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            if (deferToEnd) {
                                                // Aplica de uma só vez ao soltar (1 comando ao carro).
                                                onCommit(netSteps)
                                                netSteps = 0
                                            } else {
                                                val velocity =
                                                        velocityTracker.calculateVelocity().x
                                                val bonus =
                                                        (kotlin.math.abs(velocity) / 1200f)
                                                                .toInt()
                                                                .coerceIn(0, 3)
                                                repeat(bonus) {
                                                    if (velocity > 0) {
                                                        onInc()
                                                        sound()
                                                        BottomBarState.bumpRadialActivity()
                                                    } else if (velocity < 0) {
                                                        onDec()
                                                        sound()
                                                        BottomBarState.bumpRadialActivity()
                                                    }
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            dragging = false
                                            if (deferToEnd) {
                                                onPreview(0)
                                                netSteps = 0
                                            }
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            velocityTracker.addPosition(
                                                    change.uptimeMillis,
                                                    change.position
                                            )
                                            pending += dragAmount
                                            while (pending <= -pxPerStep) {
                                                if (deferToEnd) {
                                                    netSteps -= 1
                                                    onPreview(netSteps)
                                                } else {
                                                    onDec()
                                                }
                                                sound()
                                                BottomBarState.bumpRadialActivity()
                                                pending += pxPerStep
                                            }
                                            while (pending >= pxPerStep) {
                                                if (deferToEnd) {
                                                    netSteps += 1
                                                    onPreview(netSteps)
                                                } else {
                                                    onInc()
                                                }
                                                sound()
                                                BottomBarState.bumpRadialActivity()
                                                pending -= pxPerStep
                                            }
                                        }
                                )
                            },
            contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun rememberRadialAdjustSound(): () -> Unit {
    val view = LocalView.current
    return remember(view) { { view.playSoundEffect(SoundEffectConstants.CLICK) } }
}

@Composable
private fun RadialToggleChip(label: String, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
            onClick = if (enabled) onClick else ({}),
            shape = RoundedCornerShape(8.dp),
            color =
                    if (active && enabled) radialAccent.copy(alpha = 0.16f)
                    else Color.White.copy(alpha = 0.055f),
            border =
                    BorderStroke(
                            1.dp,
                            if (active && enabled) radialAccent.copy(alpha = 0.55f)
                            else Color.White.copy(alpha = 0.08f)
                    )
    ) {
        Text(
                label,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                color = if (active && enabled) radialAccent else radialTextPrimary,
                fontSize = radialChipSize,
                fontFamily = dockFont,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )
    }
}

/** Abra em Android Studio: Split / Design → preview deste ficheiro. */
@Preview(
        name = "Menu radial (aberto)",
        widthDp = 960,
        heightDp = 400,
        showBackground = true,
        backgroundColor = 0xFF2A2A2A
)
@Composable
private fun RadialMenuPreview() {
    BottomBarState.radialSubMenu = RadialSubMenu.None
    HavalShisukuTheme {
        Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.BottomCenter
        ) {
            // Simula mapa/app por baixo do menu translúcido
            Text(
                    "App / mapa visível por baixo",
                    color = Color.White.copy(alpha = 0.25f),
                    modifier = Modifier.align(Alignment.Center)
            )
            RadialMenuContent(
                    scope = rememberCoroutineScope(),
                    driverTemp = "22.0",
                    passTemp = "21.0",
                    volume = 12,
                    navVolume = 10,
                    alertVolume = 8,
                    phoneVolume = 6,
                    voiceVolume = 5,
                    fanSpeed = 4,
                    isACEnabled = true,
                    acSync = "0",
                    acAuto = "1",
                    driveMode = "0",
                    powerModel = "0",
                    energyRecovery = "0",
                    steeringMode = "0",
                    onDriverTempChange = {},
                    onPassTempChange = {},
                    onFanChange = {},
                    onVolumeChange = {},
                    onNavVolumeChange = {},
                    onAlertVolumeChange = {},
                    onPhoneVolumeChange = {},
                    onVoiceVolumeChange = {},
                    onSyncToggle = {},
                    onAutoToggle = {},
                    onACPowerToggle = {},
            )
        }
    }
}

@Preview(
        name = "Menu radial (Apps)",
        widthDp = 960,
        heightDp = 520,
        showBackground = true,
        backgroundColor = 0xFF2A2A2A
)
@Composable
private fun RadialMenuAppsPreview() {
    BottomBarState.radialSubMenu = RadialSubMenu.Apps
    HavalShisukuTheme {
        Box(
                modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)),
                contentAlignment = Alignment.BottomCenter
        ) {
            RadialMenuContent(
                    scope = rememberCoroutineScope(),
                    driverTemp = "22.0",
                    passTemp = "21.0",
                    volume = 12,
                    navVolume = 10,
                    alertVolume = 8,
                    phoneVolume = 6,
                    voiceVolume = 5,
                    fanSpeed = 4,
                    isACEnabled = true,
                    acSync = "1",
                    acAuto = "0",
                    driveMode = "0",
                    powerModel = "0",
                    energyRecovery = "0",
                    steeringMode = "0",
                    onDriverTempChange = {},
                    onPassTempChange = {},
                    onFanChange = {},
                    onVolumeChange = {},
                    onNavVolumeChange = {},
                    onAlertVolumeChange = {},
                    onPhoneVolumeChange = {},
                    onVoiceVolumeChange = {},
                    onSyncToggle = {},
                    onAutoToggle = {},
                    onACPowerToggle = {},
            )
        }
    }
}
