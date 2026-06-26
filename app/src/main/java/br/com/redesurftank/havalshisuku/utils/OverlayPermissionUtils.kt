package br.com.redesurftank.havalshisuku.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

object OverlayPermissionUtils {
    private const val TAG = "OverlayPermissionUtils"

    fun canDrawOverlays(context: Context): Boolean =
            Settings.canDrawOverlays(context.applicationContext)

    fun resolvePackageName(context: Context): String? {
        return sequenceOf(context.packageName, context.applicationContext.packageName)
                .firstOrNull { !it.isNullOrBlank() }
    }

    /** Opens the best available settings screen for overlay permission. */
    fun requestOverlayPermission(context: Context): Boolean {
        val appContext = context.applicationContext
        val packageName = resolvePackageName(context)
        if (packageName == null) {
            Log.e(TAG, "Could not resolve package name for overlay permission intent")
            return openGenericSettings(context)
        }

        val candidates =
                listOf(
                        Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                        ),
                        Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:$packageName")
                        ),
                )

        for (intent in candidates) {
            if (launchIfResolvable(context, appContext, intent)) {
                return true
            }
        }

        return openGenericSettings(context)
    }

    private fun launchIfResolvable(
            context: Context,
            appContext: Context,
            intent: Intent,
    ): Boolean {
        return try {
            if (appContext.packageManager.resolveActivity(intent, 0) == null) {
                false
            } else {
                if (context !is android.app.Activity) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open overlay settings via ${intent.action}", e)
            false
        }
    }

    private fun openGenericSettings(context: Context): Boolean {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS)
            if (context !is android.app.Activity) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open generic settings for overlay permission", e)
            false
        }
    }
}
