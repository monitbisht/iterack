package io.github.monitbisht.iterack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    // Channel IDs
    public static final String CHANNEL_TASK_REMINDERS = "task_reminders";
    public static final String CHANNEL_DAILY_SUMMARY = "daily_summary";

    // Create channels

    // ----------------------------------------------------------
    // Show notification
    // ----------------------------------------------------------
    public static void showNotification(Context context, String channelId,
                                        String title, String msg, int id , boolean soundOn) {

         channelId = soundOn ?
                App.CHANNEL_REMINDERS_SOUND :
                App.CHANNEL_REMINDERS_SILENT;

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(msg)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }
}
