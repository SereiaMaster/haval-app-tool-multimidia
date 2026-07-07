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
 * Tela de teste (opt-in, avançada) para experimentar o controle da luz ambiente NATIVA do carro.
 *
 * O objetivo é descobrir se o veículo aceita troca de cor/brilho/modo via as chaves
 * `car.light_setting.ambient_light.*`. Como o formato do valor de cor não é conhecido, a tela
 * primeiro LÊ os valores atuais (para revelarmos o formato) e depois permite ESCREVER valores de
 * teste — inclusive escrita livre de qualquer chave/valor.
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

private val CardBg = Color(0xFF13151A)
private val SectionBg = Color(0xFF2A2F37)
private val Accent = Color(0xFF4A9EFF)

@Composable
fun AmbientTestTab() {
    val scope = rememberCoroutineScope()
    val values = remember { mutableStateMapOf<String, String>() }
    var busy by remember { mutableStateOf(false) }
    var lastAction by remember { mutableStateOf("") }

    // Alvo da escrita de cor: config "estática" ou "cor". Trocamos para descobrir qual funciona.
    var colorTargetStatic by remember { mutableStateOf(true) }
    var customKey by remember {
        mutableStateOf(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_MULTICOLOR_COLOR_CONFIG.value)
    }
    var customValue by remember { mutableStateOf("") }

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
            "Luz Ambiente (teste)",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Experimental: escreve nas chaves nativas car.light_setting.ambient_light.*. " +
                "Se a sua luz ambiente for monocromática (azul fixo por hardware), a troca de cor " +
                "pode não ter efeito. Comece lendo os valores atuais.",
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

        // ---- Ler valores atuais ----
        Section("1) Valores atuais") {
            Button(
                onClick = { readAll() },
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) { Text("Ler valores atuais", color = Color.White) }

            if (values.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                values.toList().sortedBy { it.first }.forEach { (k, v) ->
                    Text(
                        "${k.removePrefix("car.light_setting.ambient_light.")}: $v",
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }

        // ---- Ligar / brilho / dinâmico ----
        Section("2) Ligar / Brilho / Modo") {
            RowButtons {
                TestButton("Ligar") {
                    write(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_ENABLE.value, "1")
                }
                TestButton("Desligar") {
                    write(CarConstants.CAR_LIGHT_SETTING_AMBIENT_LIGHT_ENABLE.value, "0")
                }
            }
            Spacer(Modifier.height(8.dp))
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
        Section("3) Cor (experimental)") {
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
        Section("4) Escrita livre (qualquer chave/valor)") {
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
