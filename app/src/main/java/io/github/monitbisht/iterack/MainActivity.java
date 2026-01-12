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
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {

    BottomNavigationView navigationView;
    FrameLayout frame;
    FloatingActionButton fab;

    // App Lock State Tracking
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

        // Permission Logic: Explicitly ask for Notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                // Show system dialog
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        // Track "Last Seen" for inactivity calculations
        SharedPreferences userStats = getSharedPreferences("USER_STATS", MODE_PRIVATE);
        userStats.edit().putLong("LAST_SEEN", System.currentTimeMillis()).apply();

        // Check settings before scheduling background reminders
        SharedPreferences settings = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
        boolean areNotificationsEnabled = settings.getBoolean("notifications_enabled", true);

        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();

        // Only schedule WorkManager tasks if User is Logged In AND Notifications are Enabled
        if (uid != null && areNotificationsEnabled) {
            NotificationScheduler.scheduleAll(getApplicationContext());
        }

        // Switch fragments based on bottom tab selection
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

        // Default Fragment
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.frame_layout, new HomeFragment())
                .commit();

        // Floating Action Button (Opens the "Add Task" screen)
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddTaskActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
        });
    }

    // Called every time the app comes to the foreground
    @Override
    protected void onResume() {
        super.onResume();

        // If App Lock is enabled, show the Lock Screen
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        String prefName = (uid == null) ? "APP_LOCK_PREFS_global" : "APP_LOCK_PREFS_" + uid;
        SharedPreferences lockPrefs = getSharedPreferences(prefName, MODE_PRIVATE);
        boolean lockEnabled = lockPrefs.getBoolean("enabled", false);

        if (lockEnabled && shouldShowLock) {
            Intent i = new Intent(this, AppLockActivity.class);
            startActivityForResult(i, REQ_UNLOCK);
        }
        shouldShowLock = true;

        // 2. Notification "Wake Up" Nudge (NEW ADDITION)
        SharedPreferences prefs = getSharedPreferences("USER_SETTINGS", MODE_PRIVATE);
        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);

        if (notificationsEnabled) {
            checkAndTriggerNudge();
        }
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

    // Handle return from Lock Screen
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // If unlock was successful, prevent locking again immediately
        if (requestCode == REQ_UNLOCK && resultCode == RESULT_OK) {
            shouldShowLock = false;
        }
    }

    // Helper: Checks cooldown and fires nudge if allowed
    private void checkAndTriggerNudge() {
        SharedPreferences prefs = getSharedPreferences("ITERACK_PREFS", MODE_PRIVATE);
        long lastNudge = prefs.getLong("last_nudge_time", 0);
        long now = System.currentTimeMillis();

        // Cooldown: 6 Hours
        if (now - lastNudge > TimeUnit.HOURS.toMillis(6)) {
            triggerNotificationNudge();
            prefs.edit().putLong("last_nudge_time", now).apply();
        }
    }

    // Helper: Enqueues the actual worker
    private void triggerNotificationNudge() {
        OneTimeWorkRequest nudge =
                new OneTimeWorkRequest.Builder(TaskReminderWorker.class)
                        .setInputData(
                                new Data.Builder()
                                        .putString("TYPE", "NUDGE")
                                        .build()
                        )
                        .build();

        WorkManager.getInstance(this).enqueue(nudge);
    }
}