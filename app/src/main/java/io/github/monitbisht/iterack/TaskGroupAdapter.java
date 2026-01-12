package io.github.monitbisht.iterack;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TaskGroupAdapter extends BaseAdapter {
    Context context;
    List<TaskGroup> taskGroupList;
    LayoutInflater inflater;

    public TaskGroupAdapter(Context context, List<TaskGroup> taskGroupList) {
        this.context = context;
        this.taskGroupList = taskGroupList;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return taskGroupList.size();
    }

    @Override
    public Object getItem(int position) {
        return taskGroupList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        view = inflater.inflate(R.layout.task_groups, null);

        TextView heading = view.findViewById(R.id.taskGroup_heading);
        TextView taskRatio = view.findViewById(R.id.taskRatio);
        ProgressBar progressBar = view.findViewById(R.id.progressBar);
        CircleImageView groupIcon = view.findViewById(R.id.task_group_icon);

        TaskGroup group = taskGroupList.get(position);

        // Set Data
        heading.setText(group.name);
        taskRatio.setText(String.format("%d / %d", group.completedTasks, group.totalTasks));

        // Calculate and Set Progress
        int progress = (int) ((group.completedTasks * 100.0f) / group.totalTasks);
        progressBar.setProgress(progress);

        groupIcon.setImageResource(group.profileIcon);

        // Styling
        Drawable bg = taskRatio.getBackground().mutate();
        bg.setTint(context.getResources().getColor(R.color.dark_gray));
        taskRatio.setTextColor(context.getResources().getColor(R.color.light_gray));

        return view;
    }
}