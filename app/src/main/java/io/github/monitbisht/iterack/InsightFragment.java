package io.github.monitbisht.iterack;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.auth.FirebaseAuth;


import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InsightFragment extends Fragment {

    private BarChart barChart;
    private AutoCompleteTextView trendDropdown;

    private TextView currentStreakTv, longestStreakTv, totalCompletedTv, totalMissedTv;

    private TextView scoreText, tipText, conclusionText, summaryText, weeklyTip;
    private static final String MODE_WEEKLY = "Weekly";
    private static final String MODE_MONTHLY = "Monthly";

    private int lastSavedWeek = -1;
    private int currentWeekCompleted = 0;
    private int previousWeekCompleted = 0;
    private int currentWeekMissed = 0;
    private int previousWeekMissed = 0;


    public InsightFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_insight, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.barChart);
        trendDropdown = view.findViewById(R.id.trendDropdown);
        currentStreakTv = view.findViewById(R.id.current_streak);
        longestStreakTv = view.findViewById(R.id.longest_steak);
        totalCompletedTv = view.findViewById(R.id.total_completed_tasks);
        totalMissedTv = view.findViewById(R.id.total_missed_tasks);
        scoreText = view.findViewById(R.id.productivity_score);
        tipText = view.findViewById(R.id.productivity_tip);
        conclusionText = view.findViewById(R.id.conclusion_text);
        summaryText = view.findViewById(R.id.summary_textview);
        weeklyTip = view.findViewById(R.id.weekly_tip);


        setupDropdown();
        showEmptyWeeklyChart(); // default mode only

        loadInsightDataAndRender(MODE_WEEKLY);    // default view

    }


    // 1. Load Task Data with Rendering
    private void loadInsightDataAndRender(String mode) {

        FireStoreHelper.getInstance().getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {
            @Override
            public void onSuccess(ArrayList<Tasks> tasksList) {

                if (!isAdded() || getContext() == null) return;

                if (tasksList == null || tasksList.isEmpty()) {
                    if (MODE_WEEKLY.equals(mode)) showEmptyWeeklyChart();
                    else showEmptyMonthlyChart();
                    return;
                }


                updateCounters(tasksList);

                currentWeekCompleted = countWeeklyCompleted(tasksList, false);
                previousWeekCompleted = countWeeklyCompleted(tasksList, true);

                currentWeekMissed = countWeeklyMissed(tasksList, false);
                previousWeekMissed = countWeeklyMissed(tasksList, true);

                saveWeeklyStatsToFirestore();

                if (MODE_WEEKLY.equals(mode)) {
                    handleWeeklyRollover(tasksList);
                    renderWeeklyChart(getWeeklyTaskCounts(tasksList));
                } else {
                    renderMonthlyChart(getMonthlyCounts(tasksList));
                }

                // Fetching Weekly Stats

                String uid = FirebaseAuth.getInstance().getUid();

                FireStoreHelper.getInstance().getWeeklyStats(uid,
                        new FireStoreHelper.FirestoreCallback<Map<String, Object>>() {
                            @Override
                            public void onSuccess(Map<String, Object> statsMap) {

                                long creationTime = FirebaseAuth.getInstance()
                                        .getCurrentUser()
                                        .getMetadata()
                                        .getCreationTimestamp();

                                Date joinDate = new Date(creationTime);
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                String joinDateString = sdf.format(joinDate);

                                // Now statsMap exists → no error
                                String weeklyJson = buildWeeklyJson(tasksList, joinDateString, statsMap);

                                String previousJson = getLastWeeklyJson();
                                String currentJson = weeklyJson;

                                setLoadingState();

                                if (previousJson != null && previousJson.equals(currentJson)) {
                                    String cached = getCachedInsights();
                                    if (cached != null) {
                                        applyAiOutput(cached);
                                        stopLoadingAnimation();
                                        return;
                                    }
                                }

                                AiHelper ai = new AiHelper();
                                ai.generateWeeklySummary(currentJson, new AiCallback() {
                                    @Override
                                    public void onResult(String output) {
                                        if (getActivity() == null) return;

                                        saveInsightsToCache(output);
                                        saveWeeklyJson(currentJson);

                                        getActivity().runOnUiThread(() -> applyAiOutput(output));
                                        stopLoadingAnimation();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Log.e("AI_ERROR", error);
                                    }
                                });
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e("STATS", "Could not fetch weeklyStats");
                            }
                        });
            }

            @Override
            public void onError(Exception e) {
                showEmptyWeeklyChart();
                showEmptyMonthlyChart();
            }
        });
    }

    private void saveWeeklyStatsToFirestore() {
        String uid = FirebaseAuth.getInstance().getUid();

        Map<String, Object> map = new HashMap<>();
        map.put("currentWeekCompleted", currentWeekCompleted);
        map.put("previousWeekCompleted", previousWeekCompleted);
        map.put("currentWeekMissed", currentWeekMissed);
        map.put("previousWeekMissed", previousWeekMissed);
        map.put("lastSavedWeek", lastSavedWeek);

        FireStoreHelper.getInstance().saveWeeklyStats(uid, map, new FireStoreHelper.FirestoreCallback<Boolean>() {
            @Override public void onSuccess(Boolean r) {}
            @Override public void onError(Exception e) {}
        });
    }


    // 2. Data Processing
    // Weekly: 7 entries, Mon..Sun
    private int[] getWeeklyTaskCounts(List<Tasks> tasksList) {

        int[] counts = new int[7];

        Calendar cal = Calendar.getInstance();
        int currentWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int currentYear = cal.get(Calendar.YEAR);

        Calendar taskCal = Calendar.getInstance();

        for (Tasks task : tasksList) {
            if (task == null) continue;
            if (!task.isCompleted()) continue;

            Date comp = task.getCompletionDate();
            if (comp == null) continue;

            taskCal.setTime(comp);

            int taskWeek = taskCal.get(Calendar.WEEK_OF_YEAR);
            int taskYear = taskCal.get(Calendar.YEAR);

            // ONLY COUNT TASKS FROM CURRENT WEEK
            if (taskYear != currentYear || taskWeek != currentWeek)
                continue;

            int day = taskCal.get(Calendar.DAY_OF_WEEK); // Sun=1, Mon=2..
            int index = (day == Calendar.SUNDAY) ? 6 : day - 2;

            if (index >= 0 && index < 7) {
                counts[index]++;
            }
        }

        return counts;
    }


    // Monthly: group into 4 buckets (W1,W2,W3,W4) of current month
    // This keeps the chart simple & fits the 4 columns and the y-axis (0..50)
    private int[] getMonthlyCounts(List<Tasks> tasksList) {
        int[] counts = new int[4]; // Week1..Week4
        Calendar cal = Calendar.getInstance();

        int curYear = cal.get(Calendar.YEAR);
        int curMonth = cal.get(Calendar.MONTH);

        for (Tasks t : tasksList) {
            if (t == null) continue;
            if (!t.isCompleted()) continue;

            Date comp = t.getCompletionDate();
            if (comp == null) continue;

            cal.setTime(comp);
            int y = cal.get(Calendar.YEAR);
            int m = cal.get(Calendar.MONTH);
            if (y != curYear || m != curMonth) continue; // only current month

            int dayOfMonth = cal.get(Calendar.DAY_OF_MONTH); // 1..31
            int weekIndex = (dayOfMonth - 1) / 7; // 0->days1-7, 1->8-14, 2->15-21, 3->22+
            if (weekIndex < 0) weekIndex = 0;
            if (weekIndex > 3) weekIndex = 3;
            counts[weekIndex]++;
        }

        return counts;
    }


    // 3. Render Helper Function
    private void renderWeeklyChart(int[] weeklyCounts) {
        if (weeklyCounts == null || weeklyCounts.length != 7) {
            showEmptyWeeklyChart();
            return;
        }

        boolean noData = true;
        for (int v : weeklyCounts)
            if (v > 0) {
                noData = false;
                break;
            }

        if (noData) {
            // Clean empty state (no ugly zeros)
            showEmptyWeeklyChart();
            return;
        }

        // Build entries
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            entries.add(new BarEntry(i, weeklyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Completed");
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        // Integer value formatter (no decimals)
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        // draw values above bars
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        barChart.setData(barData);

        // X axis labels Mon..Sun
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        configureChartAppearanceForWeekly(days);

        barChart.invalidate();
        barChart.animateY(800);
    }

    private void renderMonthlyChart(int[] monthlyCounts) {
        if (monthlyCounts == null || monthlyCounts.length != 4) {
            showEmptyMonthlyChart();
            return;
        }

        boolean noData = true;
        for (int v : monthlyCounts)
            if (v > 0) {
                noData = false;
                break;
            }

        if (noData) {
            showEmptyMonthlyChart();
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            entries.add(new BarEntry(i, monthlyCounts[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Completed");
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setValueTextColor(Color.WHITE);
        dataSet.setValueTextSize(10f);

        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);

        barChart.setData(barData);

        // X axis labels W1..W4
        String[] weeks = {"Week 1", "Week 2", "Week 3", "Week 4"};
        configureChartAppearanceForMonthly(weeks);

        barChart.invalidate();
        barChart.animateY(800);
    }


    // 4. Chart Appearance Config

    private void configureChartAppearanceForWeekly(String[] labels) {
        int gray = ContextCompat.getColor(requireContext(), R.color.light_gray);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(gray);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setTextColor(gray);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(25f);
        yAxis.setLabelCount(6, true); // 0,5,10,15,20,25
        yAxis.setGranularity(5f);
        yAxis.setDrawLabels(true);

        barChart.getAxisRight().setEnabled(false);

        // Legend gap and offsets
        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(gray);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setYOffset(12f);

        // Provide space between chart and legend
        barChart.setExtraBottomOffset(16f);
        barChart.getDescription().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
    }

    private void configureChartAppearanceForMonthly(String[] labels) {
        int gray = ContextCompat.getColor(requireContext(), R.color.light_gray);

        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(gray);
        xAxis.setTextSize(12f);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);

        YAxis yAxis = barChart.getAxisLeft();
        yAxis.setDrawGridLines(false);
        yAxis.setTextColor(gray);
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(50f);
        yAxis.setLabelCount(6, true); // 0,10,20,30,40,50
        yAxis.setGranularity(10f);
        yAxis.setDrawLabels(true);

        barChart.getAxisRight().setEnabled(false);

        Legend legend = barChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(gray);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setYOffset(12f);

        barChart.setExtraBottomOffset(28f);

        barChart.getDescription().setEnabled(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
    }


    // 5. Empty State (still remaining)
    private void showEmptyWeeklyChart() {
        if (barChart == null || getContext() == null) return;

        int[] empty = {0,0,0,0,0,0,0};
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 7; i++) entries.add(new BarEntry(i, empty[i]));

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Completed");
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        String[] labels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        configureChartAppearanceForWeekly(labels);

        barChart.invalidate();
    }

    private void showEmptyMonthlyChart() {
        if (barChart == null || getContext() == null) return;

        int[] empty = {0,0,0,0};
        List<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < 4; i++) entries.add(new BarEntry(i, empty[i]));

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Completed");
        dataSet.setColor(Color.parseColor("#4A90E2"));
        dataSet.setDrawValues(false);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        String[] labels = {"Week 1","Week 2","Week 3","Week 4"};
        configureChartAppearanceForMonthly(labels);

        barChart.invalidate();
    }


    // 6. Dropdown Setup

    private void setupDropdown() {
        String[] options = {MODE_WEEKLY, MODE_MONTHLY};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.dropdown_item,
                options
        );
        trendDropdown.setAdapter(adapter);
        trendDropdown.setText(MODE_WEEKLY, false);

        trendDropdown.setOnItemClickListener((parent, view, position, id) -> {
            String selected = (String) parent.getItemAtPosition(position);
            // reload / re-render according to mode
            loadInsightDataAndRender(selected);
            // re-enable legend when data appears (render methods enable legend)
            barChart.getAxisLeft().setDrawLabels(true);
            barChart.getLegend().setEnabled(true);
        });
    }



    public int getMonthlyCompleted(List<Tasks> tasksList) {
        int count = 0;

        for (Tasks t : tasksList) {
            if (t == null) continue;
            if (!t.isCompleted()) continue;

            Date comp = t.getCompletionDate();
            if (isInCurrentMonth(comp)) {
                count++;
            }
        }

        return count;
    }

    private void updateCounters(List<Tasks> tasksList) {
        // monthly completed & missed
        int monthlyCompleted = getMonthlyCompleted(tasksList);
        int monthlyMissed = getMonthlyMissed(tasksList);

        // streaks (calculated from all completed dates)
        int currentStreak = countCurrentStreak(tasksList);
        int longest = countLongestStreak(tasksList);

        if (!isAdded() || getContext() == null) return;

        if (currentStreak == 1){
            currentStreakTv.setText(String.valueOf(currentStreak) + " day");
        } else {
            currentStreakTv.setText(String.valueOf(currentStreak) + " days");
        }

        if (longest == 1){
            longestStreakTv.setText(String.valueOf(longest) + " day");
        } else {
            longestStreakTv.setText(String.valueOf(longest) + " days");
        }

        totalCompletedTv.setText(String.valueOf(monthlyCompleted));
        totalMissedTv.setText(String.valueOf(monthlyMissed));
    }
    public int getMonthlyMissed(List<Tasks> tasksList) {
        if (tasksList == null) return 0;
        int count = 0;
        Date today = stripTime(new Date());

        for (Tasks t : tasksList) {
            if (t == null || t.isCompleted()) continue;

            Date end = t.getEndDate();
            if (end == null) continue;

            if (!isInCurrentMonth(end)) continue;

            Date endStripped = stripTime(end);
            if (endStripped.before(today)) count++;
        }
        return count;
    }
    private Map<String, Integer> buildCompletionMap(List<Tasks> tasksList) {
        Map<String, Integer> map = new HashMap<>();
        if (tasksList == null) return map;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (Tasks t : tasksList) {
            if (t == null || !t.isCompleted()) continue;

            Date comp = t.getCompletionDate();
            if (comp == null) continue;

            String day = sdf.format(stripTime(comp));

            map.put(day, map.getOrDefault(day, 0) + 1);
        }
        return map;
    }
    public int countCurrentStreak(List<Tasks> tasksList) {
        Map<String, Integer> map = buildCompletionMap(tasksList);
        if (map.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        int streak = 0;

        while (true) {
            String key = sdf.format(stripTime(cal.getTime()));
            if (map.containsKey(key)) {
                streak++;
                cal.add(Calendar.DATE, -1);
            } else {
                break;
            }
        }
        return streak;
    }
    public int countLongestStreak(List<Tasks> tasksList) {
        Map<String, Integer> map = buildCompletionMap(tasksList);
        if (map.isEmpty()) return 0;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        List<Date> dates = new ArrayList<>();
        for (String key : map.keySet()) {
            try {
                Date d = sdf.parse(key);
                if (d != null) dates.add(d);
            } catch (Exception ignored) {}
        }

        if (dates.isEmpty()) return 0;

        Collections.sort(dates);

        int longest = 1;
        int current = 1;

        for (int i = 1; i < dates.size(); i++) {
            long diff = stripTime(dates.get(i)).getTime() - stripTime(dates.get(i - 1)).getTime();
            long days = diff / (24L * 60L * 60L * 1000L);

            if (days == 1) {
                current++;
                longest = Math.max(longest, current);
            } else {
                current = 1;
            }
        }

        return longest;
    }


    private boolean isInCurrentMonth(Date date) {
            if (date == null) return false;

            Calendar now = Calendar.getInstance();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);

            return now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    now.get(Calendar.MONTH) == cal.get(Calendar.MONTH);
        }

    private Date stripTime(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }
    private String buildWeeklyJson(List<Tasks> tasksList, String joinDateString , Map<String, Object> stats) {
        try {
            int[] weekly = getWeeklyTaskCounts(tasksList);

            int totalCompleted = getMonthlyCompleted(tasksList);
            int totalMissed = getMonthlyMissed(tasksList);

            int current = countCurrentStreak(tasksList);
            int longest = countLongestStreak(tasksList);

            JSONObject root = new JSONObject();
            JSONObject week = new JSONObject();

            week.put("mon", weekly[0]);
            week.put("tue", weekly[1]);
            week.put("wed", weekly[2]);
            week.put("thu", weekly[3]);
            week.put("fri", weekly[4]);
            week.put("sat", weekly[5]);
            week.put("sun", weekly[6]);

            week.put("total_completed", totalCompleted);
            week.put("total_missed", totalMissed);

            JSONObject streak = new JSONObject();
            streak.put("current_streak", current);
            streak.put("longest_streak", longest);

            root.put("week", week);
            root.put("streak", streak);
            root.put("joining_date", joinDateString);

            if (stats != null) {
                week.put("current_week_completed", stats.getOrDefault("currentWeekCompleted", 0));
                week.put("previous_week_completed", stats.getOrDefault("previousWeekCompleted", 0));
                week.put("current_week_missed", stats.getOrDefault("currentWeekMissed", 0));
                week.put("previous_week_missed", stats.getOrDefault("previousWeekMissed", 0));
            }


            return root.toString(2);

        } catch (Exception e) {
            return "{}";
        }
    }


    private void applyAiOutput(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            // Read the fields
            int score = obj.optInt("productivity_score", 0);
            String tip = obj.optString("productivity_tip", "");
            String summary = obj.optString("weekly_summary", "");
            String conclusion = obj.optString("conclusion", "");
            String wTip = obj.optString("weekly_tip", "");

            // Set texts
            scoreText.setText(String.valueOf(score));
            tipText.setText(tip);
            summaryText.setText(summary);
            conclusionText.setText(conclusion);
            weeklyTip.setText(wTip);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveInsightsToCache(String json) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("ai_insights_cache", Context.MODE_PRIVATE);
        prefs.edit().putString("insights_json", json).apply();
    }

    private String getCachedInsights() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("ai_insights_cache", Context.MODE_PRIVATE);
        return prefs.getString("insights_json", null);
    }

    private void clearCache() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("ai_insights_cache", Context.MODE_PRIVATE);
        prefs.edit().remove("insights_json").apply();
    }

    private void saveWeeklyJson(String json) {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("ai_insights_cache", Context.MODE_PRIVATE);
        prefs.edit().putString("last_weekly_json", json).apply();
    }

    private String getLastWeeklyJson() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("ai_insights_cache", Context.MODE_PRIVATE);
        return prefs.getString("last_weekly_json", null);
    }
    private void setLoadingState() {
        scoreText.setText("...");
        tipText.setText("Loading...");
        summaryText.setText("Generating weekly summary...");
        weeklyTip.setText("Preparing personalized tips...");
        conclusionText.setText("");

        startFadeAnimation(scoreText);
        startFadeAnimation(tipText);
        startFadeAnimation(summaryText);
        startFadeAnimation(weeklyTip);
    }

    private void startFadeAnimation(View view) {
        AlphaAnimation anim = new AlphaAnimation(0.3f, 1.0f);
        anim.setDuration(800);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(Animation.INFINITE);
        view.startAnimation(anim);
    }

    private void stopLoadingAnimation() {
        scoreText.clearAnimation();
        tipText.clearAnimation();
        summaryText.clearAnimation();
        weeklyTip.clearAnimation();
    }
    private void handleWeeklyRollover(List<Tasks> tasksList) {

        int thisWeek = getWeekNumber(new Date());

        // FIRST TIME → initialize and exit
        if (lastSavedWeek == -1) {
            lastSavedWeek = thisWeek;
            return;
        }

        // WEEK CHANGED → reset the weekly chart
        if (thisWeek != lastSavedWeek) {

            // reset chart by clearing weekly counts
            currentWeekCompleted = 0;
            currentWeekMissed = 0;

            lastSavedWeek = thisWeek;

            // force refresh of weekly chart
            renderWeeklyChart(new int[]{0,0,0,0,0,0,0});
            return;
        }
    }

    private int getWeekNumber(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return c.get(Calendar.WEEK_OF_YEAR);
    }


    private int countWeeklyCompleted(List<Tasks> tasksList, boolean previousWeek) {
        Calendar cal = Calendar.getInstance();
        int thisWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int thisYear = cal.get(Calendar.YEAR);

        int targetWeek = previousWeek ? thisWeek - 1 : thisWeek;

        // Handle year rollover when thisWeek == 1 and previous week is last week of last year
        if (targetWeek == 0) {
            cal.add(Calendar.YEAR, -1);
            targetWeek = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
            thisYear = cal.get(Calendar.YEAR);
        }

        int count = 0;
        Calendar taskCal = Calendar.getInstance();

        for (Tasks t : tasksList) {
            if (t == null || !t.isCompleted()) continue;

            Date comp = t.getCompletionDate();
            if (comp == null) continue;

            taskCal.setTime(comp);

            int taskWeek = taskCal.get(Calendar.WEEK_OF_YEAR);
            int taskYear = taskCal.get(Calendar.YEAR);

            if (taskWeek == targetWeek && taskYear == thisYear) {
                count++;
            }
        }

        return count;
    }
    private int countWeeklyMissed(List<Tasks> tasksList, boolean previousWeek) {
        Calendar cal = Calendar.getInstance();
        int thisWeek = cal.get(Calendar.WEEK_OF_YEAR);
        int thisYear = cal.get(Calendar.YEAR);

        int targetWeek = previousWeek ? thisWeek - 1 : thisWeek;

        if (targetWeek == 0) {
            cal.add(Calendar.YEAR, -1);
            targetWeek = cal.getActualMaximum(Calendar.WEEK_OF_YEAR);
            thisYear = cal.get(Calendar.YEAR);
        }

        int count = 0;
        Calendar taskCal = Calendar.getInstance();
        Date today = stripTime(new Date());

        for (Tasks t : tasksList) {
            if (t == null || t.isCompleted()) continue;

            Date end = t.getEndDate();
            if (end == null) continue;

            // Must be in past to count as missed
            if (!stripTime(end).before(today)) continue;

            taskCal.setTime(end);

            int taskWeek = taskCal.get(Calendar.WEEK_OF_YEAR);
            int taskYear = taskCal.get(Calendar.YEAR);

            if (taskWeek == targetWeek && taskYear == thisYear) {
                count++;
            }
        }

        return count;
    }



    public interface AiCallback {
        void onResult(String output);
        void onError(String error);
    }

}
