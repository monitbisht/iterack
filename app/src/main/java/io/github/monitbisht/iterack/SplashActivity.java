package io.github.monitbisht.iterack;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

        firebaseAuth = FirebaseAuth.getInstance();

        // Check if this is the very first app launch
        SharedPreferences prefs = getSharedPreferences("ITERACK_PREFS", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("is_first_launch", true);


        if (isFirstLaunch){
            // Show special intro for first app launch
            playFirstLaunchAnimation();

            getStartedButton.setOnSlideCompleteListener(v -> {
                prefs.edit().putBoolean("is_first_launch", false).apply();

                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                finish();
            });

        }else{
            // Show standard loading animation
            playRegularAnimation();

            // Auto-redirect based on login status
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

    // Animation for first-time users (Slide Up + Fade In)
    private void playFirstLaunchAnimation(){
        getStartedButton.setAlpha(0f);
        getStartedButton.setTranslationY(30f);
        getStartedButton.setVisibility(View.VISIBLE);

        appLogo.animate()
                .translationY(-200f)
                .setDuration(1000)
                .setStartDelay(500);

        getStartedButton.animate()
                .alpha(1f)
                .setDuration(1000)
                .setStartDelay(1200)
                .start();
    }

    // Animation for returning users (Pulse effect)
    private void playRegularAnimation(){
        appLogo.animate()
                .scaleX(1.1f).scaleY(1.1f)
                .setDuration(700)
                .setStartDelay(200)
                .withEndAction(() -> {
                    appLogo.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(500)
                            .start();
                });
    }

    // Checks current Firebase Auth state
    private boolean isUserLoggedIn(){
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null;
    }
}