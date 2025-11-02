package com.example.taskmanager;

import android.graphics.Color;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;


import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;
import java.util.List;


public class InsightFragment extends Fragment {


    private BarChart barChart ;


    public InsightFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_insight, container, false);


    }

    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        barChart = view.findViewById(R.id.barChart);

        List<BarEntry> entries = new ArrayList<>();
        entries.add(new BarEntry(0, 18));
        entries.add(new BarEntry(1, 34));
        entries.add(new BarEntry(2, 28));
        entries.add(new BarEntry(3, 42));
        entries.add(new BarEntry(4, 30));
        entries.add(new BarEntry(5, 15));
        entries.add(new BarEntry(6, 10));

        BarDataSet dataSet = new BarDataSet(entries, "Tasks Completed");
        dataSet.setColor(Color.parseColor("#4A90E2")); // blue
        dataSet.setValueTextColor(Color.WHITE);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);

        barChart.setRenderer(new RoundedBarChartRenderer(barChart, 50f)); // 20f = corner radius


        // Customize
        String[] days = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(days));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(getResources().getColor(R.color.light_gray));
        xAxis.setTextSize(12);

        // Remove vertical grid lines (X-axis)
        barChart.getXAxis().setDrawGridLines(false);

        // Remove horizontal grid lines (Y-axis left)
        barChart.getAxisLeft().setDrawGridLines(false);



        barChart.getAxisLeft().setTextColor(getResources().getColor(R.color.light_gray));
        barChart.getAxisRight().setEnabled(false);
        barChart.getLegend().setEnabled(true);
        barChart.getDescription().setEnabled(false);


        barChart.setPinchZoom(false);
        barChart.setScaleEnabled(false);
        barChart.setDoubleTapToZoomEnabled(false);
        barChart.setExtraBottomOffset(20f);

        barChart.getLegend().setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        barChart.getLegend().setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        barChart.getLegend().setOrientation(Legend.LegendOrientation.HORIZONTAL);
        barChart.getLegend().setDrawInside(false);
        barChart.getLegend().setTextColor(getResources().getColor(R.color.light_gray)); // match theme

        barChart.invalidate();

        AutoCompleteTextView trendDropdown = view.findViewById(R.id.trendDropdown);
        String[] options = {"Weekly", "Monthly"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),   // or getContext()
                R.layout.dropdown_item,
                options
        );


        trendDropdown.setAdapter(adapter);
        trendDropdown.setText(options[0], false); // Default selection


    }
}