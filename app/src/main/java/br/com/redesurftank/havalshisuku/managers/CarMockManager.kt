package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.util.Log
import br.com.redesurftank.App
import br.com.redesurftank.havalshisuku.models.CarConstants
import br.com.redesurftank.havalshisuku.models.DisplayAppConfig
import br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys
import br.com.redesurftank.havalshisuku.utils.EmulatorUtils
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Dados simulados do veículo para desenvolvimento (emulador / UI sem head unit).
 * Ative em Configurações → "Modo mock local".
 */
object CarMockManager {
    private const val TAG = "CarMockManager"
    private const val PREFS_MOCK_CONFIGURED = "localCarMockConfigured"

    private val gson = Gson()
    private val values = linkedMapOf<String, String>()

    @JvmStatic
    fun isEnabled(context: Context = App.getDeviceProtectedContext()): Boolean {
        return context.getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
                .getBoolean(SharedPreferencesKeys.ENABLE_LOCAL_CAR_MOCK.key, false)
    }

    @JvmStatic
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
                .edit()
                .putBoolean(SharedPreferencesKeys.ENABLE_LOCAL_CAR_MOCK.key, enabled)
                .putBoolean(PREFS_MOCK_CONFIGURED, true)
                .apply()
        if (enabled) {
            loadOrSeed(context)
            seedDisplayAppsIfEmpty()
        }
    }

    /** Primeira execução no emulador: ativa mock automaticamente. */
    @JvmStatic
    fun ensureEnabledForEmulator(context: Context = App.getDeviceProtectedContext()) {
        if (!EmulatorUtils.isEmulator()) return
        val prefs = context.getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean(PREFS_MOCK_CONFIGURED, false)) return
        Log.i(TAG, "Emulator detected — enabling local car mock by default")
        setEnabled(context, true)
    }

    @JvmStatic
    fun bootstrap(serviceManager: ServiceManager, context: Context) {
        loadOrSeed(context)
        seedDisplayAppsIfEmpty()
        serviceManager.seedMockData(HashMap(values))
        Log.i(TAG, "Mock bootstrap with ${values.size} values")
    }

    @JvmStatic
    fun getValue(key: String): String? {
        return values[key] ?: defaultValues()[key]
    }

    @JvmStatic
    fun setValue(key: String, value: String, context: Context = App.getDeviceProtectedContext()) {
        values[key] = value
        persist(context)
    }

    @JvmStatic
    fun resetToDefaults(context: Context = App.getDeviceProtectedContext()) {
        values.clear()
        values.putAll(defaultValues())
        persist(context)
        val sm = ServiceManager.getInstance()
        if (sm.isServicesInitialized) {
            values.forEach { (k, v) -> sm.applyMockDataChange(k, v) }
        }
    }

    private fun loadOrSeed(context: Context) {
        val prefs = context.getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
        val json = prefs.getString(SharedPreferencesKeys.LOCAL_CAR_MOCK_DATA.key, null)
        values.clear()
        if (json != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val loaded: Map<String, String> = gson.fromJson(json, type)
                values.putAll(loaded)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load mock data, using defaults", e)
                values.putAll(defaultValues())
            }
        } else {
            values.putAll(defaultValues())
        }
        mergeMissingDefaults()
        persist(context)
    }

    private fun mergeMissingDefaults() {
        defaultValues().forEach { (key, value) ->
            if (!values.containsKey(key)) {
                values[key] = value
            }
        }
    }

    private fun persist(context: Context) {
        context.getSharedPreferences("haval_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString(SharedPreferencesKeys.LOCAL_CAR_MOCK_DATA.key, gson.toJson(values))
                .apply()
    }

    private fun seedDisplayAppsIfEmpty() {
        if (DisplayAppLauncher.getAllConfigs().isNotEmpty()) return
        listOf(
                        DisplayAppConfig(
                                packageName = "com.google.android.apps.maps",
                                activityName = "com.google.android.maps.MapsActivity",
                                displayId = 0,
                                x = 0,
                                y = 0,
                                width = 1920,
                                height = 720,
                                substituteIcon = "nav",
                                customName = "Maps (mock)",
                        ),
                        DisplayAppConfig(
                                packageName = "com.google.android.youtube",
                                activityName = "com.google.android.youtube.app.honeycomb.Shell\$HomeActivity",
                                displayId = 0,
                                x = 0,
                                y = 0,
                                width = 1920,
                                height = 720,
                                substituteIcon = "video",
                                customName = "YouTube (mock)",
                        ),
                        DisplayAppConfig(
                                packageName = "com.android.settings",
                                activityName = "com.android.settings.Settings",
                                displayId = 0,
                                x = 0,
                                y = 0,
                                width = 1920,
                                height = 720,
                                substituteIcon = "settings",
                                customName = "Definições (mock)",
                        ),
                )
                .forEach { DisplayAppLauncher.saveConfig(it) }
        Log.i(TAG, "Seeded sample display app configs for mock mode")
    }

    private fun defaultValues(): Map<String, String> =
            mapOf(
                    CarConstants.CAR_HVAC_DRIVER_TEMPERATURE.getValue() to "22.0",
                    CarConstants.CAR_HVAC_PASS_TEMPERATURE.getValue() to "21.0",
                    CarConstants.CAR_HVAC_FAN_SPEED.getValue() to "4",
                    CarConstants.CAR_HVAC_POWER_MODE.getValue() to "1",
                    CarConstants.CAR_HVAC_SYNC_ENABLE.getValue() to "0",
                    CarConstants.CAR_HVAC_AUTO_ENABLE.getValue() to "1",
                    CarConstants.SYS_SETTINGS_AUDIO_MEDIA_VOLUME.getValue() to "12",
                    CarConstants.CAR_DRIVE_SETTING_DRIVE_MODE.getValue() to "0",
                    CarConstants.CAR_DRIVE_SETTING_ESP_ENABLE.getValue() to "1",
                    CarConstants.CAR_EV_SETTING_POWER_MODEL_CONFIG.getValue() to "0",
                    CarConstants.CAR_EV_SETTING_ENERGY_RECOVERY_LEVEL.getValue() to "0",
                    CarConstants.CAR_CONFIGURE_PEDAL_CONTROL_ENABLE.getValue() to "0",
                    CarConstants.CAR_DRIVE_SETTING_STEERING_WHEEL_ASSIST_MODE.getValue() to "0",
                    CarConstants.CAR_BASIC_VEHICLE_SPEED.getValue() to "65",
                    CarConstants.CAR_BASIC_OUTSIDE_TEMP.getValue() to "24.0",
                    CarConstants.CAR_BASIC_INSIDE_TEMP.getValue() to "22.0",
                    CarConstants.CAR_BASIC_GEAR_STATUS.getValue() to "4",
                    CarConstants.CAR_EV_INFO_CUR_BATTERY_POWER_PERCENTAGE.getValue() to "78",
            )
}
