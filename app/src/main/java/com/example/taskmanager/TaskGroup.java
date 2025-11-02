package com.example.taskmanager;

public class TaskGroup {
    public String name;
    public int completedTasks;
    public int totalTasks;

    public int profileIcon;

    public TaskGroup(String name, int completedTasks, int totalTasks , int icon) {
        this.name = name;
        this.completedTasks = completedTasks;
        this.totalTasks = totalTasks;
        this.profileIcon = icon;

    }
}
