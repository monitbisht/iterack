package io.github.monitbisht.iterack;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


public class ChangePasswordFragment extends Fragment {

    private TextInputEditText currentPasswordTextInput, newPasswordTextInput, confirmPasswordTextInput;
    private TextInputLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private AppCompatButton updatePasswordButton;
    private OnBackPressedCallback backCallback;

    public ChangePasswordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_change_password, container, false);

        // Bind UI elements
        currentPasswordTextInput = view.findViewById(R.id.et_current_password);
        newPasswordTextInput = view.findViewById(R.id.et_new_password);
        confirmPasswordTextInput = view.findViewById(R.id.et_confirm_password);

        currentPasswordLayout = view.findViewById(R.id.layout_current_password);
        newPasswordLayout = view.findViewById(R.id.layout_new_password);
        confirmPasswordLayout = view.findViewById(R.id.layout_confirm_password);

        updatePasswordButton = view.findViewById(R.id.update_password_button);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Back Press Callback (Initially disabled)
        backCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                // Do nothing while disabled (prevents exit during loading)
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backCallback);

        // Text Watchers: Clear errors and restore password toggle icon when user types
        currentPasswordTextInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPasswordTextInput.setError(null);
                currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        newPasswordTextInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newPasswordTextInput.setError(null);
                newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        confirmPasswordTextInput.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPasswordTextInput.setError(null);
                confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Update Button Click Listener
        updatePasswordButton.setOnClickListener(v -> {
            updatePassword();
        });
    }

    // Main Logic: Validates input and calls Firebase to change password
    private void updatePassword() {
        String currentPassword = currentPasswordTextInput.getText().toString().trim();
        String newPassword = newPasswordTextInput.getText().toString().trim();
        String confirmPassword = confirmPasswordTextInput.getText().toString().trim();

        // Validation: Check for empty fields and complexity requirements
        if (currentPassword.isEmpty()) {
            currentPasswordTextInput.setError("Password is required");
            currentPasswordTextInput.requestFocus();
            currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else if (!isValidPassword(currentPassword)) {
            currentPasswordTextInput.setError("Password must be 8+ chars, include a number & a symbol");
            currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else {
            currentPasswordTextInput.setError(null);
            currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        if (newPassword.isEmpty()) {
            newPasswordTextInput.setError("Password is required");
            newPasswordTextInput.requestFocus();
            newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else if (!isValidPassword(newPassword)) {
            newPasswordTextInput.setError("Password must be 8+ chars, include a number & a symbol");
            newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else {
            newPasswordTextInput.setError(null);
            newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        if (confirmPassword.isEmpty()) {
            confirmPasswordTextInput.setError("Confirm Password is required");
            confirmPasswordTextInput.requestFocus();
            confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else{
            confirmPasswordTextInput.setError(null);
            confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        // Logic Check: Passwords must match
        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordTextInput.setError("Password does not match");
            confirmPasswordTextInput.requestFocus();
            confirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        }

        // Logic Check: New password cannot be the same as old
        if (currentPassword.equals(newPassword)) {
            newPasswordTextInput.setError("New password must be different from current password");
            newPasswordTextInput.requestFocus();
            newPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        }


        showLoadingState(); // Disable inputs & button

        // Disable back navigation while loading
        backCallback.setEnabled(true);
        updatePasswordButton.setEnabled(false);
        currentPasswordTextInput.setEnabled(false);
        newPasswordTextInput.setEnabled(false);
        confirmPasswordTextInput.setEnabled(false);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            hideLoadingState();
            backCallback.setEnabled(false);
            Snackbar.make(requireView(), "User authentication failed", Snackbar.LENGTH_LONG).show();
            return;
        }

        String userEmail = user.getEmail();
        if (userEmail == null) {
            hideLoadingState();
            backCallback.setEnabled(false);
            Snackbar.make(requireView(), "Cannot verify email. Try again later.", Snackbar.LENGTH_LONG).show();
            return;
        }

        // Re-authenticate user before allowing sensitive change
        AuthCredential credential =
                EmailAuthProvider.getCredential(userEmail, currentPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {

                    // Authentication Success: Proceed to update password
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(unused -> {

                                hideLoadingState();
                                showSuccessBanner(requireView(), "Password Updated Successfully!");

                                // Clear fields
                                currentPasswordTextInput.setText("");
                                newPasswordTextInput.setText("");
                                confirmPasswordTextInput.setText("");

                                // Re-enable back press BEFORE navigating
                                backCallback.setEnabled(false);

                                // Delay navigation slightly for UX
                                new Handler().postDelayed(() -> {
                                    requireActivity().getSupportFragmentManager()
                                            .beginTransaction()
                                            .replace(R.id.frame_layout, new ProfileFragment())
                                            .commit();
                                }, 700);

                            })
                            .addOnFailureListener(e -> {
                                // Update Failed (e.g., Weak Password, Server Error)
                                hideLoadingState();

                                backCallback.setEnabled(false);
                                updatePasswordButton.setEnabled(true);
                                currentPasswordTextInput.setEnabled(true);
                                newPasswordTextInput.setEnabled(true);
                                confirmPasswordTextInput.setEnabled(true);

                                Snackbar.make(requireView(),
                                        "Failed to update password: " + e.getMessage(),
                                        Snackbar.LENGTH_LONG).show();
                            });

                })
                .addOnFailureListener(e -> {
                    // Re-authentication Failed (Wrong Current Password)
                    hideLoadingState();

                    backCallback.setEnabled(false);
                    updatePasswordButton.setEnabled(true);
                    currentPasswordTextInput.setEnabled(true);
                    newPasswordTextInput.setEnabled(true);
                    confirmPasswordTextInput.setEnabled(true);

                    currentPasswordTextInput.setError("Incorrect password");
                    currentPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
                    currentPasswordTextInput.requestFocus();
                });

    }

    // Helper: Enforces Regex for password complexity
    boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[@#$%^&+=!]).{6,}$";
        return password != null && password.matches(passwordRegex);
    }

    // Helper: Updates UI to show loading
    private void showLoadingState() {
        updatePasswordButton.setEnabled(false);
        updatePasswordButton.setText("Updating...");
        updatePasswordButton.setAlpha(0.7f);
    }

    // Helper: Reverts UI from loading state
    private void hideLoadingState() {
        updatePasswordButton.setEnabled(true);
        updatePasswordButton.setText("UPDATE PASSWORD");
        updatePasswordButton.setAlpha(1f);
    }

    // Helper: Shows a styled green snackbar
    private void showSuccessBanner(View v, String message) {
        Snackbar snackbar = Snackbar.make(v, message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(getResources().getColor(R.color.dark_emerald));
        snackbar.setTextColor(getResources().getColor(R.color.fresh_green));
        snackbar.show();
    }
}