package io.github.monitbisht.iterack;

import java.util.Calendar;
import java.util.Date;

public class Tasks {

    private String taskId;           // Firestore document ID
    private String taskTitle;
    private String taskDescription;
    private String taskGroup;        // Work / Personal / Study / Health

    private boolean isCompleted;
    private String status;           // "Upcoming", "Active", "Missed", "Completed"

    private Date startDate;
    private Date endDate;

    private Date createdOn;



    // Required empty constructor for Firestore
    public Tasks() {}


    // Constructor used when adding a new task
    public Tasks(String taskTitle, String taskDescription, String taskGroup,
                 Date startDate, Date endDate) {

        this.taskTitle = taskTitle;
        this.taskDescription = taskDescription;
        this.taskGroup = taskGroup;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isCompleted = false;
        this.status = "Upcoming";
        this.createdOn = new Date();
    }

    // GETTERS
    public String getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public String getTaskDescription() { return taskDescription; }
    public String getTaskGroup() { return taskGroup; }
    public boolean isCompleted() { return isCompleted; }
    public String getStatus() { return status; }
    public Date getStartDate() { return startDate; }
    public Date getEndDate() { return endDate; }
    public Date getCreatedOn() { return createdOn; }


    // SETTERS (needed for Firestore)
    public void setTaskId(String taskId) { this.taskId = taskId; }
    public void setTaskTitle(String taskTitle) { this.taskTitle = taskTitle; }
    public void setTaskDescription(String taskDescription) { this.taskDescription = taskDescription; }
    public void setTaskGroup(String taskGroup) { this.taskGroup = taskGroup; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    public void setStatus(String status) { this.status = status; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public void setEndDate(Date endDate) { this.endDate = endDate; }

    // STATUS CALCULATION
    public void updateStatus(Date today) {

        Date cleanToday = stripTime(today);
        Date cleanStart = stripTime(startDate);
        Date cleanEnd = stripTime(endDate);

        if (isCompleted) {
            status = "Completed";
            return;
        }

        if (cleanToday.before(cleanStart)) {
            status = "Upcoming";
        }
        else if (cleanToday.after(cleanEnd)) {
            status = "Missed";
        }
        else {
            status = "Active";
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
