package github.monitbisht.iterack;

import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import io.github.monitbisht.iterack.R;


public class HomeFragment extends Fragment {

    MaterialButton viewTaskButton;

    GridView gridView;

    CircularProgressBar circularProgressBar;

    TextView progressBarText;

    private int totalTodaysTasks = 0;
    private int completedTodayTasks = 0;


    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);


        viewTaskButton = view.findViewById(R.id.viewProgress_button);
        gridView = view.findViewById(R.id.taskGroups_gridview);
        circularProgressBar = view.findViewById(R.id.circular_progress_bar);
        progressBarText = view.findViewById(R.id.progressPercentage);

        return view;
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTodayTasksDialog();
            }
        });

        loadTaskGroups();
        loadTaskProgress();
    }

    private void showTodayTasksDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.home_tasks_bottomsheet, null);
        bottomSheetDialog.setContentView(dialogView);

        ImageButton closeButton = dialogView.findViewById(R.id.close_button);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
            }
        });


        Date today = new Date();

        FireStoreHelper.getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {
            @Override
            public void onSuccess(ArrayList<Tasks> result) {
                LinearLayout tasksContainer = dialogView.findViewById(R.id.bottomSheetTaskContainer);


                tasksContainer.removeAllViews();

                boolean hasTasksForToday = false;


                for (Tasks t : result) {
                    t.updateStatus(today);

                    if (isTodayWithinRange(today, t.getStartDate(), t.getEndDate())) {
                        hasTasksForToday = true;

                        View taskRow = inflateBottomsheetTask(t);
                        tasksContainer.addView(taskRow);
                    }
                }

                // If no tasks for today, show toast
                if (!hasTasksForToday) {
                    Toast.makeText(getContext(), "No tasks for today", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    bottomSheetDialog.show();
                }
            }
            @Override
            public void onError(Exception e) {

            }
        });

    }

    private boolean isTodayWithinRange(Date today, Date start, Date end) {
        today = stripTime(today);
        start = stripTime(start);
        end = stripTime(end);

        return !today.before(start) && !today.after(end);
    }


    private View inflateBottomsheetTask(Tasks task) {
        View item = LayoutInflater.from(requireContext())
                .inflate(R.layout.bottomsheet_tasks_card_layout, null, false);

        MaterialCardView card = item.findViewById(R.id.task_card_root);
        TextView title = item.findViewById(R.id.task_text);
        TextView status = item.findViewById(R.id.task_status);
        CheckBox check = item.findViewById(R.id.taskCheckbox);

        // Flatten card for bottomsheet
        card.setStrokeWidth(0);
        card.setElevation(0);
        card.setRadius(12);

        title.setText(task.getTaskTitle());
        status.setText(task.getStatus());
        check.setChecked(task.isCompleted());

        // Correct pill color on load
        tintStatusPill(status, task.getStatus());

        // Green Checkbox for completed tasks
        if (task.getStatus().equals("Completed")) {
            check.setButtonTintList(
                    ContextCompat.getColorStateList(requireContext(), R.color.fresh_green)
            );
        }

        check.setOnCheckedChangeListener((btn, isChecked) -> {

            task.setCompleted(isChecked);

            if (isChecked) {
                // Completed
                task.setStatus("Completed");
                status.setText("Completed");
                tintStatusPill(status, "Completed");

                title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(requireContext().getColor(R.color.medium_gray));

                check.setButtonTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.fresh_green)
                );
                loadTaskProgress();
            } else {
                // Recalculate proper status from dates
                task.updateStatus(new Date());

                String newStatus = task.getStatus();
                status.setText(newStatus);

                // restore pill color
                tintStatusPill(status, newStatus);

                // restore title text style
                title.setPaintFlags(title.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                title.setTextColor(requireContext().getColor(R.color.off_white));

                // restore checkbox tint
                check.setButtonTintList(
                        ContextCompat.getColorStateList(requireContext(), R.color.medium_gray)
                );

                // Missed tasks should not be clickable
                if (newStatus.equals("Missed")) {
                    check.setClickable(false);
                    check.setButtonTintList(
                            ContextCompat.getColorStateList(requireContext(), R.color.dark_gray)
                    );
                }
                loadTaskProgress();
            }

            FireStoreHelper.updateTask(task, new FireStoreHelper.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void r) {
                    Log.d("TaskUpdate", "Updated successfully");
                }

                @Override
                public void onError(Exception e) {
                    Log.e("TaskUpdate", "Update failed: " + e.getMessage());
                }
            });
        });

        return item;
    }

    private void tintStatusPill(TextView pill, String status) {
        int color;

        switch (status) {
            case "Completed":
                color = requireContext().getColor(R.color.fresh_green);
                break;
            case "Missed":
                color = requireContext().getColor(R.color.poppy_red);
                break;
            case "Active":
                color = requireContext().getColor(R.color.sky_blue);
                break;
            default:
                color = requireContext().getColor(R.color.sunny_yellow);
        }

        Drawable bg = pill.getBackground().mutate();
        bg.setTint(color);
    }

    private void loadTaskGroups() {

        FireStoreHelper.getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {

            @Override
            public void onSuccess(ArrayList<Tasks> result) {

                int workTotal = 0, workCompleted = 0;
                int personalTotal = 0, personalCompleted = 0;
                int healthTotal = 0, healthCompleted = 0;
                int studyTotal = 0, studyCompleted = 0;

                for (Tasks t : result) {

                    // Skip missed tasks entirely
                    if (t.getStatus().equals("Missed")) {
                        continue;
                    }


                    switch (t.getTaskGroup()) {
                        case "Work":
                            workTotal++;
                            if (t.isCompleted()) workCompleted++;
                            break;

                        case "Personal":
                            personalTotal++;
                            if (t.isCompleted()) personalCompleted++;
                            break;

                        case "Health":
                            healthTotal++;
                            if (t.isCompleted()) healthCompleted++;
                            break;

                        case "Study":
                            studyTotal++;
                            if (t.isCompleted()) studyCompleted++;
                              break;

                        default:
                            Log.e("TaskDebug", "NO MATCH for group: " + t.getTaskGroup());
                            break;
                    }
                }


                List<TaskGroup> taskGroups = new ArrayList<>();
                taskGroups.add(new TaskGroup("Personal", personalCompleted, personalTotal, R.drawable.ic_personal));
                taskGroups.add(new TaskGroup("Work", workCompleted, workTotal, R.drawable.ic_work));
                taskGroups.add(new TaskGroup("Health", healthCompleted, healthTotal, R.drawable.ic_health));
                taskGroups.add(new TaskGroup("Study", studyCompleted, studyTotal, R.drawable.ic_study));

                TaskGroupAdapter adapter = new TaskGroupAdapter(getContext(), taskGroups);
                gridView.setAdapter(adapter);


            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load groups", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadTaskProgress(){


        FireStoreHelper.getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {

            @Override
            public void onSuccess(ArrayList<Tasks> result) {

                totalTodaysTasks = 0;
                completedTodayTasks = 0;
                Date today = new Date();


                for (Tasks t : result) {
                    t.updateStatus(today);

                    if (isTodayWithinRange(today, t.getStartDate(), t.getEndDate())) {
                        if (t.getStatus().equals("Completed")) completedTodayTasks++;
                        totalTodaysTasks++;

                    }
                }
                setProgressBar();
            }

            @Override
            public void onError(Exception e) {

            }
        });

        setProgressBar();
    }

    private void setProgressBar(){
        if (totalTodaysTasks == 0) {
            progressBarText.setText("0%");
            circularProgressBar.setProgress(0);
            return;
        }

        float percent = (completedTodayTasks * 100f) / totalTodaysTasks;
        progressBarText.setText((int) percent + "%");
        circularProgressBar.setProgress(percent);



    }


    @Override
    public void onResume() {
        super.onResume();
        loadTaskGroups();
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



}
