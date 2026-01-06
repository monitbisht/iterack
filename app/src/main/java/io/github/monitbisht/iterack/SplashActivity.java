package io.github.monitbisht.iterack;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.ncorti.slidetoact.SlideToActView;

public class SplashActivity extends AppCompatActivity {

    private FirebaseAuth firebaseAuth;

    private ImageView appLogo;
    private SlideToActView getStartedButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        appLogo = findViewById(R.id.appLogo);
        getStartedButton = findViewById(R.id.get_started_button);

        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();

        //Shared Preference Set Up
        SharedPreferences prefs = getSharedPreferences("ITERACK_PREFS", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);



        if (isFirstLaunch){

            // Show Intro Animation
            playFirstLaunchAnimation();

            // Set up the button click listener
            getStartedButton.setOnSlideCompleteListener(v -> {

                // Mark that app has now been launched once
                prefs.edit().putBoolean("is_first_launch", false).apply();

                // Start the LoginActivity
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                finish();  //Finish the SplashActivity
            });

        }else{
            // Show Regular Animation
            playRegularAnimation();

            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                if (isUserLoggedIn()) {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                } else {
                    startActivity(new Intent(this, LoginActivity.class));
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                }
                finish();
            }, 1200);
        }

    }

    private void playFirstLaunchAnimation(){
        // Set the initial state (Hide the button completely first)
        getStartedButton.setAlpha(0f); // Fully transparent
        getStartedButton.setTranslationY(30f);
        getStartedButton.setVisibility(View.VISIBLE);

        // Animate the Logo (Slide UP)
        appLogo.animate()
                .translationY(-200f)  // Move up by 200 pixels
                .setDuration(1000)    // Animation takes 1 second
                .setStartDelay(500);  // Wait 0.5s before starting


        // Animate the Button (Fade IN)
        getStartedButton.animate()
                .alpha(1f)// Fade to fully opaque
                .setDuration(1000)    // Animation takes 1 second
                .setStartDelay(1200)  // Wait 1.2s (starts slightly after logo moves)
                .start();
    }

    private void playRegularAnimation(){

        appLogo.animate()
                .scaleX(1.1f).scaleY(1.1f) // Grow slightly (10%)
                .setDuration(700)
                .setStartDelay(200)
                .withEndAction(() -> {
                    // Shrink back to normal (Bouncing effect)
                    appLogo.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(500)
                            .start();
                });
    }

    private boolean isUserLoggedIn(){
        //From the Firebase documentation: getCurrentUser() returns the currently signed-in FirebaseUser, or null if no user is signed in.
        FirebaseUser user = firebaseAuth.getCurrentUser();

        // Check if user is signed in (non-null) and update UI accordingly.
        if (user != null)
            return true;
        else
            return false;

    }
}
