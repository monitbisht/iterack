package io.github.monitbisht.iterack;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {

    // Notification Channel IDs
    public static final String CHANNEL_REMINDERS_SOUND = "task_reminders_sound";
    public static final String CHANNEL_REMINDERS_SILENT = "task_reminders_silent";

    @Override
    public void onCreate() {
        super.onCreate();

        // Notification Channels (required for Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Channel 1: High Importance (Makes Sound & Vibrate)
            NotificationChannel soundChannel = new NotificationChannel(
                    CHANNEL_REMINDERS_SOUND,
                    "Task Reminders (Sound)",
                    NotificationManager.IMPORTANCE_HIGH
            );

            soundChannel.setDescription("Reminders with notification sound");
            soundChannel.enableVibration(true);
            soundChannel.setVibrationPattern(new long[]{0, 500, 200, 0}); // Wait 0ms, Vibrate 500ms, Pause 200ms, Vibrate 500ms

            // Channel 2: Low Importance (Silent & No Pop-up)
            NotificationChannel silentChannel = new NotificationChannel(
                    CHANNEL_REMINDERS_SILENT,
                    "Task Reminders (Silent)",
                    NotificationManager.IMPORTANCE_LOW
            );
            silentChannel.setDescription("Silent task reminders");
            silentChannel.setSound(null, null);
            silentChannel.enableVibration(true);

            // Register channels with the System
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(soundChannel);
            nm.createNotificationChannel(silentChannel);
        }
    }
}