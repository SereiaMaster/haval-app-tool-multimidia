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

private val radialAccent = Color(0xFF2196F3)
private val glassBase = Color(0xFF13151A)
private val glassEdge = Color.White.copy(alpha = 0.14f)

// --- Escala tipográfica/cores consistente da dock ---
private val radialTextPrimary = Color.White
private val radialTextSecondary = Color.White.copy(alpha = 0.62f)
private val radialTextMuted = Color.White.copy(alpha = 0.45f)

private val radialTitleSize = 16.sp // títulos de secção (Clima / Som)
private val radialValueSize = 32.sp // valores numéricos do clima (temp / ventilação) — grandes p/ tocar e arrastar
private val radialVolumeValueSize = 26.sp // número do volume (linha arrastável)
private val radialVolumeLabelSize = 15.sp // título de cada volume (linha arrastável)
private val radialLabelSize = 12.sp // rótulos (Motorista / Ventilação / etc.)
private val radialChipSize = 13.sp // texto de chips/toggles
private val radialNavLabelSize = 11.sp // rótulos da navegação inferior

/** Fração da largura do ecrã ocupada pela dock (responsivo head unit/emulador). */
internal const val DOCK_WIDTH_FRACTION = 0.5f

/** Superfície translúcida arredondada da dock principal. */
private fun Modifier.dockSurface(): Modifier =
        this.background(
                brush =
                        Brush.verticalGradient(
                                colors =
                                        listOf(
                                                Color(0xFF1B2230).copy(alpha = 0.94f),
                                                glassBase.copy(alpha = 0.97f),
                                        ),
                        ),
                shape = RoundedCornerShape(20.dp),
        ).border(1.dp, glassEdge, RoundedCornerShape(20.dp))

