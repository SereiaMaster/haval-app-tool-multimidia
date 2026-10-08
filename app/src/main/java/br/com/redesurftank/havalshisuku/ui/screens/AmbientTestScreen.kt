package br.com.redesurftank.havalshisuku.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.redesurftank.havalshisuku.managers.ServiceManager
import br.com.redesurftank.havalshisuku.models.CarConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Tela de teste/reparo da luz ambiente NATIVA do carro.
 *
 * O principal uso hoje é REPARO: alguns testes anteriores escreveram 0 (desligar) em uma zona
 * específica (por ex. a luz ambiente do motorista), e esse valor fica persistido no veículo.
 * A seção "Reparo por zona" permite reativar cada zona individualmente.
 *
 * As demais seções continuam servindo para experimentar brilho/cor/modo e escrita livre.
 */
private val AMBIENT_KEYS: List<CarConstants> = listOf(
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_ENABLE,
    CarConstants.CAR_LIGHT_SETTING_DRIVER_AMBIENT_LIGHT_ENABLE,
    CarConstants.CAR_LIGHT_SETTING_PASSENGER_AMBIENT_LIGHT_ENABLE,
    CarConstants.CAR_LIGHT_SETTING_REAR_ROW_AMBIENT_LIGHT_ENABLE,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_BRIGHTNESS,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_DYNAMIC_MODE,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_BREATHING_MODE_SWITCH,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_COLOR_CONFIG,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_STATIC_CONFIG,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_DYNAMIC_CONFIG,
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_PARTITION_CONTROL,
    CarConstants.CAR_CONFIGURE_MOOD_LAMP,
    CarConstants.CAR_CONFIGURE_MOOD_LAMP_PARTITION,
)

// Zonas que podem ser ligadas/desligadas individualmente (chave -> rótulo).
private val ZONE_KEYS: List<Pair<CarConstants, String>> = listOf(
    CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_ENABLE to "Global",
    CarConstants.CAR_LIGHT_SETTING_DRIVER_AMBIENT_LIGHT_ENABLE to "Motorista",
    CarConstants.CAR_LIGHT_SETTING_PASSENGER_AMBIENT_LIGHT_ENABLE to "Passageiro",
    CarConstants.CAR_LIGHT_SETTING_REAR_ROW_AMBIENT_LIGHT_ENABLE to "Traseira",
)

private val CardBg = Color(0xFF13151A)
private val SectionBg = Color(0xFF2A2F37)
private val Accent = Color(0xFF4A9EFF)
private val GreenOk = Color(0xFF2E7D32)

