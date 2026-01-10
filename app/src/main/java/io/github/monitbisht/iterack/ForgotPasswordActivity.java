package io.github.monitbisht.iterack;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText emailInput;
    private TextInputLayout emailLayout;
    private AppCompatButton resetButton;
    private TextView backToLogin;

    private FirebaseAuth auth;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);


        emailInput = findViewById(R.id.etForgotEmail);
        emailLayout = findViewById(R.id.forgot_email_layout);
        resetButton = findViewById(R.id.btnResetPassword);
        backToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.resetProgress);


        auth = FirebaseAuth.getInstance();

        backToLogin.setOnClickListener(v -> finish());

        resetButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (!isValidEmail(email)) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return;
        }

        emailInput.setError(null);
        resetButton.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);

        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    resetButton.setEnabled(true);
                    progressBar.setVisibility(View.GONE);

                    if (task.isSuccessful()) {
                        Toast.makeText(
                                this,
                                "If an account exists, a reset link has been sent.",
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    } else {
                        String errorMsg = "Connection failed. Please check your internet.";

                        if (task.getException() != null) {
                            // Log the real error for debugging
                            Log.e("ResetPassword", "Error: " + task.getException().getMessage());
                        }

                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
    
}

