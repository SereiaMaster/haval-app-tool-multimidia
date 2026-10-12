package br.com.redesurftank.havalshisuku.managers

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import br.com.redesurftank.havalshisuku.utils.ShizukuUtils
import br.com.redesurftank.havalshisuku.utils.SilentApkInstall
import java.io.File
import java.io.FileOutputStream

/**
 * Instala, de forma silenciosa (via Shizuku), APKs companheiros que viajam EMPACOTADOS dentro do
 * Impulse (em `assets/companion_apps/`).
 *
 * Objetivo: "instalar junto com o Impulse quando atualizar". O Impulse sobe uma vez por ignição;
 * ao subir uma versão nova, este passo roda no boot e garante que o app companheiro esteja
 * presente. A regra é simples e segura: **só instala se o pacote NÃO estiver presente**. Se o dono
 * desinstalar depois, volta a instalar no próximo boot; se já estiver lá, não mexe.
 *
 * Não abre o instalador do sistema (o whitelist do GWM recusaria) — usa o mesmo caminho dos patches
 * (copia pra /data/local/tmp e `pm install -r -t` via Shizuku).
 */
object CompanionAppInstaller {

    private const val TAG = "CompanionAppInstaller"
    private const val ASSET_DIR = "companion_apps"

    /**
     * Apps empacotados. Chave = nome do pacote (precisa bater com o `package` do APK), valor = nome
     * do arquivo em `assets/companion_apps/`.
     */
    private val BUNDLED_APPS: Map<String, String> = linkedMapOf(
        "com.gwm.app.themestore" to "com.gwm.app.themestore.apk",
    )

    @Volatile
    private var alreadyRan = false

    /** Chamado no boot do serviço. Roda numa thread própria pois o `pm install` do Shizuku bloqueia. */
    @JvmStatic
    fun ensureInstalledAsync(context: Context) {
        if (alreadyRan) return
        alreadyRan = true
        val appContext = context.applicationContext
        Thread({ runCatching { ensureInstalledBlocking(appContext) }
            .onFailure { Log.e(TAG, "Companion install run failed", it) } },
            "companion-app-installer").apply { isDaemon = true }.start()
    }

    private fun ensureInstalledBlocking(context: Context) {
        if (!ShizukuUtils.isShizukuAvailable()) {
            Log.w(TAG, "Shizuku indisponível; instalação de apps companheiros adiada")
            return
        }
        val pm = context.packageManager
        for ((packageName, assetName) in BUNDLED_APPS) {
            try {
                if (isInstalled(pm, packageName)) {
                    Log.d(TAG, "Companion já instalado, pulando: $packageName")
                    continue
                }
                val staged = copyAssetToCache(context, assetName) ?: continue
                val ok = SilentApkInstall.install(staged, packageName)
                Log.w(TAG, "Companion install $packageName -> $ok")
                staged.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Falha instalando companion $packageName", e)
            }
        }
    }

    private fun isInstalled(pm: PackageManager, packageName: String): Boolean =
        try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }

    private fun copyAssetToCache(context: Context, assetName: String): File? {
        return try {
            val out = File(context.cacheDir, assetName)
            context.assets.open("$ASSET_DIR/$assetName").use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            out
        } catch (e: Exception) {
            Log.e(TAG, "Asset companheiro ausente: $ASSET_DIR/$assetName", e)
            null
        }
    }
}