/** Cartão interno (Clima / Som) — mais leve que a superfície da dock. */
private fun Modifier.innerCard(): Modifier =
        this.background(
                color = Color.White.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
        ).border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))

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
) {
    val subMenu = BottomBarState.radialSubMenu
    val bodyScroll = rememberScrollState()

    BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            contentAlignment = Alignment.BottomStart,
    ) {
        // Dock no lado do motorista (esquerda), ocupando ~50% da largura.
        // Responsiva: cresce/encolhe conforme a largura real (emulador x carro).
        val dockWidth = (maxWidth * DOCK_WIDTH_FRACTION).coerceIn(420.dp, 900.dp)
        val uiScale = (dockWidth / 620.dp).coerceIn(0.9f, 1.2f)
        val pad = (14 * uiScale).dp
        val gap = (8 * uiScale).dp
        val cardPad = (10 * uiScale).dp
        val handleClose = bumpAnd { BottomBarState.hideRadialMenu() }

        Column(
                modifier =
                        Modifier.width(dockWidth)
                                .padding(start = (10 * uiScale).dp, bottom = (10 * uiScale).dp)
                                .dockSurface()
                                .padding(pad),
                verticalArrangement = Arrangement.spacedBy(gap),
        ) {
            // Corpo: controlos (Clima | Som) ou o submenu integrado na dock.
            Box(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(max = 320.dp)
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
                                Box(modifier = Modifier.height(64.dp)) { AppSwitcherSection() }
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

            // Navegação fixa: Fechar | Condução | Apps | Avançado | Voltar.
            Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
            ) {
                RadialOuterRingButton(
                        label = "Fechar",
                        icon = Icons.Default.Close,
                        selected = false,
                        onClick = handleClose,
                )
                outerRingItems.forEach { item ->
                    val isSelected = item.subMenu != null && subMenu == item.subMenu
                    RadialOuterRingButton(
                            label = item.label,
                            icon = item.icon,
                            selected = isSelected,
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

@Composable
private fun RadialOuterRingButton(
        label: String,
        icon: ImageVector,
        selected: Boolean,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        val iconSize = 46.dp
        Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
            if (selected) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                            color = radialAccent.copy(alpha = 0.35f),
                            radius = size.minDimension / 2f + 4f,
                            center = center,
                    )
                }
            }
            Surface(
                    onClick = onClick,
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier.matchParentSize()
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    drawCircle(
                            brush =
                                    Brush.radialGradient(
                                            colors =
                                                    if (selected) {
                                                        listOf(
                                                                radialAccent.copy(alpha = 0.7f),
                                                                Color(0xFF1A2840).copy(alpha = 0.95f),
                                                        )
                                                    } else {
                                                        listOf(
                                                                Color(0xFF3A4254).copy(alpha = 0.9f),
                                                                Color(0xFF10141C).copy(alpha = 0.95f),
                                                        )
                                                    },
                                    ),
                            radius = size.minDimension / 2f,
                            center = center,
                    )
                    drawCircle(
                            color = if (selected) radialAccent else glassEdge,
                            radius = size.minDimension / 2f,
                            center = center,
                            style = Stroke(width = if (selected) 2f else 1.2f),
                    )
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                            icon,
                            label,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Text(
                label,
                color = if (selected) radialAccent else radialTextSecondary,
                fontSize = radialNavLabelSize,
                maxLines = 1,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
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
) {
    val alpha = if (isACEnabled) 1f else 0.45f
    Column(
            modifier = modifier.alpha(alpha),
            verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Ar-condicionado", color = radialAccent, fontSize = radialTitleSize, fontWeight = FontWeight.Bold)

        // Temperaturas (motorista / passageiro) com o Sync (ícone) centralizado entre elas.
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
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

        // Ventilação centralizada, com o Auto na mesma linha dos botões +/-.
        Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
        ) {
            RadialIntStepper("Ventilação", fanSpeed, true, onFanChange) {
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
        Text("Volume", color = radialAccent, fontSize = radialTitleSize, fontWeight = FontWeight.Bold)
        RadialVolumeRow("Mídia", volume, onVolumeChange)
        RadialDivider()
        RadialVolumeRow("Navegação", navVolume, onNavVolumeChange, 12.sp, 18.sp, 28.dp)
        RadialDivider()
        RadialVolumeRow("Alertas", alertVolume, onAlertVolumeChange, 12.sp, 18.sp, 28.dp)
        RadialDivider()
        RadialVolumeRow("Telefone", phoneVolume, onPhoneVolumeChange, 12.sp, 18.sp, 28.dp)
        RadialDivider()
        RadialVolumeRow("Voz", voiceVolume, onVoiceVolumeChange, 12.sp, 18.sp, 28.dp)
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
        minHeight: Dp = 38.dp,
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
                    color = radialTextSecondary,
            )
            Text(
                    value.toString(),
                    fontSize = valueSize,
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
    val tint =
            when {
                !enabled -> Color.White.copy(alpha = 0.3f)
                active -> radialAccent
                else -> Color.White
            }
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
                label,
                style =
                        labelStyle.copy(
                                fontSize = radialLabelSize,
                                color = if (active && enabled) radialAccent else radialTextSecondary,
                        ),
        )
        Surface(
                onClick = if (enabled) bumpAnd(onClick) else ({}),
                shape = CircleShape,
                color =
                        if (active && enabled) radialAccent.copy(alpha = 0.22f)
                        else Color.Black.copy(alpha = 0.5f),
                border =
                        BorderStroke(
                                1.dp,
                                if (active && enabled) radialAccent else Color.Transparent,
                        ),
                modifier = Modifier.size(36.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription, tint = tint, modifier = Modifier.size(18.dp))
            }
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
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = labelStyle.copy(fontSize = radialLabelSize, color = radialTextSecondary))
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            RadialMiniButton(dragEnabled, Icons.Default.Remove) { onDelta(-0.5f) }
            RadialDraggableValue(
                    enabled = dragEnabled,
                    onIncrement = { onDelta(0.5f) },
                    onDecrement = { onDelta(-0.5f) },
            ) {
                Text(
                        buildAnnotatedString {
                            withStyle(
                                    SpanStyle(
                                            color =
                                                    if (floatTemp > 30f) Color.Red
                                                    else Color.White
                                    )
                            ) { append(displayTemp) }
                            if (displayTemp != "--") append("°")
                        },
                        fontSize = radialValueSize,
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
        buttonSize: Dp = 32.dp,
        valueMinWidth: Dp = 70.dp,
        valueMinHeight: Dp = 54.dp,
        rowSpacing: Dp = 6.dp,
        labelSpacing: Dp = 2.dp,
        trailing: @Composable (() -> Unit)? = null,
) {
    Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(labelSpacing),
    ) {
        Text(label, style = labelStyle.copy(fontSize = labelSize, color = radialTextSecondary))
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
            ) {
                Text(
                        value.toString(),
                        fontSize = valueSize,
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
        size: Dp = 32.dp,
        iconSize: Dp = 16.dp,
        onClick: () -> Unit,
) {
    Surface(
            onClick = if (enabled) bumpAnd(onClick) else ({}),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.5f),
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
        minWidth: Dp = 70.dp,
        minHeight: Dp = 54.dp,
        content: @Composable () -> Unit,
) {
    val playSound = rememberRadialAdjustSound()
    var dragging by remember { mutableStateOf(false) }
    val pxPerStep = with(LocalDensity.current) { 9.dp.toPx() }
    val onInc by rememberUpdatedState(onIncrement)
    val onDec by rememberUpdatedState(onDecrement)
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
                            .pointerInput(enabled, pxPerStep) {
                                if (!enabled) return@pointerInput
                                var pending = 0f
                                val velocityTracker = VelocityTracker()
                                detectHorizontalDragGestures(
                                        onDragStart = {
                                            BottomBarState.bumpRadialActivity()
                                            pending = 0f
                                            velocityTracker.resetTracking()
                                            dragging = true
                                        },
                                        onDragEnd = {
                                            dragging = false
                                            val velocity = velocityTracker.calculateVelocity().x
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
                                        },
                                        onDragCancel = { dragging = false },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            velocityTracker.addPosition(
                                                    change.uptimeMillis,
                                                    change.position
                                            )
                                            pending += dragAmount
                                            while (pending <= -pxPerStep) {
                                                onDec()
                                                sound()
                                                BottomBarState.bumpRadialActivity()
                                                pending += pxPerStep
                                            }
                                            while (pending >= pxPerStep) {
                                                onInc()
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
                    if (active && enabled) radialAccent.copy(alpha = 0.25f)
                    else Color.White.copy(alpha = 0.06f),
            border =
                    BorderStroke(
                            1.dp,
                            if (active && enabled) radialAccent else Color.Transparent
                    )
    ) {
        Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                color = if (active && enabled) radialAccent else radialTextPrimary,
                fontSize = radialChipSize,
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
            )
        }
    }
}
