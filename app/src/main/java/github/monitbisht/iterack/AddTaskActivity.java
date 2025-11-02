package github.monitbisht.iterack;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.github.monitbisht.iterack.R;
import io.github.monitbisht.iterack.databinding.ActivityAddTaskBinding;
import com.google.android.material.button.MaterialButton;

public class AddTaskActivity extends AppCompatActivity {

    private ActivityAddTaskBinding binding;
    ImageView imageView;
    MaterialButton workButton, personalButton, healthButton, studyButton;

    MaterialButton startDateBtn, endDateBtn;

    MaterialButton submitButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        imageView = findViewById(R.id.back_arrow);
        workButton = findViewById(R.id.work_button);
        personalButton = findViewById(R.id.personal_button);
        healthButton = findViewById(R.id.health_button);
        studyButton = findViewById(R.id.study_button);
        startDateBtn = findViewById(R.id.startDate_button);
        endDateBtn = findViewById(R.id.endDate_button);
        submitButton = findViewById(R.id.addProject_SubmitButton);

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


                ((MaterialButton) v).setChecked(true);
            }
        };

        workButton.setOnClickListener(buttonClickListener);
        personalButton.setOnClickListener(buttonClickListener);
        healthButton.setOnClickListener(buttonClickListener);
        studyButton.setOnClickListener(buttonClickListener);

        startDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(AddTaskActivity.this);
                datePickerDialog.show();
                datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = dayOfMonth + "/" + month + "/" + year;
                        startDateBtn.setText(date);
                    }

                });
            }
        });

        endDateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DatePickerDialog datePickerDialog = new DatePickerDialog(AddTaskActivity.this);
                datePickerDialog.show();
                datePickerDialog.setOnDateSetListener(new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        String date = dayOfMonth + "/" + month + "/" + year;
                        endDateBtn.setText(date);
                    }

                });
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AddTaskActivity.this, MainActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation);
                Toast toast = Toast.makeText(AddTaskActivity.this, "Project Added Successfully", Toast.LENGTH_SHORT);
                toast.show();   
            }
        });

    }

}
