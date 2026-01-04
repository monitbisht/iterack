package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * AppLockManager — per-user preference helper.
 * Use new AppLockManager(context, uid).
 * If uid == null, uses "APP_LOCK_PREFS_global".
 *
 * Keys:
 * - enabled (boolean)
 * - first_time (boolean)
 * - biometric_enabled (boolean)
 * - device_credential_enabled (boolean)
 */
public class AppLockManager {

    private final SharedPreferences prefs;

    public AppLockManager(Context context, String uid) {
        String name = (uid == null) ? "APP_LOCK_PREFS_global" : "APP_LOCK_PREFS_" + uid;
        prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        return prefs.getBoolean("enabled", false);
    }

    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).apply();
    }

    public boolean isFirstTime() {
        return prefs.getBoolean("first_time", true);
    }

    public void setFirstTimeDone() {
        prefs.edit().putBoolean("first_time", false).apply();
    }

    public boolean biometrics() {
        return prefs.getBoolean("biometric_enabled", false);
    }

    public void setBiometrics(boolean enabled) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply();
    }

    public boolean deviceCredential() {
        return prefs.getBoolean("device_credential_enabled", false);
    }

    public void setDeviceCredential(boolean enabled) {
        prefs.edit().putBoolean("device_credential_enabled", enabled).apply();
    }

    public void clearAll() {
        prefs.edit().clear().apply();
    }
}
