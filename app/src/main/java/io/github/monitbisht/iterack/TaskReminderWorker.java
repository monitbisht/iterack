package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TaskReminderWorker extends Worker {

    public TaskReminderWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("USER_SETTINGS", Context.MODE_PRIVATE);

        boolean areNotificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean playSound = prefs.getBoolean("sound_enabled", true);   // ← NEW

        if (!areNotificationsEnabled) return Result.success();


        String type = getInputData().getString("TYPE");

        ArrayList<Tasks> tasks;
        try {
            tasks = FireStoreHelper.getInstance().getAllTasksBlocking();
        } catch (Exception e) {
            return Result.success();
        }

        if (type == null) return Result.success();

        switch (type) {

            case "MORNING":
                if (isUserInactive()) {
                    showInactivityNotification(playSound);
                } else {
                    handleMorning(tasks, playSound);
                }
                break;

            case "MIDDAY":
                handleMidday(tasks, playSound);
                break;

            case "EVENING":
                handleEvening(tasks, playSound);
                break;

            case "SUMMARY":
                handleDailySummary(tasks, playSound);
                break;

            case "OVERDUE":
                handleOverdue(tasks, playSound);
                break;
        }

        return Result.success();
    }



    // NOTIFICATION HANDLERS (Every method now receives `playSound`)
    private void handleMorning(ArrayList<Tasks> tasks, boolean playSound) {

        Date today = strip(new Date());
        int starts = 0, deadlines = 0;

        for (Tasks t : tasks) {
            if (t.getStartDate() != null && strip(t.getStartDate()).equals(today))
                starts++;

            if (t.getEndDate() != null && strip(t.getEndDate()).equals(today))
                deadlines++;
        }

        String msg = getString(starts, deadlines);

        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_TASK_REMINDERS,
                "Today's Focus 🎯",
                msg,
                201,
                playSound
        );
    }

    @NonNull
    private static String getString(int starts, int deadlines) {
        String msg;

        if (starts == 0 && deadlines == 0) {
            msg = "Nothing scheduled for today. Enjoy your free time!";
        }
        else if (starts > 0 && deadlines > 0) {
            msg = starts + " task(s) start today and " + deadlines + " reach their deadline.";
        }
        else if (starts > 0) {
            msg = starts + " task(s) start today.";
        }
        else {
            // This handles the case where only deadlines > 0
            msg = deadlines + " task(s) reach their deadline today.";
        }

        return msg;
    }

    private void handleMidday(ArrayList<Tasks> tasks, boolean playSound) {

        Date today = strip(new Date());
        int pending = 0;

        for (Tasks t : tasks) {
            if (!t.isCompleted() &&
                    t.getStartDate() != null &&
                    t.getEndDate() != null &&
                    !today.before(strip(t.getStartDate())) &&
                    !today.after(strip(t.getEndDate()))) {
                pending++;
            }
        }

        if (pending == 0) return;

        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_TASK_REMINDERS,
                "Mid-Day Check 🌤️",
                pending + " tasks are still pending for today.",
                202,
                playSound
        );
    }

    private void handleEvening(ArrayList<Tasks> tasks, boolean playSound) {

        Date today = strip(new Date());
        int pending = 0;

        for (Tasks t : tasks) {
            if (!t.isCompleted() &&
                    t.getStartDate() != null &&
                    t.getEndDate() != null &&
                    !today.before(strip(t.getStartDate())) &&
                    !today.after(strip(t.getEndDate()))) {
                pending++;
            }
        }

        if (pending == 0) return;

        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_TASK_REMINDERS,
                "Almost There! 🌙",
                "Just " + pending + " tasks left. One last push before you relax.",
                203,
                playSound
        );
    }

    private void handleDailySummary(ArrayList<Tasks> tasks, boolean playSound) {

        Date today = strip(new Date());
        int active = 0, completed = 0, missed = 0, pending = 0;

        for (Tasks t : tasks) {

            if (t.getStartDate() == null || t.getEndDate() == null) continue;

            Date start = strip(t.getStartDate());
            Date end = strip(t.getEndDate());

            boolean isTodayTask = !today.before(start) && !today.after(end);

            if (isTodayTask) {
                active++;

                if (t.isCompleted()) completed++;
                else if (end.before(today)) missed++;
                else pending++;
            }
        }

        if (active == 0) return;

        String msg = "Out of " + active + " active tasks, you completed "
                + completed + ", missed " + missed + ", and " + pending + " are pending.";

        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_DAILY_SUMMARY,
                "Day in Review 📊",
                msg,
                204,
                playSound
        );
    }

    private void handleOverdue(ArrayList<Tasks> tasks, boolean playSound) {

        Date today = strip(new Date());
        int overdue = 0;

        for (Tasks t : tasks) {
            if (!t.isCompleted() &&
                    t.getEndDate() != null &&
                    strip(t.getEndDate()).before(today)) {
                overdue++;
            }
        }

        if (overdue == 0) return;

        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_TASK_REMINDERS,
                "Overdue Alert 🚨",
                overdue + " task(s) are overdue.",
                205,
                playSound
        );
    }

    private void showInactivityNotification(boolean playSound) {
        NotificationHelper.showNotification(
                getApplicationContext(),
                NotificationHelper.CHANNEL_DAILY_SUMMARY,
                "We Miss You! 👋",
                "Your planner is empty. Come back and set some goals!",
                999,
                playSound
        );
    }

    private boolean isUserInactive() {
        SharedPreferences prefs = getApplicationContext()
                .getSharedPreferences("USER_STATS", Context.MODE_PRIVATE);

        long lastSeen = prefs.getLong("LAST_SEEN", System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();

        long threeDays = 3L * 24 * 60 * 60 * 1000;

        return (currentTime - lastSeen) > threeDays;
    }

    // Utility
    private Date strip(Date d) {
        Calendar c = Calendar.getInstance();
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
}
