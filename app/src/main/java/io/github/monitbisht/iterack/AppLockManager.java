package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLockManager {

    private final SharedPreferences prefs;

    // Constructor: Opens the specific preference file for the logged-in user
    public AppLockManager(Context context, String uid) {
        String name = (uid == null) ? "APP_LOCK_PREFS_global" : "APP_LOCK_PREFS_" + uid;
        prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
    }

    // Check if App Lock is enabled for this user
    public boolean isEnabled() {
        return prefs.getBoolean("enabled", false);
    }

    // Toggle App Lock on/off
    public void setEnabled(boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).apply();
    }

    // Check if this is the first time setting up the lock
    public boolean isFirstTime() {
        return prefs.getBoolean("first_time", true);
    }

    // Mark setup as complete
    public void setFirstTimeDone() {
        prefs.edit().putBoolean("first_time", false).apply();
    }

    // Check if Biometric (Fingerprint/Face) is enabled
    public boolean biometrics() {
        return prefs.getBoolean("biometric_enabled", false);
    }

    // Toggle Biometric authentication
    public void setBiometrics(boolean enabled) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply();
    }

    // Check if Device Credential (PIN/Pattern) is enabled
    public boolean deviceCredential() {
        return prefs.getBoolean("device_credential_enabled", false);
    }

    // Toggle Device Credential authentication
    public void setDeviceCredential(boolean enabled) {
        prefs.edit().putBoolean("device_credential_enabled", enabled).apply();
    }

    // Wipes all settings (Useful for logout/reset)
    public void clearAll() {
        prefs.edit().clear().apply();
    }
}