package br.com.redesurftank;

import android.app.Application;
import android.content.Context;
import android.content.Intent;

import br.com.redesurftank.havalshisuku.BuildConfig;
import br.com.redesurftank.havalshisuku.diagnostics.ClusterPersistentEventLogger;
import br.com.redesurftank.havalshisuku.managers.CarMockManager;
import br.com.redesurftank.havalshisuku.services.ForegroundService;

public class App extends Application {

    private static Application sApplication;
    private static Context deviceProtectedContext;

    public static Application getApplication() {
        return sApplication;
    }

    public static Context getContext() {
        return getApplication().getApplicationContext();
    }

    public synchronized static Context getDeviceProtectedContext() {
        if (deviceProtectedContext == null) {
            deviceProtectedContext = getApplication().createDeviceProtectedStorageContext();
        }
        return deviceProtectedContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = this;
        ClusterPersistentEventLogger.logText(
                "app_start",
                "versionCode=" + BuildConfig.VERSION_CODE + " versionName=" + BuildConfig.VERSION_NAME
        );
        br.com.redesurftank.havalshisuku.managers.DisplayAppLauncher.ensureDefaultDesktopShortcuts();
        CarMockManager.ensureEnabledForEmulator(this);

        // Semeia os temas embutidos (Basic/Basic Light) para ficarem selecionaveis offline
        // e define o tema escuro classico (Basic) como padrao quando o usuario ainda nao escolheu.
        new Thread(() -> {
            try {
                br.com.redesurftank.havalshisuku.managers.ThemeManager.Companion
                        .getInstance(getContext())
                        .seedBundledThemes();

                android.content.SharedPreferences prefs = getDeviceProtectedContext()
                        .getSharedPreferences("haval_prefs", Context.MODE_PRIVATE);
                if (!prefs.contains(br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys.VIRTUAL_CLUSTER_THEME.getKey())) {
                    prefs.edit()
                            .putString(br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys.VIRTUAL_CLUSTER_THEME.getKey(), "Basic")
                            .putString(br.com.redesurftank.havalshisuku.models.SharedPreferencesKeys.ACTIVE_CUSTOM_THEME.getKey(), "Basic")
                            .apply();
                }
            } catch (Exception ignored) {
            }
        }).start();

        var context = getContext();
        Intent serviceIntent = new Intent(context, ForegroundService.class);
        context.startForegroundService(serviceIntent);
    }
}