@Composable
fun AmbientTestTab() {
    val scope = rememberCoroutineScope()
    val values = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }

    // Alvo da escrita de cor: config "estática" ou "cor". Trocamos para descobrir qual funciona.
    var colorTargetStatic by remember { mutableStateOf(true) }
    var customKey by remember {
        mutableStateOf(CarConstants.CAR_LIGHT_SETTING_DRIVER_AMBIENT_LIGHT_ENABLE.value)
    }
    var customValue by remember { mutableStateOf("1") }

    fun write(key: String, value: String) {
        busy = true
        lastAction = "escrevendo $key = $value"
        scope.launch {
            withContext(Dispatchers.IO) {
                ServiceManager.getInstance().updateData(key, value)
                // Relê a chave logo após escrever para refletir o valor aceito pelo carro.
                val readback = ServiceManager.getInstance().getUpdatedData(key)
                withContext(Dispatchers.Main) {
                    if (readback != null) values[key] = readback
                }
            }
            lastAction = "OK: $key = $value"
            busy = false
        }
    }

    fun readAll() {
        busy = true
        lastAction = "lendo valores atuais..."
        scope.launch {
            withContext(Dispatchers.IO) {
                AMBIENT_KEYS.forEach { c ->
                    val v = ServiceManager.getInstance().getUpdatedData(c.value)
                    withContext(Dispatchers.Main) { values[c.value] = v ?: "(null)" }
                }
            }
            lastAction = "leitura concluída"
            busy = false
        }
    }

    fun colorTargetKey(): String =
        if (colorTargetStatic)
            CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_STATIC_CONFIG.value
        else
            CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_COLOR_CONFIG.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Luz Ambiente (teste / reparo)",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Se uma zona da luz ambiente ficou apagada (ex.: porta do motorista), use o " +
                "\"Reparo por zona\" para religá-la. Comece lendo os valores atuais para ver qual " +
                "zona está em 0 (desligada).",
            color = Color(0xFFB0B8C4),
            fontSize = 13.sp
        )

        if (lastAction.isNotEmpty()) {
            Text(
                (if (busy) "… " else "") + lastAction,
                color = if (busy) Accent else Color(0xFF7FD17F),
                fontSize = 12.sp
            )
        }

        // ---- Reparo por zona (principal) ----
        Section("Reparo por zona (ligar/desligar)") {
            Button(
                onClick = { readAll() },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Ler status atual", color = Color.White) }
            Spacer(Modifier.height(10.dp))
            ZONE_KEYS.forEach { (zoneKey, label) ->
                val current = values[zoneKey.value]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "status: " + (current ?: "—"),
                            color = Color(0xFFB0B8C4),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp
                        )
                    }
                    Button(
                        onClick = { write(zoneKey.value, "1") },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenOk),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Ligar", color = Color.White, fontSize = 13.sp) }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { write(zoneKey.value, "0") },
                        enabled = !busy,
                        colors = ButtonDefaults.buttonColors(containerColor = SectionBg),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) { Text("Desligar", color = Color.White, fontSize = 13.sp) }
                }
            }
        }

        // ---- Ler valores atuais ----
        Section("Valores atuais (todas as chaves)") {
            Button(
                onClick = { readAll() },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Ler valores atuais", color = Color.White) }

            if (values.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                values.toList().sortedBy { it.first }.forEach { (k, v) ->
                    Text(
                        "${k.removePrefix("car.light_setting.")}: $v",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // ---- Ligar / brilho / dinâmico ----
        Section("Brilho / Modo") {
            Text("Brilho", color = Color(0xFFB0B8C4), fontSize = 12.sp)
            RowButtons {
                listOf(0, 25, 50, 75, 100).forEach { b ->
                    TestButton(b.toString()) {
                        write(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_BRIGHTNESS.value, b.toString())
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            RowButtons {
                TestButton("Dinâmico ON") {
                    write(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_DYNAMIC_MODE.value, "1")
                }
                TestButton("Dinâmico OFF") {
                    write(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_DYNAMIC_MODE.value, "0")
                }
            }
        }

        // ---- Cor (experimental) ----
        Section("Cor (experimental)") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Alvo: " + if (colorTargetStatic) "STATIC_CONFIG" else "COLOR_CONFIG",
                    color = Color.White,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Switch(checked = colorTargetStatic, onCheckedChange = { colorTargetStatic = it })
            }
            Text(
                "Muitos carros usam um índice de paleta (0,1,2,…) em vez de RGB. " +
                    "Teste os índices abaixo e observe a cor; depois ajuste na escrita livre.",
                color = Color(0xFFB0B8C4),
                fontSize = 12.sp
            )
            Spacer(Modifier.height(8.dp))
            RowButtons {
                (0..7).forEach { idx ->
                    TestButton(idx.toString()) { write(colorTargetKey(), idx.toString()) }
                }
            }
            Spacer(Modifier.height(4.dp))
            RowButtons {
                (8..15).forEach { idx ->
                    TestButton(idx.toString()) { write(colorTargetKey(), idx.toString()) }
                }
            }
        }

        // ---- Escrita livre ----
        Section("Escrita livre (qualquer chave/valor)") {
            OutlinedTextField(
                value = customKey,
                onValueChange = { customKey = it },
                label = { Text("Chave") },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFB0B8C4),
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color(0xFF3A3F47),
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = Color(0xFFB0B8C4)
                )
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = customValue,
                onValueChange = { customValue = it },
                label = { Text("Valor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color(0xFFB0B8C4),
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Color(0xFF3A3F47),
                    focusedLabelColor = Accent,
                    unfocusedLabelColor = Color(0xFFB0B8C4)
                )
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { if (customKey.isNotBlank()) write(customKey.trim(), customValue) },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Escrever", color = Color.White) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

@Composable
private fun RowButtons(content: @Composable RowScope.() -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        content()
    }
}

@Composable
private fun TestButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = SectionBg),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) { Text(label, color = Color.White, fontSize = 13.sp) }
}
