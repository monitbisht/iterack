package io.github.monitbisht.iterack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.github.monitbisht.iterack.databinding.ActivityAddTaskBinding;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class AddTaskActivity extends AppCompatActivity {

    private ActivityAddTaskBinding binding;
    private ImageView imageView;

    private EditText taskName, taskDescription;

    private MaterialButton workButton, personalButton, healthButton, studyButton;

    private MaterialButton startDateBtn, endDateBtn;

    private MaterialButton submitButton;

    private String selectedGroup;

    private Integer savedStartYear = null, savedStartMonth = null, savedStartDay = null;
    private Integer savedEndYear = null, savedEndMonth = null, savedEndDay = null;





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Inputs
         taskName = binding.projectName;
         taskDescription = binding.projectDescription;

       // Group buttons
         workButton = binding.workButton;
         personalButton = binding.personalButton;
         healthButton = binding.healthButton;
         studyButton = binding.studyButton;
       // Date buttons
         startDateBtn = binding.startDateButton;
         endDateBtn = binding.endDateButton;

       // Submit button
         submitButton = binding.addProjectSubmitButton;

       // Back button
         imageView = binding.backArrow;


        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);

            }
        });

        View.OnClickListener buttonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                workButton.setChecked(false);
                personalButton.setChecked(false);
                healthButton.setChecked(false);
                studyButton.setChecked(false);


                MaterialButton clickedBtn = (MaterialButton) v;
                clickedBtn.setChecked(true);

                selectedGroup = clickedBtn.getText().toString();
            }
        };

        workButton.setOnClickListener(buttonClickListener);
        personalButton.setOnClickListener(buttonClickListener);
        healthButton.setOnClickListener(buttonClickListener);
        studyButton.setOnClickListener(buttonClickListener);



        startDateBtn.setOnClickListener(v -> {

            final Calendar cal = Calendar.getInstance();
            int year = (savedStartYear != null) ? savedStartYear : cal.get(Calendar.YEAR);
            int month = (savedStartMonth != null) ? savedStartMonth : cal.get(Calendar.MONTH);
            int day = (savedStartDay != null) ? savedStartDay : cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    AddTaskActivity.this,
                    (view, y, m, d) -> {
                        savedStartYear = y;
                        savedStartMonth = m;
                        savedStartDay = d;

                        String date = d + "/" + (m + 1) + "/" + y;
                        startDateBtn.setText(date);
                    },
                    year, month, day
            );

            dialog.show();
        });

        endDateBtn.setOnClickListener(v -> {

            final Calendar cal = Calendar.getInstance();
            int year = (savedStartYear != null) ? savedStartYear : cal.get(Calendar.YEAR);
            int month = (savedStartMonth != null) ? savedStartMonth : cal.get(Calendar.MONTH);
            int day = (savedStartDay != null) ? savedStartDay : cal.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dialog = new DatePickerDialog(
                    AddTaskActivity.this,
                    (view, y, m, d) -> {
                        savedEndYear = y;
                        savedEndMonth = m;
                        savedEndDay = d;

                        String date = d + "/" + (m + 1) + "/" + y;
                        endDateBtn.setText(date);
                    },
                    year, month, day
            );

            dialog.show();
        });


        submitButton.setOnClickListener(v -> {

            String taskTitle = taskName.getText().toString().trim();
            String description = taskDescription.getText().toString().trim();
            String startDateText = startDateBtn.getText().toString();
            String endDateText = endDateBtn.getText().toString();
            String group = selectedGroup;


            // VALIDATION PART
            if (taskTitle.isEmpty()) {
                Toast.makeText(this, "Enter task name", Toast.LENGTH_SHORT).show();
                return;
            }

            if (group == null || group.isEmpty()) {
                Toast.makeText(this, "Select a project group", Toast.LENGTH_SHORT).show();
                return;
            }

            if (startDateText.equals("Start Date")) { // default button text
                Toast.makeText(this, "Select start date", Toast.LENGTH_SHORT).show();
                return;
            }

            if (endDateText.equals("End Date")) {
                Toast.makeText(this, "Select end date", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check date validity
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            Date startDate, endDate;
            Date today = stripTime(new Date());


            try {
                startDate = sdf.parse(startDateText);
                endDate = sdf.parse(endDateText);

                if (startDate.before(today)) {
                    Toast.makeText(this, "Start date can't be before today", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (endDate.before(startDate)) {
                    Toast.makeText(this, "End date cannot be earlier than start date", Toast.LENGTH_SHORT).show();
                    return;
                }

            } catch (Exception e) {
                Toast.makeText(this, "Invalid date selected", Toast.LENGTH_SHORT).show();
                return;
            }

            // If everything is valid → upload
            Tasks task = new Tasks(taskTitle, description, group, startDate, endDate);

            FireStoreHelper.getInstance().addTask(task, new FireStoreHelper.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Toast.makeText(AddTaskActivity.this, "Task added", Toast.LENGTH_SHORT).show();
                    finish();
                }

                @Override
                public void onError(Exception e) {
                    Toast.makeText( AddTaskActivity.this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        });



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
