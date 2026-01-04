package io.github.monitbisht.iterack;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import com.google.firebase.auth.FirebaseAuth;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private boolean allowBiometric;
    private boolean allowDeviceCredential;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Transparent black background while system dialog is shown
        View view = new View(this);
        view.setBackgroundColor(getColor(android.R.color.black));
        setContentView(view);

        loadPreferences();
        authenticate();
    }

    private void loadPreferences() {
        String uid = FirebaseAuth.getInstance().getUid();
        String prefName = (uid == null) ? "APP_LOCK_PREFS_global" : "APP_LOCK_PREFS_" + uid;
        SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);

        allowBiometric = prefs.getBoolean("biometric_enabled", false);
        allowDeviceCredential = prefs.getBoolean("device_credential_enabled", false);
    }

    private void authenticate() {
        int allowedAuth = 0;
        if (allowBiometric) allowedAuth |= BiometricManager.Authenticators.BIOMETRIC_STRONG;
        if (allowDeviceCredential) allowedAuth |= BiometricManager.Authenticators.DEVICE_CREDENTIAL;

        if (allowedAuth == 0) {
            // nothing allowed
            finish();
            return;
        }

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock App")
                .setSubtitle("Authenticate to continue")
                .setAllowedAuthenticators(allowedAuth)
                .build();

        Executor executor = getMainExecutor();

        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {

                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        unlockSuccess();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // stay on screen
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {

                        if (allowDeviceCredential &&
                                (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                        errorCode == BiometricPrompt.ERROR_CANCELED)) {
                            unlockSuccess();
                            return;
                        }

                        // If user cancelled or negative button, finish (stay locked)
                        if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                errorCode == BiometricPrompt.ERROR_CANCELED) {
                            finish();
                        }
                    }
                });

        biometricPrompt.authenticate(promptInfo);
    }

    private void unlockSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Prevent leaving the lock screen
    }
}
