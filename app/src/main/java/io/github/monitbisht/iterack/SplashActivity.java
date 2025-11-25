package io.github.monitbisht.iterack;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;
    private MaterialButton getStartedButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        getStartedButton = findViewById(R.id.get_started_button);

        getStartedButton.setOnClickListener(v -> {

            //From the Firebase documentation: getCurrentUser() returns the currently signed-in FirebaseUser, or null if no user is signed in.
            FirebaseUser user = firebaseAuth.getCurrentUser();

            // Check if user is signed in (non-null) and update UI accordingly.
            if (user != null) {
                //If user is already logged in, go to MainActivity
                startActivity(new Intent(SplashActivity.this, MainActivity.class));

            } else {
                //If user is not logged in, go to LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
            }
            finish();  //Finish the SplashActivity
        });
    }

}
