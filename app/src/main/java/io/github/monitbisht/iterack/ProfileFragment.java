package io.github.monitbisht.iterack;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


public class ProfileFragment extends Fragment {

    private TextView profileName, profileEmail;
    private TextView changePasswordText , exportData , resetData;
    private CircleImageView profileImage;
    private ImageView changePasswordIcon, changePasswordArrow , exportDataIcon , resetDataIcon;

    private SwitchMaterial appLockSwitch, biometricSwitch, deviceCredentialSwitch;

    private SwitchMaterial taskReminderSwitch, soundSwitch;
    private ImageView biometricIcon, deviceIcon;
    private TextView biometricTitle, deviceCredentialTitle;

    private AppLockManager lockManager;

    private MaterialButton logoutButton;

    public ProfileFragment() {  }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        profileName = view.findViewById(R.id.profile_name_textView);
        profileEmail = view.findViewById(R.id.profile_email);
        profileImage = view.findViewById(R.id.profileImage);
        changePasswordText = view.findViewById(R.id.change_password_textView);
        changePasswordIcon = view.findViewById(R.id.change_password_icon);
        changePasswordArrow = view.findViewById(R.id.change_password_arrow);
        logoutButton = view.findViewById(R.id.logout_button);
        exportData = view.findViewById(R.id.export_data_textView);
        exportDataIcon = view.findViewById(R.id.export_data_icon);
        resetData = view.findViewById(R.id.reset_data_textView);
        resetDataIcon = view.findViewById(R.id.reset_data_icon);
        taskReminderSwitch = view.findViewById(R.id.task_reminder_switch);
        soundSwitch = view.findViewById(R.id.sound_switch);


        // app-lock related views
        appLockSwitch = view.findViewById(R.id.app_lock_toggle_button);
        biometricSwitch = view.findViewById(R.id.biometrics_toggle_button);
        deviceCredentialSwitch = view.findViewById(R.id.device_credential_toggle_button);

        biometricIcon = view.findViewById(R.id.biometrics_icon);
        deviceIcon = view.findViewById(R.id.device_credential_icon);

        biometricTitle = view.findViewById(R.id.biometrics_title);
        deviceCredentialTitle = view.findViewById(R.id.device_credential_title);

        // Per-user manager
        String uid = FirebaseAuth.getInstance().getUid();
        lockManager = new AppLockManager(requireContext(), uid);

        // set initial UI
        boolean enabled = lockManager.isEnabled();
        appLockSwitch.setChecked(enabled);
        setUnlockMethodsVisibility(enabled);

        biometricSwitch.setChecked(lockManager.biometrics());
        deviceCredentialSwitch.setChecked(lockManager.deviceCredential());

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // load other user data
        loadUserData();
        signOut();
        changePasswordText.setOnClickListener(passwordClickListener);
        changePasswordIcon.setOnClickListener(passwordClickListener);
        changePasswordArrow.setOnClickListener(passwordClickListener);



        //Notification Switch

        //Load the Saved State (Default to TRUE/ON)
        SharedPreferences prefs = requireContext().getSharedPreferences("USER_SETTINGS", Context.MODE_PRIVATE);
        boolean isEnabled = prefs.getBoolean("notifications_enabled", true);
        taskReminderSwitch.setChecked(isEnabled);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        soundSwitch.setChecked(isSoundEnabled);


        // Reminder Switch Listener
        taskReminderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            // Save the new state immediately
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply();

            if (isChecked) {
                NotificationScheduler.scheduleAll(requireContext());
                Toast.makeText(getContext(), "Notifications Enabled", Toast.LENGTH_SHORT).show();
            } else {
                NotificationScheduler.cancelAll(requireContext());
                Toast.makeText(getContext(), "Notifications Disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Sound Switch Listener
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sound_enabled", isChecked).apply();

            String msg = isChecked ? "Sound & Vibration Enabled" : "Sound & Vibration Muted";
            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
        });

