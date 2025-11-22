package github.monitbisht.iterack;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.listeners.OnCalendarDayClickListener;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.github.monitbisht.iterack.R;

public class PlannerFragment extends Fragment {

    private RecyclerView recyclerView;
    private Chip filterChip;
    private TextView noTasksText;

    private CalendarView calendarView;

    private Date selectedDate = new Date();

    private ArrayList<Tasks> allTasks = new ArrayList<>();
    private TasksAdapter tasksAdapter;


    public PlannerFragment() { }


    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_planner, container, false);
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.planner_footer);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        filterChip = view.findViewById(R.id.filterChip);
        noTasksText = view.findViewById(R.id.noTasksText);
        calendarView = view.findViewById(R.id.calendar  );

        selectedDate = stripTime(new Date());
        setupCalendar();
        setupFilterMenu();
        loadTasks();



    }

    private void setupCalendar() {
        calendarView.setOnCalendarDayClickListener(new OnCalendarDayClickListener() {
            @Override
            public void onClick(@NonNull CalendarDay calendarDay) {
                Calendar clicked = calendarDay.getCalendar();
                selectedDate = clicked.getTime();

                applyFilter(filterChip.getText().toString());
            }

        });
    }

        // Filter Chip Popup Menu (UI)

    private void setupFilterMenu() {
        filterChip.setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(getContext(), v);

            Date today = stripTime(new Date());

            popup.getMenu().add("All");
            popup.getMenu().add("Active");
            popup.getMenu().add("Missed");
            popup.getMenu().add("Completed");

            // Only visible when selected date >= today
            if (!selectedDate.before(today)) {
                popup.getMenu().add("New");
                popup.getMenu().add("Created Earlier");
            }

            popup.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                filterChip.setText(selected);
                applyFilter(selected);
                return true;
            });

            popup.show();
        });
    }



    //  Load tasks from Firestore

    private void loadTasks() {

        FireStoreHelper.getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {
            @Override
            public void onSuccess(ArrayList<Tasks> taskList) {

                allTasks = taskList; // store all tasks locally

                // Update status before showing
                Date today = new Date();
                for (Tasks t : allTasks) {
                    t.updateStatus(today);
                }

                if (taskList.isEmpty()) {
                    noTasksText.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noTasksText.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);

                    tasksAdapter = new TasksAdapter(getContext(), allTasks);
                    recyclerView.setAdapter(tasksAdapter);
                }

                applyFilter(filterChip.getText().toString());
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(getContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show();
            }
        });
    }


    // Apply Filters

    private void applyFilter(String filterType) {

        if (selectedDate == null) {
            selectedDate = new Date();
        }

        selectedDate = stripTime(selectedDate);
        ArrayList<Tasks> filtered = new ArrayList<>();

        for (Tasks t : allTasks) {

            Date start = stripTime(t.getStartDate());
            Date end = stripTime(t.getEndDate());
            Date created = t.getCreatedOn() != null ? stripTime(t.getCreatedOn()) : null;

            boolean inRange = !selectedDate.before(start) && !selectedDate.after(end);
            boolean isUpcoming = start.after(selectedDate);
            boolean isMissed = selectedDate.after(end);
            boolean isCompleted = t.isCompleted();
            boolean isMissedOnSelectedDate = isMissed && isCreatedOnDate(t, selectedDate);


            switch (filterType) {

                case "New":
                    if (created != null && created.equals(selectedDate))
                        filtered.add(t);
                    break;

                case "Created Earlier":
                    if (created != null
                            && created.before(selectedDate)
                            && start.equals(selectedDate))     // IMPORTANT FIX
                        filtered.add(t);
                    break;

                case "Upcoming":
                    if (isUpcoming
                            && created != null
                            && !created.after(selectedDate))   // don’t show future-created tasks
                        filtered.add(t);
                    break;

                case "Active":
                    if (inRange && !isCompleted)
                        filtered.add(t);
                    break;

                case "Missed":
                    if (isMissed && isMissedOnSelectedDate)
                        filtered.add(t);
                    break;

                case "Completed":
                    if (inRange && isCompleted)
                        filtered.add(t);
                    break;

                default:    // All
                    if (inRange)
                        filtered.add(t);
                    break;
            }
        }

        tasksAdapter = new TasksAdapter(getContext(), filtered);
        recyclerView.setAdapter(tasksAdapter);

        noTasksText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
    }


    // Helper: Check if a task was created today

    private boolean isCreatedOnDate(Tasks t, Date date) {

        if (t.getCreatedOn() == null) return false;

        Calendar c1 = Calendar.getInstance();
        Calendar c2 = Calendar.getInstance();

        c1.setTime(t.getCreatedOn());
        c2.setTime(date);

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
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
