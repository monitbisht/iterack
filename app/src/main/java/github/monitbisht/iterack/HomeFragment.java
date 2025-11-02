package github.monitbisht.iterack;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import io.github.monitbisht.iterack.R;


public class HomeFragment extends Fragment {

    MaterialButton viewTaskButton;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        GridView gridView = view.findViewById(R.id.taskGroups_gridview);
        viewTaskButton = view.findViewById(R.id.viewProgress_button);

        List<TaskGroup> taskGroups = new ArrayList<>();
        taskGroups.add(new TaskGroup("Personal", 5, 10 , R.drawable.ic_personal));
        taskGroups.add(new TaskGroup("Work", 3, 5 , R.drawable.ic_work));
        taskGroups.add(new TaskGroup("Fitness", 2, 4 , R.drawable.ic_health));
        taskGroups.add(new TaskGroup("Study",4,8 , R.drawable.ic_study));

        TaskGroupAdapter adapter = new TaskGroupAdapter(getContext(), taskGroups);
        gridView.setAdapter(adapter);
        return view;

      //  viewTaskButton.setOnClickListener(v -> {
          //  showTodayTasksDialog();
       // });

    }



}