        // App lock main toggle - require authentication to enable/disable
        appLockSwitch.setOnCheckedChangeListener((button, isChecked) -> {

            appLockSwitch.setOnCheckedChangeListener(null);
            appLockSwitch.setChecked(!isChecked);
            appLockSwitch.post(() -> appLockSwitch.setOnCheckedChangeListener((b, c) -> {})); // temporary noop

            if (isChecked) {
                // enabling
                if (lockManager.isFirstTime()) {
                    // First time -> open setup screens (user chooses methods)
                    openSetupFlow();
                    // leave UI unchanged here; after saving preferences the UI is updated
                    restoreAppLockListener();
                    return;
                }

                // not first time -> authenticate and enable
                authenticate(() -> {
                    lockManager.setEnabled(true);
                    // show method switches when enabled
                    setUnlockMethodsVisibility(true);
                    biometricSwitch.setChecked(lockManager.biometrics());
                    deviceCredentialSwitch.setChecked(lockManager.deviceCredential());
                    appLockSwitch.setChecked(true);
                    restoreAppLockListener();
                }, () -> {
                    // failed
                    Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show();
                    appLockSwitch.setChecked(false);
                    restoreAppLockListener();
                });

            } else {
                // disabling -> authenticate then set disabled
                authenticate(() -> {
                    lockManager.setEnabled(false);
                    setUnlockMethodsVisibility(false);
                    appLockSwitch.setChecked(false);
                    restoreAppLockListener();
                }, () -> {
                    // failed -> keep enabled
                    appLockSwitch.setChecked(true);
                    restoreAppLockListener();
                });
            }
        });


        biometricSwitch.setOnCheckedChangeListener((button, checked) -> {
            // If switches hidden then ignore
            if (biometricSwitch.getVisibility() != View.VISIBLE) return;

            // require auth
            authenticate(() -> {
                lockManager.setBiometrics(checked);
                biometricSwitch.setChecked(checked);
            }, () -> {
                biometricSwitch.setChecked(!checked); // revert
                Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            });
        });

        deviceCredentialSwitch.setOnCheckedChangeListener((button, checked) -> {
            if (deviceCredentialSwitch.getVisibility() != View.VISIBLE) return;

            authenticate(() -> {
                lockManager.setDeviceCredential(checked);
                deviceCredentialSwitch.setChecked(checked);
            }, () -> {
                deviceCredentialSwitch.setChecked(!checked);
                Toast.makeText(requireContext(), "Authentication required", Toast.LENGTH_SHORT).show();
            });
        });

        resetData.setOnClickListener(v -> {
            clearData();
        });

        resetDataIcon.setOnClickListener(v -> {
            clearData();
        });

        exportData.setOnClickListener(v -> {
            new ExportBottomSheet().show(
                    requireActivity().getSupportFragmentManager(),
                    "export_sheet"
            );
        });

