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

        checkHardware();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        saveButton.setOnClickListener(v -> savePreferences());
    }

    private void checkHardware() {
        BiometricManager manager = BiometricManager.from(requireContext());
        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
        );

        if (result == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                result == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE) {
            switchBiometric.setEnabled(false);
            switchBiometric.setChecked(false);
        }
    }

    private void savePreferences() {

        boolean useBio = switchBiometric.isChecked();
        boolean useDevice = switchDevicePasscode.isChecked();

        if (!useBio && !useDevice) {
            Toast.makeText(requireContext(), "Select at least one unlock method", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        AppLockManager lockManager = new AppLockManager(requireContext(), uid);

        lockManager.setBiometrics(useBio);
        lockManager.setDeviceCredential(useDevice);

        // Mark first-time done and enable app lock
        lockManager.setFirstTimeDone();
        lockManager.setEnabled(true);

        Toast.makeText(requireContext(), "Unlock methods saved", Toast.LENGTH_SHORT).show();

        // Return to Profile (fresh instance reads updated prefs)
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new ProfileFragment())
                .commit();
    }
}
