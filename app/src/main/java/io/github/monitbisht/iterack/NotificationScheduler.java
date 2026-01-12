package io.github.monitbisht.iterack;

import android.content.Context;

import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class NotificationScheduler {

    // Queue up all daily reminders
    public static void scheduleAll(Context context) {
        scheduleReminder(context, "MORNING", 8, 0);
        scheduleReminder(context, "MIDDAY", 14, 0);
        scheduleReminder(context, "EVENING", 20, 0);
        scheduleReminder(context, "OVERDUE", 21, 0);
        scheduleReminder(context, "SUMMARY", 22, 0);
    }

    // Cancel all pending notification work
    public static void cancelAll(Context context) {
        WorkManager wm = WorkManager.getInstance(context);

        wm.cancelUniqueWork("REMINDER_MORNING");
        wm.cancelUniqueWork("REMINDER_MIDDAY");
        wm.cancelUniqueWork("REMINDER_EVENING");
        wm.cancelUniqueWork("REMINDER_SUMMARY");
        wm.cancelUniqueWork("REMINDER_OVERDUE");
    }

    // Create and enqueue a periodic work request
    private static void scheduleReminder(Context context, String tag, int hour, int minute) {

        long delay = calculateDelay(hour, minute);

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                TaskReminderWorker.class,
                24, TimeUnit.HOURS
        )
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag(tag)
                .build();

        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        "REMINDER_" + tag,            // Unique name
                        ExistingPeriodicWorkPolicy.UPDATE,   // Update existing work
                        request
                );
    }

    // Calculate time remaining until the next specific hour/minute
    private static long calculateDelay(int hour, int minute) {
        Calendar now = Calendar.getInstance();
        Calendar next = Calendar.getInstance();
        next.set(Calendar.HOUR_OF_DAY, hour);
        next.set(Calendar.MINUTE, minute);
        next.set(Calendar.SECOND, 0);
        next.set(Calendar.MILLISECOND, 0);

        // If time has passed today, schedule for tomorrow
        if (next.before(now)) {
            next.add(Calendar.DAY_OF_YEAR, 1);
        }

        return next.getTimeInMillis() - now.getTimeInMillis();
    }
}