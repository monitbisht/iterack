package io.github.monitbisht.iterack;

import static android.content.ContentValues.TAG;
import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;

import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;

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
import androidx.credentials.CredentialManagerCallback;
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


public class LoginActivity extends AppCompatActivity {

    private TextInputEditText emailEditText, passwordEditText;
    private TextInputLayout passwordLayout;
    private TextView forgotPasswordTextView, signUpTextView;
    private AppCompatButton loginButton;
    private LinearLayout googleSignInButton;
    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailEditText = findViewById(R.id.etEmail);
        passwordEditText = findViewById(R.id.etPassword);
        forgotPasswordTextView = findViewById(R.id.forgot_password);
        signUpTextView = findViewById(R.id.signup_textview);
        loginButton = findViewById(R.id.login_button);
        passwordLayout = findViewById(R.id.password_layout);

        googleSignInButton = findViewById(R.id.btn_google_sign_in);
        firebaseAuth = FirebaseAuth.getInstance();


        // Automatically show the eye icon of Password input field again when user types
        passwordEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                // Remove error immediately when user starts typing
                passwordLayout.setError(null);

                // Restore the eye icon
                passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Navigation to SignUp Page
        signUpTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, SignupActivity.class));
        });

        // Navigation to Forgot Password Page
        forgotPasswordTextView.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this , ForgotPasswordActivity.class));
        });

        // Login Process
        loginButton.setOnClickListener(v -> {
            loginUser();
        });

        // Google Sign-In Flow
        googleSignInButton.setOnClickListener(v -> {
            googleSignIn();
        });
    }

    public void googleSignIn() {
        // Instantiate a Google sign-in request
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getBaseContext().getString(R.string.default_web_client_id))
                .build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Ask Credential Manager to retrieve Google credentials
        CredentialManager credentialManager = CredentialManager.create(LoginActivity.this);

        credentialManager.getCredentialAsync(
                LoginActivity.this,
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
                        Toast.makeText(LoginActivity.this, "Google Sign-in failed", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    // Process the credential returned by Google
    private void handleSignIn(Credential credential) {

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential = (CustomCredential) credential;

            if (customCredential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {

                Bundle credentialData = customCredential.getData();

                GoogleIdTokenCredential googleIdTokenCredential =
                        GoogleIdTokenCredential.createFrom(credentialData);

                // Proceed to Firebase Authentication
                firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
                return;
            }
        }

        Log.w("LoginActivity", "Credential is not of type Google ID!");
    }


    // Exchange Google Token for Firebase Auth Credential
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);

        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {

                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        boolean isNewUser =
                                task.getResult().getAdditionalUserInfo().isNewUser();

                        // Create Firestore entry only if user is new
                        if (isNewUser) {
                            saveGoogleUserToFirestore(user);
                        }

                        startActivity(new Intent(this, MainActivity.class));
                        finish();

                    } else {
                        Toast.makeText(this, "Google Sign-in failed: "
                                        + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Store Google Account details in Firestore
    private void saveGoogleUserToFirestore(FirebaseUser user) {
        Map<String, Object> data = new HashMap<>();
        data.put("name", user.getDisplayName());
        data.put("email", user.getEmail());
        data.put("photoUrl", user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null);
        data.put("createdAt", FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.getUid())
                .set(data)
                .addOnSuccessListener(aVoid ->
                        Log.d("Firestore", "Google user saved"))
                .addOnFailureListener(e ->
                        Log.e("Firestore", "Error saving Google user: " + e.getMessage()));
    }

    // Standard Email/Password Login
    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Check if email is empty
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        // Check format
        if (!isValidEmail(email)) {
            emailEditText.setError("Enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        // Check password validity
        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else if (!isValidPassword(password)) {
            passwordEditText.setError("Password must be 6+ chars, include a number & a symbol");
            passwordEditText.requestFocus();
            passwordLayout.setEndIconMode(TextInputLayout.END_ICON_NONE);
            return;
        } else {
            passwordEditText.setError(null);
            passwordLayout.setEndIconMode(TextInputLayout.END_ICON_PASSWORD_TOGGLE);
        }


        loginButton.setEnabled(false); //Prevent double taps

        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {
                    // Sign in success , switch to main activity with user's info

                    Log.d("TAG", "signInWithEmail:success");
                    FirebaseUser user = firebaseAuth.getCurrentUser();

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    loginButton.setEnabled(true);
                    finish();
                } else {
                    // If sign in fails , display a message to the user

                    Log.w("TAG", "signInWithEmail:failure", task.getException());
                    String message = "Login failed. Please try again.";

                    try {
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e) {
                        message = "No account found with this email.";
                    } catch (FirebaseAuthInvalidCredentialsException e) {
                        message = "Incorrect email or password.";
                    } catch (Exception e) {
                        message = "Something went wrong. Try again.";
                    }

                    Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
                    snackbar.show();
                    loginButton.setEnabled(true);
                }
            }
        });
    }

    // Helper: Regex for email
    boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    // Helper: Regex for password complexity
    boolean isValidPassword(String password) {
        String passwordRegex = "^(?=.*[0-9])(?=.*[@#$%^&+=!]).{6,}$";
        return password != null && password.matches(passwordRegex);
    }
}