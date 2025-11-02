package com.example.taskmanager;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class TasksAdapter extends RecyclerView.Adapter<TasksAdapter.TaskViewHolder> {

    private final Context context;
    private final ArrayList<Tasks> tasksList;

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
        task.updateStatus(new Date());

        holder.taskTitle.setText(task.getTaskTitle());
        holder.taskDescription.setText(task.getTaskDescription());
        holder.taskCheckbox.setOnCheckedChangeListener(null);
        holder.taskCheckbox.setChecked(task.isCompleted());

        // Show proper due date message
        updateDueDateText(holder, task);

        // Handle checkbox click
        holder.taskCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (task.getStatus() == Tasks.TaskStatus.Missed && isChecked) {
                Toast.makeText(context, "Missed tasks can’t be marked completed.", Toast.LENGTH_SHORT).show();
                holder.taskCheckbox.setChecked(false);
                return;
            }
            task.setCompleted(isChecked);
            notifyItemChanged(holder.getAdapterPosition());
        });

        // Overflow menu
        holder.moreBtn.setOnClickListener(v -> showPopupMenu(v, holder.getAdapterPosition()));

        // Update UI based on task state
        updateTaskUI(holder, task);
    }

    private void updateDueDateText(TaskViewHolder holder, Tasks task) {
        Date endDate = task.getEndDate();
        if (endDate == null) {
            holder.taskDueDate.setText("");
            return;
        }

        Calendar today = Calendar.getInstance();
        Calendar dueDate = Calendar.getInstance();
        dueDate.setTime(endDate);

        // Reset time to midnight for fair comparison
        for (Calendar c : new Calendar[]{today, dueDate}) {
            c.set(Calendar.HOUR_OF_DAY, 0);
            c.set(Calendar.MINUTE, 0);
            c.set(Calendar.SECOND, 0);
            c.set(Calendar.MILLISECOND, 0);
        }

        long diff = dueDate.getTimeInMillis() - today.getTimeInMillis();
        long daysDiff = diff / (24 * 60 * 60 * 1000);

        if (daysDiff == 1) {
            holder.taskDueDate.setText("Due Tomorrow");
        } else if (daysDiff == 0) {
            holder.taskDueDate.setText("Due Today");
        } else if (daysDiff < 0) {
            holder.taskDueDate.setText("Past Due");
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            holder.taskDueDate.setText("Due " + sdf.format(endDate));
        }
    }

    private void updateTaskUI(TaskViewHolder holder, Tasks task) {
        int borderColor;
        int pillColor;
        int backgroundColor = ContextCompat.getColor(context, R.color.dark_charcoal);

        // Reset strikethrough
        holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));

        // Base colors
        int textNormal = ContextCompat.getColor(context, R.color.off_white);
        int textGray = ContextCompat.getColor(context, R.color.medium_gray);

        if (task.isCompleted()) {
            // Completed
            borderColor = ContextCompat.getColor(context, R.color.fresh_green);
            pillColor = borderColor;
            holder.taskStatus.setText("Completed");

            holder.taskTitle.setPaintFlags(holder.taskTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.taskDescription.setPaintFlags(holder.taskDescription.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

            // Dull colors for completed tasks
            holder.itemView.setAlpha(0.5f);
            holder.taskTitle.setTextColor(textGray);
            holder.taskDescription.setTextColor(textGray);
            holder.taskDueDate.setTextColor(textGray);
        } else {
            holder.itemView.setAlpha(1f);
            holder.taskTitle.setTextColor(textNormal);
            holder.taskDescription.setTextColor(textGray);
            holder.taskDueDate.setTextColor(textGray);

            switch (task.getStatus()) {
                case Missed:
                    borderColor = ContextCompat.getColor(context, R.color.poppy_red);
                    pillColor = borderColor;
                    holder.taskStatus.setText("Missed");
                    holder.taskCheckbox.setButtonTintList(ContextCompat.getColorStateList(context, R.color.dark_gray));
                    holder.taskCheckbox.setClickable(false);
                    holder.moreBtn.setClickable(false);
                    break;
                case Active:
                    borderColor = ContextCompat.getColor(context, R.color.medium_gray);
                    pillColor = ContextCompat.getColor(context, R.color.medium_gray);
                    holder.taskStatus.setText("Active");
                    break;
                case Upcoming:
                default:
                    borderColor = ContextCompat.getColor(context, R.color.medium_gray);
                    pillColor = ContextCompat.getColor(context, R.color.medium_gray);
                    holder.taskStatus.setText("Upcoming");
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
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.task_item_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> handleMenuItemClick(item, position));
        popup.show();
    }

    private boolean handleMenuItemClick(MenuItem item, int position) {
        int id = item.getItemId();
        if (id == R.id.action_edit) {
            Toast.makeText(context, "Edit clicked for " + tasksList.get(position).getTaskTitle(), Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_delete) {
            tasksList.remove(position);
            notifyItemRemoved(position);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getItemCount() {
        return tasksList.size();
    }

    public static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle, taskDescription, taskStatus, taskDueDate;
        CheckBox taskCheckbox;
        ImageButton moreBtn;
        MaterialCardView cardRoot;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.task_text);
            taskDescription = itemView.findViewById(R.id.task_description);
            taskStatus = itemView.findViewById(R.id.task_status);
            taskCheckbox = itemView.findViewById(R.id.taskCheckbox);
            moreBtn = itemView.findViewById(R.id.btn_more);
            cardRoot = itemView.findViewById(R.id.task_card_root);
            taskDueDate = itemView.findViewById(R.id.task_due_date);
        }
    }
}
