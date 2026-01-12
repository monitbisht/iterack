    package io.github.monitbisht.iterack;

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

        // Updates the selected date when user clicks the calendar UI
        private void setupCalendar() {
            calendarView.setOnCalendarDayClickListener(new OnCalendarDayClickListener() {
                @Override
                public void onClick(@NonNull CalendarDay calendarDay) {
                    Calendar clicked = calendarDay.getCalendar();
                    selectedDate = clicked.getTime();

                    // Re-apply current filter for the new date
                    applyFilter(filterChip.getText().toString());
                }

            });
        }

        // Pop-up menu for filtering tasks (Active, Missed, etc.)
        private void setupFilterMenu() {
            filterChip.setOnClickListener(v -> {

                PopupMenu popup = new PopupMenu(getContext(), v);
                Date today = stripTime(new Date());

                // Add standard filters
                popup.getMenu().add("All");
                popup.getMenu().add("Active");
                popup.getMenu().add("Missed");
                popup.getMenu().add("Completed");

                // Only show future-related filters if selected date is today or later
                if (!selectedDate.before(today)) {
                    popup.getMenu().add("Upcoming");
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


        // Fetches all tasks from Firestore once, then filters locally
        private void loadTasks() {

            FireStoreHelper.getInstance().getAllTasks(new FireStoreHelper.FirestoreCallback<ArrayList<Tasks>>() {
                @Override
                public void onSuccess(ArrayList<Tasks> taskList) {

                    allTasks = taskList; // Store for local filtering

                    // Ensure status is up-to-date
                    Date today = new Date();
                    for (Tasks t : allTasks) {
                        t.updateStatus(today);
                    }

                    // Handle Empty State
                    if (taskList.isEmpty()) {
                        noTasksText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        noTasksText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);

                        tasksAdapter = new TasksAdapter(getContext(), allTasks);
                        recyclerView.setAdapter(tasksAdapter);
                    }

                    // Apply default filter
                    applyFilter(filterChip.getText().toString());
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(getContext(), "Failed to load tasks", Toast.LENGTH_SHORT).show();
                }
            });
        }


        // Core filtering engine (Compares Task Dates vs Selected Date)
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
                Date today = stripTime(new Date());

                // Check if selected date falls within task duration
                boolean inRange = !selectedDate.before(start) && !selectedDate.after(end);
                boolean isMissed = selectedDate.after(end);
                boolean isCompleted = t.isCompleted();
                boolean isMissedOnSelectedDate = isMissed && isCreatedOnDate(t, selectedDate);

                // Filter Switch Case
                switch (filterType) {

                    case "New":
                        if (created != null && created.equals(selectedDate))
                            filtered.add(t);
                        break;

                    case "Created Earlier":
                        if (created != null
                                && created.before(selectedDate)
                                && !selectedDate.before(start)
                                && !selectedDate.after(end))
                            filtered.add(t);
                        break;

                    case "Upcoming":
                        if (start.after(today))
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

                    default:    // "All" case
                        if (inRange)
                            filtered.add(t);
                        break;
                }
            }

            // Update UI
            tasksAdapter = new TasksAdapter(getContext(), filtered);
            recyclerView.setAdapter(tasksAdapter);

            noTasksText.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        }


        // Helper: Check if a task was created on a specific date
        private boolean isCreatedOnDate(Tasks t, Date date) {
            if (t.getCreatedOn() == null) return false;

            Calendar c1 = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();
            c1.setTime(t.getCreatedOn());
            c2.setTime(date);

            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                    && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
        }

        // Helper: Normalize date to midnight
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