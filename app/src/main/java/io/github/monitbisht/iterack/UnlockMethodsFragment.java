package io.github.monitbisht.iterack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.biometric.BiometricManager;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;


public class UnlockMethodsFragment extends Fragment {

    private SwitchMaterial switchBiometric, switchDevicePasscode;
    private AppCompatButton saveButton;

    public UnlockMethodsFragment() {  }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_unlock_methods, container, false);

        switchBiometric = view.findViewById(R.id.switchBiometric);
        switchDevicePasscode = view.findViewById(R.id.switchDevicePasscode);
        saveButton = view.findViewById(R.id.save_unlock_prefs_button);

        // Verify device capabilities before showing options
        checkHardware();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        saveButton.setOnClickListener(v -> savePreferences());
    }

    // Checks if the device actually has fingerprint/face hardware
    private void checkHardware() {
        BiometricManager manager = BiometricManager.from(requireContext());
        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        // Disable biometric switch if hardware is missing or unavailable
        if (result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            switchBiometric.setEnabled(false);
            switchBiometric.setChecked(false);
        }
    }

    // Saves user choices and enables the lock
    private void savePreferences() {

        boolean useBio = switchBiometric.isChecked();
        boolean useDevice = switchDevicePasscode.isChecked();

        // Validation: Must select at least one method
        if (!useBio && !useDevice) {
            Toast.makeText(requireContext(), "Select at least one unlock method", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        AppLockManager lockManager = new AppLockManager(requireContext(), uid);

        lockManager.setBiometrics(useBio);
        lockManager.setDeviceCredential(useDevice);

        // Mark setup as complete so this screen doesn't show again automatically
        lockManager.setFirstTimeDone();
        lockManager.setEnabled(true);

        Toast.makeText(requireContext(), "Unlock methods saved", Toast.LENGTH_SHORT).show();

        // Return to Profile screen to show updated state
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new ProfileFragment())
                .commit();
    }
}