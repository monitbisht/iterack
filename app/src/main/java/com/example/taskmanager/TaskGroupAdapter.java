package com.example.taskmanager;

import android.content.Context;
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

        heading.setText(group.name);
        int percentage =  (int) ((group.completedTasks * 100.0f) / group.totalTasks);
        taskRatio.setText(percentage + "%");

        int progress = (int) ((group.completedTasks * 100.0f) / group.totalTasks);
        progressBar.setProgress(progress);

        groupIcon.setImageResource(group.profileIcon);


        return view;
    }
}
