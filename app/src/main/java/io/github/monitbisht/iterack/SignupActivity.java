package io.github.monitbisht.iterack;


import static android.content.ContentValues.TAG;
import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;


import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private TextInputEditText username, signupEmail, signupPassword, signupConfirmPassword;
    private TextInputLayout  signUpPasswordLayout, signUpConfirmPasswordLayout;
    private AppCompatButton signupBtn;
    private LinearLayout googleSignUpButton;
    private TextView goToLogin;

    private FirebaseAuth auth;
    private FirebaseFirestore db;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        username = findViewById(R.id.etName);
        signupEmail = findViewById(R.id.etSignupEmail);
        signupPassword = findViewById(R.id.etSignupPassword);
        signupConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        signupBtn = findViewById(R.id.signup_button);
        googleSignUpButton = findViewById(R.id.google_signup_button);
        goToLogin = findViewById(R.id.tvGoToLogin);
        signUpPasswordLayout = findViewById(R.id.signup_password_layout);
        signUpConfirmPasswordLayout = findViewById(R.id.signup_confirm_password_layout);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();


        // TextWatcher: Resets error state and icon on user input
        signupPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                signUpPasswordLayout.setError(null);
                signUpPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        signupConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                signUpConfirmPasswordLayout.setError(null);
                signUpConfirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Navigation Redirects
        goToLogin.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
        });

        signupBtn.setOnClickListener(v -> {
            signupUser();
        });

        googleSignUpButton.setOnClickListener(v -> {
            googleSignIn();
        });

    }

    // Initiates the Google Sign-In request via Credential Manager
    public void googleSignIn() {
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getBaseContext().getString(R.string.default_web_client_id))
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(SignupActivity.this);

        credentialManager.getCredentialAsync(
                SignupActivity.this,
                request,
                null,
                ContextCompat.getMainExecutor(this),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        handleSignIn(credential);
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        Log.e("GoogleSignIn", "Credential error: " + e.getMessage());
                        Toast.makeText(SignupActivity.this, "Google Sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Extracts token from Google Credential and passes to Firebase
    private void handleSignIn(Credential credential) {

        if (credential instanceof CustomCredential) {
            CustomCredential customCredential = (CustomCredential) credential;

            if (customCredential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                Bundle credentialData = customCredential.getData();
                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credentialData);

                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                return;
            }
        }
        Log.w(TAG, "Credential is not of type Google ID!");
    }

    // Authenticates with Firebase using the Google Token
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        boolean isNewUser = task.getResult().getAdditionalUserInfo().isNewUser();

                        if (isNewUser) {
                            saveGoogleUserToFirestore(user);
                        }

                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    } else {
                        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Sign in failed", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                });
    }

    // Stores Google profile data in Firestore for new users
    private void saveGoogleUserToFirestore(FirebaseUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", user.getDisplayName());
        data.put("email", user.getEmail());
        data.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        data.put("createdAt", FieldValue.serverTimestamp());

        db.collection("users")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Google user saved"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error saving user: " + e.getMessage()));
    }


    // Standard Email/Password Registration
    private void signupUser() {
        String name = username.getText().toString().trim();
        String email = signupEmail.getText().toString().trim();
        String password = signupPassword.getText().toString().trim();
        String confirmPassword = signupConfirmPassword.getText().toString().trim();

        // Validation Checks
        if (name.isEmpty()) {
            username.setError("Username is required");
            username.requestFocus();
            return;
        }
        if (email.isEmpty()) {
            signupEmail.setError("Email is required");
            signupEmail.requestFocus();
            return;
        }
        if (!isValidEmail(email)) {
            signupEmail.setError("Enter a valid email");
            signupEmail.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            signupPassword.setError("Password is required");
            signupPassword.requestFocus();
            signUpPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else if (!isValidPassword(password)) {
            signupPassword.setError("Password must be 8+ chars, include a number & a symbol");
            signUpPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else {
            signupPassword.setError(null);
            signUpPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        if (confirmPassword.isEmpty()) {
            signupConfirmPassword.setError("Confirm Password is required");
            signupConfirmPassword.requestFocus();
            signUpConfirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else{
            signupConfirmPassword.setError(null);
            signUpConfirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }

        if (!password.equals(confirmPassword)) {
            signupConfirmPassword.setError("Password does not match");
            signupConfirmPassword.requestFocus();
            signUpConfirmPasswordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        }

        signupBtn.setEnabled(false); // Prevent double clicks

        // Firebase Creation
        auth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if (task.isSuccessful()) {
                    Log.d("TAG", "signUp:success");
                    FirebaseUser user = auth.getCurrentUser();
                    if (user == null) return;

                    String uid = user.getUid();

                    // Prepare User Data
                    Map<String, Object> userData = new HashMap<>();
                    userData.put("name", name);
                    userData.put("email", email);
                    userData.put("createdAt", FieldValue.serverTimestamp());
                    userData.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);


                    // Save to Firestore
                    db.collection("users").document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Sign up successful", Snackbar.LENGTH_LONG);

                                startActivity(new Intent(SignupActivity.this, MainActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), "Sign up failed", Snackbar.LENGTH_LONG);
                                signupBtn.setEnabled(true);
                            });
                }
                else {
                    // Error Handling
                    Log.w("TAG", "signInWithEmail:failure", task.getException());
                    String message = "Login failed. Please try again.";

                    try {
                        throw task.getException();
                    } catch (Exception e) {
                        message = "Something went wrong. Try again.";
                    }
                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    signupBtn.setEnabled(true);
                }
            }
        });
    }

    // Helper: Validates Email Format
    boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Helper: Validates Password Complexity
    boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[@#$%^&+=!]).{6,}$";
        return password != null && password.matches(passwordRegex);
    }
}