package com.example.taskmanager;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


public class PlannerFragment extends Fragment {


    RecyclerView recyclerView;
    public PlannerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_planner, container, false);
    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.planner_footer);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 2. Sample tasks
        ArrayList<Tasks> taskList = new ArrayList<>();
        Date now = new Date();

        //Setting dates
        Calendar c1 = Calendar.getInstance();
        c1.set(2025, Calendar.OCTOBER, 29);
        Date startDate1 = c1.getTime();

        Calendar c2 = Calendar.getInstance();
        c2.set(2025, Calendar.OCTOBER, 30);
        Date endDate1 = c2.getTime();

        Calendar c3 = Calendar.getInstance();
        c1.set(2025, Calendar.OCTOBER, 30);
        Date startDate2 = c1.getTime();

        Calendar c4 = Calendar.getInstance();
        c2.set(2025, Calendar.NOVEMBER, 1);
        Date endDate2 = c2.getTime();


        taskList.add(new Tasks("Finish Assignment", "Math homework", false, startDate1, endDate1));
        taskList.add(new Tasks("Project Meeting", "Discuss Android app", false, startDate2, endDate2));


        // 3. Set adapter
        TasksAdapter adapter = new TasksAdapter(getContext(), taskList);
        recyclerView.setAdapter(adapter);
    }

}