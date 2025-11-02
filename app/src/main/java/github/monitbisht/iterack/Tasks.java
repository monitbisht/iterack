    package github.monitbisht.iterack;

    import java.util.Date;

    public class Tasks {

        private String taskTitle;
        private String taskDescription;
        private boolean isCompleted;

        public void setCompleted(boolean isChecked) {
            this.isCompleted = isChecked;
        }

        public Date getStartDate() {
            return startDate;
        }

        public enum TaskStatus {
            Missed,
            Active,
           Upcoming

        }

        private TaskStatus  status;

        private Date startDate;
        private Date endDate;



        public boolean isCompleted() {
            return isCompleted;
        }

        public String getTaskDescription() {
            return taskDescription;
        }

        public String getTaskTitle() {
            return taskTitle;
        }

        public TaskStatus getStatus() {
            return status;
        }

        public Date getEndDate(){
            return endDate;
        }

        public void updateStatus(Date currentDate) {
            if (currentDate.before(startDate)) {
                status = TaskStatus.Upcoming; // start date not reached
            } else if (currentDate.after(endDate)) {
                status = TaskStatus.Missed; // deadline passed
            } else {
                status = TaskStatus.Active; // current date between start & end
            }
        }



        public Tasks(String taskTitle, String taskDescription, boolean isCompleted , Date startDate , Date endDate) {
            this.taskTitle = taskTitle;
            this.taskDescription = taskDescription;
            this.isCompleted = isCompleted;
            this.startDate = startDate;
            this.endDate = endDate;
        }
    }
