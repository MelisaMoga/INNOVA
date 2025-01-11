package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.StatisticsActivityBinding;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;


public class StatisticsActivity extends AppCompatActivity {
    private StatisticsActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();


    // Method to load, rescale and apply transparency
    public Drawable createRescaledDrawable(int resourceId, float rescaleFactor, int alphaValue) {
        // Load the image from resources as a Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);

        // Rescale the Bitmap based on the rescale factor
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap,
                (int) (bitmap.getWidth() * rescaleFactor),
                (int) (bitmap.getHeight() * rescaleFactor),
                false);

        // Release memory from the original bitmap as it is no longer needed
        bitmap.recycle();

        // Convert the rescaled Bitmap to Drawable
        BitmapDrawable drawable = new BitmapDrawable(getResources(), scaledBitmap);

        // Apply transparency by setting the alpha value
        drawable.setAlpha(alphaValue); // 0 is fully transparent, 255 is fully opaque

        return drawable;
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = StatisticsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        PieChart pieChart = binding.pieChart;




        // Adding data
        float x = 0.07f;
        int alpha = 255;
        // Data for the chart
        ArrayList<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry(35f, "Walking", createRescaledDrawable(R.raw.mers, x, alpha)));
        entries.add(new PieEntry(30f, "Running", createRescaledDrawable(R.raw.pe_scaun, x, alpha)));
        entries.add(new PieEntry(20f, "Lying Down", createRescaledDrawable(R.raw.cadere, x, alpha)));
        entries.add(new PieEntry(15f, "neutilizat", createRescaledDrawable(R.raw.neutilizat, x, alpha)));

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
        dataSet.setValueTextSize(20f);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueFormatter(new PercentFormatter());

        // Disable labels on the chart slices (labels won't appear on the slices)
        pieChart.setDrawSliceText(false);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);

        ViewPortHandler handler = pieChart.getViewPortHandler();


        // Styling
        pieChart.getDescription().setEnabled(false); // Disable the description text

        float y = 10f;
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(y);

        Legend legend = pieChart.getLegend();

        legend.setEnabled(true);
        legend.setFormSize(10f); // set the size of the legend forms/shapes
        legend.setForm(Legend.LegendForm.CIRCLE); // set what type of form/shape should be used
//        legend.setTypeface(...);
        legend.setTextSize(12f);
        legend.setXEntrySpace(5f); // space between the legend entries on the x-axis
        legend.setYEntrySpace(5f); // space between the legend entries on the y-axis

        pieChart.invalidate(); // Refresh chart

        /////

        // Setting click listener for the date picker button
        binding.dateRangePickerButton.setOnClickListener(view -> DatePickerDialog());

    }

    private void DatePickerDialog() {
        // Creating a MaterialDatePicker builder for selecting a date range
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select a date range");

        // Building the date picker dialog
        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {

            // Retrieving the selected start and end dates
            Long startDate = selection.first;
            Long endDate = selection.second;

            // Formating the selected dates as strings
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            String startDateString = sdf.format(new Date(startDate));
            String endDateString = sdf.format(new Date(endDate));

            // Creating the date range string
            String selectedDateRange = startDateString + " - " + endDateString;

            // Displaying the selected date range in the TextView
            binding.selectedDateRange.setText(selectedDateRange);
        });

        // Showing the date picker dialog
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}