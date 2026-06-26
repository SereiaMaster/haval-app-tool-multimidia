package br.com.redesurftank.havalshisuku.utils

import android.os.Build

object EmulatorUtils {
    @JvmStatic
    fun isEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT.lowercase()
        val model = Build.MODEL.lowercase()
        val hardware = Build.HARDWARE.lowercase()
        val product = Build.PRODUCT.lowercase()
        val manufacturer = Build.MANUFACTURER.lowercase()

        return fingerprint.contains("generic") ||
                fingerprint.contains("emulator") ||
                fingerprint.contains("unknown") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk built for x86") ||
                manufacturer.contains("genymotion") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                product.contains("sdk") ||
                product.contains("emulator") ||
                product.contains("simulator")
    }
}