        exportDataIcon.setOnClickListener(v -> {
            new ExportBottomSheet().show(
                    requireActivity().getSupportFragmentManager(),
                    "export_sheet"
            );
        });
    }

    private void restoreAppLockListener() {
        appLockSwitch.setOnCheckedChangeListener((button, isChecked) -> {
            // re-attach the logic by reloading the fragment view (simpler)
            // Simpler approach: just recreate fragment to read state — safe and minimal change.
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new ProfileFragment())
                    .commit();
        });
    }

    private void openSetupFlow() {
        // Open the setup fragment where user chooses unlock methods.
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, new AppLockEnableFragment())
                .addToBackStack(null)
                .commit();
    }

    private void setUnlockMethodsVisibility(boolean visible) {
        int v = visible ? View.VISIBLE : View.GONE;
        biometricSwitch.setVisibility(v);
        deviceCredentialSwitch.setVisibility(v);
        biometricIcon.setVisibility(v);
        deviceIcon.setVisibility(v);
        biometricTitle.setVisibility(v);
        deviceCredentialTitle.setVisibility(v);
    }

    // Authenticate user using BiometricPrompt + device credential fallback.
    // onSuccess / onFail are run on main thread.
        private void authenticate(Runnable onSuccess, Runnable onFail) {

            BiometricPrompt.PromptInfo promptInfo =
                    new BiometricPrompt.PromptInfo.Builder()
                            .setTitle("Confirm your identity")
                            .setSubtitle("Authentication required")
                            .setAllowedAuthenticators(
                                    BiometricManager.Authenticators.BIOMETRIC_STRONG |
                                            BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                            .build();

            BiometricPrompt prompt =
                    new BiometricPrompt(requireActivity(),
                            requireActivity().getMainExecutor(),
                            new BiometricPrompt.AuthenticationCallback() {

                                @Override
                                public void onAuthenticationSucceeded(
                                        @NonNull BiometricPrompt.AuthenticationResult result) {

                                    onSuccess.run();
                                }

                                @Override
                                public void onAuthenticationFailed() {
                                    // Biometric failures should allow fallback to PIN.
                                    Snackbar.make(requireView(), "Try again", Snackbar.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onAuthenticationError(int errorCode,
                                                                  @NonNull CharSequence errString) {

                                    // DEVICE CREDENTIAL SUCCESS = ERROR_CANCELED or ERROR_USER_CANCELED
                                    if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                                            errorCode == BiometricPrompt.ERROR_USER_CANCELED) {

                                        // Treat as success: user completed PIN authentication
                                        onSuccess.run();
                                        return;
                                    }

                                    // REAL FAILURES ONLY
                                    onFail.run();
                                }
                            });

            prompt.authenticate(promptInfo);
        }

    // password click listener
    private final View.OnClickListener passwordClickListener = v -> {
        if (isGoogleUser()) {
            Snackbar.make(v, "Google users must change password from Google Account", Snackbar.LENGTH_LONG).show();
        } else {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, new ChangePasswordFragment())
                    .addToBackStack(null)
                    .commit();
        }
    };


    // Sign out (clear cached data)
    private void signOut() {
        logoutButton.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { // 1. Sign out from Firebase
             FirebaseAuth.getInstance().signOut();

             // 2. Clear shared preferences (if user data is cached)
            SharedPreferences prefs = requireActivity().getSharedPreferences("userData", Context.MODE_PRIVATE);
            prefs.edit().clear().apply();

            // 3. Redirect to LoginActivity
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); startActivity(intent);
            requireActivity().finish(); } } );
    }

    private void loadUserData() {

        SharedPreferences prefs = requireContext().getSharedPreferences("USER_DATA", MODE_PRIVATE);
        String cachedName = prefs.getString("name", null);
        String cachedPhoto = prefs.getString("photoUrl", null);
        String cachedEmail = prefs.getString("email", null);

        if (cachedName != null) profileName.setText(cachedName);
        if (cachedEmail != null) profileEmail.setText(cachedEmail);

        if (cachedPhoto != null) {
            Glide.with(requireContext()).load(cachedPhoto).placeholder(R.drawable.profile).into(profileImage);
        }else {
            profileImage.setImageResource(R.drawable.profile_pic);
        }

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String name = doc.getString("name");
                    String photoUrl = doc.getString("photoUrl");
                    String email = doc.getString("email");

                    if (name != null) profileName.setText(name);
                    if (email != null) profileEmail.setText(email);
                    if (photoUrl != null) {
                        Glide.with(requireContext()).load(photoUrl).placeholder(R.drawable.profile).into(profileImage);
                    }else {
                        profileImage.setImageResource(R.drawable.profile_pic);
                    }
                })
                .addOnFailureListener(e -> Log.e("ProfileFragment", "Failed to fetch user data: " + e.getMessage()));
    }

    private void clearData(){

        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
                .setTitle("Reset All Data?")
                .setMessage("This will permanently delete all task's data, weekly stats, insights, and cached data. This action cannot be undone.")
                .setIcon(R.drawable.ic_warning)
                .setCancelable(true)
                .setPositiveButton("Reset", (dialog, which) -> {

                    // Run reset
                    new ResetDataManager(requireContext())
                            .resetAllData(new ResetDataManager.ResetCallback() {
                                @Override
                                public void onSuccess() {
                                    Snackbar.make(requireView(), "All data reset successfully", Snackbar.LENGTH_LONG).show();
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Snackbar.make(requireView(), "Reset failed: " + e.getMessage(), Snackbar.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private boolean isGoogleUser() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return false;
        for (UserInfo profile : user.getProviderData()) {
            if ("google.com".equals(profile.getProviderId())) return true;
        }
        return false;
    }
}
