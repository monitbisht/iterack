package io.github.monitbisht.iterack;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final Context context;
    private final ArrayList<Tasks> tasksList;

    Date today = stripTime(new Date());

    private String dot = "\u2022";

    public TasksAdapter(Context context, ArrayList<Tasks> tasksList) {
        this.context = context;
        this.tasksList = tasksList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.tasks_card_layout, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Tasks task = tasksList.get(position);

        // Update status based on today's date
        task.updateStatus(new Date());

        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDescription.setText(task.getTaskDescription());
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());
        holder.moreBtn.setOnClickListener(null);
        holder.taskDueDate.setText(task.getTaskGroup());
        holder.taskGroup.setText(dot + " " + task.getTaskGroup());


        updateDueDateText(holder, task);
        updateTaskUI(holder, task);

        // Handle checkbox click
        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {


            if (task.getStatus().equals("Missed") && isChecked) {
                Toast.makeText(context, "Missed tasks can’t be marked completed.", Toast.LENGTH_SHORT).show();
                holder.taskCheckbox.setChecked(false);
                return;
            }
            // If task passed end date → user can't change status
            if (today.after(task.getEndDate())) {
                holder.taskCheckbox.setChecked(task.isCompleted()); // revert UI
                Toast.makeText(context, "Task past end date so can’t update.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isChecked) {
                task.setCompleted(true);
                task.setCompletionDate(new Date());
            } else {
                task.setCompleted(false);
                task.setCompletionDate(null);
            }

            task.updateStatus(new Date());


            FireStoreHelper.getInstance().updateTask(task, new FireStoreHelper.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void r) {
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText(context, "Update failed", Toast.LENGTH_SHORT).show();
                }
            });

            notifyItemChanged(holder.getAdapterPosition());
        });

        // Click listener only for non-missed tasks
        if (!task.getStatus().equals("Missed") && !today.after(stripTime(stripTime(task.getEndDate())))){
            holder.moreBtn.setOnClickListener(v ->
                    showPopupMenu(v, holder.getAdapterPosition())
            );
        } else {
            holder.moreBtn.setOnClickListener(null); // disable click
        }
    }

    private void updateDueDateText(TaskViewHolder holder, Tasks task) {
        Date endDate = task.getEndDate();
        if (endDate == null) {
            holder.taskDueDate.setText("");
            return;
        }

        Calendar today = Calendar.getInstance();
        Calendar due = Calendar.getInstance();
        due.setTime(endDate);

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        due.set(Calendar.HOUR_OF_DAY, 0);
        due.set(Calendar.MINUTE, 0);
        due.set(Calendar.SECOND, 0);
        due.set(Calendar.MILLISECOND, 0);

        long diff = due.getTimeInMillis() - today.getTimeInMillis();
        long days = diff / (24 * 60 * 60 * 1000);

        if (days == 1)
            holder.taskDueDate.setText("Due Tomorrow");
        else if (days == 0)
            holder.taskDueDate.setText("Due Today");
        else if (days < 0)
            holder.taskDueDate.setText("Past Due");
        else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            holder.taskDueDate.setText("Due " + sdf.format(endDate));
        }
    }

    private void updateTaskUI(TaskViewHolder holder, Tasks task) {
        int borderColor;
        int pillColor;
        int backgroundColor = ContextCompat.getColor(context, R.color.dark_charcoal);

        holder.itemView.setAlpha(1f);

        // Completed
        if (task.isCompleted()) {
            borderColor = ContextCompat.getColor(context, R.color.fresh_green);
            pillColor = borderColor;
            holder.taskStatus.setText("Completed");

            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskTitle.setTextColor(context.getColor(R.color.medium_gray));
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskDueDate.setText(task.getTaskGroup());
            holder.taskGroup.setText("");


        } else {

            String status = task.getStatus();

            switch (status) {
                case "Missed":
                    borderColor = ContextCompat.getColor(context, R.color.poppy_red);
                    pillColor = borderColor;
                    holder.taskStatus.setText("Missed");
                    holder.taskCheckbox.setClickable(false);
                    holder.moreBtn.setColorFilter(R.color.dark_gray);
                    holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    holder.taskCheckbox.setButtonTintList(
                            ContextCompat.getColorStateList(context, R.color.dark_gray)
                    );
                    break;

                case "Active":
                    borderColor = ContextCompat.getColor(context, R.color.medium_gray);
                    pillColor = borderColor;
                    holder.taskStatus.setText("Active");
                    pillColor = ContextCompat.getColor(context, R.color.sky_blue);
                    holder.taskCheckbox.setButtonTintList(
                            ContextCompat.getColorStateList(context, R.color.medium_gray)
                    );
                    break;

                default:
                    borderColor = ContextCompat.getColor(context, R.color.medium_gray);
                    pillColor = ContextCompat.getColor(context, R.color.sunny_yellow);
                    holder.taskStatus.setText("Upcoming");
                    SimpleDateFormat sdf = new SimpleDateFormat("dd MMM", Locale.getDefault());
                    holder.taskDueDate.setText("Scheduled for " + sdf.format(task.getStartDate()));
                    holder.taskCheckbox.setButtonTintList(
                            ContextCompat.getColorStateList(context, R.color.medium_gray)
                    );
                    break;
            }
        }

        // Apply visuals
        holder.cardRoot.setStrokeColor(borderColor);
        holder.cardRoot.setCardBackgroundColor(backgroundColor);

        Drawable bg = holder.taskStatus.getBackground().mutate();
        bg.setTint(pillColor);
    }

    private void showPopupMenu(View view, int position) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.task_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, position));
        popup.show();
    }

    private boolean handleMenuItemClick(MenuItem item, int position) {
        int id = item.getItemId();

        if (id == R.id.action_delete) {
            showDeleteConfirmation(position);
            return true;
        }

        if (id == R.id.action_edit) {
            showEditBottomSheet(position);
            return true;
        }

        return false;
    }

    @Override
    public int getItemCount() {
        return tasksList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDescription, taskStatus, taskDueDate, taskGroup;
        CheckBox taskCheckbox;
        ImageButton moreBtn;
        MaterialCardView cardRoot;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.task_text);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskStatus = itemView.findViewById(R.id.task_status);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            taskDueDate = itemView.findViewById(R.id.task_due_date);
            moreBtn = itemView.findViewById(R.id.btn_more);
            cardRoot = itemView.findViewById(R.id.task_card_root);
            taskGroup = itemView.findViewById(R.id.task_group);

        }

    }

    private void showDeleteConfirmation(int position) {

        Tasks t = tasksList.get(position);

            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Delete", (dialog, which) -> {


                        FireStoreHelper.getInstance().deleteTask(t.getTaskId(), new FireStoreHelper.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void r) {
                                tasksList.remove(position);
                                notifyItemRemoved(position);
                                Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(Exception e) {
                                Toast.makeText(context, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        }

    private void showEditBottomSheet(int position) {

        Tasks task = tasksList.get(position);

            BottomSheetDialog dialog = new BottomSheetDialog(context);

            View sheet = LayoutInflater.from(context).inflate(R.layout.edit_task_bottomsheet, null);
            dialog.setContentView(sheet);

            EditText name = sheet.findViewById(R.id.editTaskName);
            EditText desc = sheet.findViewById(R.id.editTaskDescription);

            MaterialButton work = sheet.findViewById(R.id.editWorkBtn);
            MaterialButton personal = sheet.findViewById(R.id.editPersonalBtn);
            MaterialButton health = sheet.findViewById(R.id.editHealthBtn);
            MaterialButton study = sheet.findViewById(R.id.editStudyBtn);

            MaterialButton startBtn = sheet.findViewById(R.id.editStartDateBtn);
            MaterialButton endBtn = sheet.findViewById(R.id.editEndDateBtn);
            MaterialButton saveBtn = sheet.findViewById(R.id.saveEditedTaskBtn);

            // Fill existing data
            name.setText(task.getTaskTitle());
            desc.setText(task.getTaskDescription());
            startBtn.setText(formatDate(task.getStartDate()));
            endBtn.setText(formatDate(task.getEndDate()));

            final String[] group = {task.getTaskGroup()};

            highlightGroupButton(group[0], work, personal, health, study);

            // Group selection
            View.OnClickListener groupClick = v -> {
                work.setChecked(false);
                personal.setChecked(false);
                health.setChecked(false);
                study.setChecked(false);

                MaterialButton b = (MaterialButton) v;
                b.setChecked(true);
                group[0] = b.getText().toString();
            };

            work.setOnClickListener(groupClick);
            personal.setOnClickListener(groupClick);
            health.setOnClickListener(groupClick);
            study.setOnClickListener(groupClick);

            // Date pickers
            startBtn.setOnClickListener(v -> pickDate(startBtn));
            endBtn.setOnClickListener(v -> pickDate(endBtn));

            // Save Edited Task
            saveBtn.setOnClickListener(v -> {

                task.setTaskTitle(name.getText().toString().trim());
                task.setTaskDescription(desc.getText().toString().trim());
                task.setTaskGroup(group[0]);

                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
                    task.setStartDate(sdf.parse(startBtn.getText().toString()));
                    task.setEndDate(sdf.parse(endBtn.getText().toString()));
                } catch (Exception ignored) {
                }

                FireStoreHelper.getInstance().updateTask(task, new FireStoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void r) {
                        notifyItemChanged(position);
                        Toast.makeText(context, "Task updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(context, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            });

            dialog.show();
        }


    private void pickDate(MaterialButton btn) {
        Calendar cal = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
                context,
                (view, y, m, d) -> btn.setText(d + "/" + (m + 1) + "/" + y),
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy").format(date);
    }

    private void highlightGroupButton(String group, MaterialButton... buttons) {
        for (MaterialButton b : buttons) {
            b.setChecked(b.getText().toString().equals(group));
        }
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

