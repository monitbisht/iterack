package io.github.monitbisht.iterack;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.FrameLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;



public class MainActivity extends AppCompatActivity {

    BottomNavigationView navigationView;
    FrameLayout frame;
    FloatingActionButton fab;

    private boolean shouldShowLock = true;
    private static final int REQ_UNLOCK = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        navigationView = findViewById(R.id.bottom_navigation);
        frame = findViewById(R.id.frame_layout);
        fab = findViewById(R.id.fab_view);

        // ASK FOR PERMISSION (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Show the popup asking "Allow Iterack to send notifications?"
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        // TRACK USER ACTIVITY
        SharedPreferences userStats = getSharedPreferences("USER_STATS", MODE_PRIVATE);
        userStats.edit().putLong("LAST_SEEN", System.currentTimeMillis()).apply();

        // CHECK SETTINGS BEFORE SCHEDULING
        SharedPreferences settings = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
        boolean areNotificationsEnabled = settings.getBoolean("notifications_enabled", true);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

        // Only schedule if User is Logged In AND Notifications are Enabled
        if (uid != null && areNotificationsEnabled) {
            NotificationScheduler.scheduleAll(getApplicationContext());
        }

        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();

                if (itemId == R.id.home_tab) {
                    ft.replace(R.id.frame_layout, new HomeFragment());
                } else if (itemId == R.id.today_tab) {
                    ft.replace(R.id.frame_layout, new PlannerFragment());
                } else if (itemId == R.id.insights_tab) {
                    ft.replace(R.id.frame_layout, new InsightFragment());
                } else {
                    ft.replace(R.id.frame_layout, new ProfileFragment());
                }
                ft.commit();
                return true;
            }
        });

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, new HomeFragment())
                .commit();

        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
        });


    }

    @Override
    protected void onResume() {
        super.onResume();

        // check per-user app lock preferences; if enabled -> launch AppLockActivity
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String prefName = (uid == null) ? "APP_LOCK_PREFS_global" : "APP_LOCK_PREFS_" + uid;
        SharedPreferences prefs = getSharedPreferences(prefName, MODE_PRIVATE);
        boolean lockEnabled = prefs.getBoolean("enabled", false);

        if (lockEnabled && shouldShowLock) {
            Intent i = new Intent(this, AppLockActivity.class);
            startActivityForResult(i, REQ_UNLOCK);
            // don't change shouldShowLock here; onActivityResult we will handle.
        }
        shouldShowLock = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldShowLock = true;
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        shouldShowLock = true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // If AppLockActivity returns RESULT_OK, don't immediately relaunch it
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_UNLOCK && resultCode == RESULT_OK) {
            shouldShowLock = false;
        }
    }

}
