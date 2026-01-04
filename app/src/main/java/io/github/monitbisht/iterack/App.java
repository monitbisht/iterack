package io.github.monitbisht.iterack;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

public class App extends Application {

    public static final String CHANNEL_REMINDERS_SOUND = "task_reminders_sound";
    public static final String CHANNEL_REMINDERS_SILENT = "task_reminders_silent";

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            // Channel WITH sound
            NotificationChannel soundChannel = new NotificationChannel(
                    CHANNEL_REMINDERS_SOUND,
                    "Task Reminders (Sound)",
                    NotificationManager.IMPORTANCE_HIGH
            );

            soundChannel.setDescription("Reminders with notification sound");
            soundChannel.enableVibration(true);
            soundChannel.setVibrationPattern(new long[]{0, 500, 200, 0}); // Wait 0ms, Vibrate 500ms, Pause 200ms, Vibrate 500ms

            // Silent channel (No sound, no vibration)
            NotificationChannel silentChannel = new NotificationChannel(
                    CHANNEL_REMINDERS_SILENT,
                    "Task Reminders (Silent)",
                    NotificationManager.IMPORTANCE_LOW
            );
            silentChannel.setDescription("Silent task reminders");
            silentChannel.setSound(null, null);
            silentChannel.enableVibration(true  );

            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(soundChannel);
            nm.createNotificationChannel(silentChannel);
        }
    }
}
