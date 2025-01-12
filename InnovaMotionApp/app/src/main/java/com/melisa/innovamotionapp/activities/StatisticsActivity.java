package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Pair;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointF;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.databinding.StatisticsActivityBinding;
import com.melisa.innovamotionapp.ui.viewmodels.StatisticsViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class StatisticsActivity extends AppCompatActivity {
    private StatisticsActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();
    private long startDate;
    private long endDate;
    private InnovaDatabase database;
    private StatisticsViewModel viewModel;
    private boolean showDefaultData = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = StatisticsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init database with current context
        database = InnovaDatabase.getInstance(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        // Setting click listener for the date picker button
        binding.dateRangePickerButton.setOnClickListener(view -> datePickerDialog());

        // DEFAULT BEHAVIOUR
        // Fetch all data from the database
        viewModel.getDataForDevice().observe(this, receivedBtDataEntities -> {
            if (showDefaultData) {
                // If data is available, find the start and end timestamps
                startDate = receivedBtDataEntities.get(0).getTimestamp(); // Set startDate as the timestamp of the first entry
                endDate = receivedBtDataEntities.get(receivedBtDataEntities.size() - 1).getTimestamp(); // Set endDate as the timestamp of the last entry

                // Update UI with the selected date range (formatted)
                updateWithDateRange(startDate, endDate);
                this.onSavedDataChange(receivedBtDataEntities);
            }
        });

    }

    // Method to load, rescale and apply transparency
    public Drawable createRescaledDrawable(int resourceId, int newSizeX, int newSizeY) {
        // Load the image from resources as a Bitmap
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resourceId);

        // Rescale the Bitmap based on the rescale factor
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newSizeX, newSizeY, false);

        // Release memory from the original bitmap as it is no longer needed
        bitmap.recycle();

        // Convert the rescaled Bitmap to Drawable
        BitmapDrawable drawable = new BitmapDrawable(getResources(), scaledBitmap);

        return drawable;
    }

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

    private void createPieChart(List<Posture> postureListInDateRange) {

        PieChart pieChart = binding.pieChart;

        Legend legend = pieChart.getLegend();
        legend.setEnabled(true);
        legend.setWordWrapEnabled(true);
        legend.setFormSize(10f); // set the size of the legend forms/shapes
        legend.setForm(Legend.LegendForm.CIRCLE); // set what type of form/shape should be used
        legend.setTextSize(12f);
        legend.setXEntrySpace(5f); // space between the legend entries on the x-axis
        legend.setYEntrySpace(5f); // space between the legend entries on the y-axis

        ArrayList<PieEntry> entries = addDataToPieChart(postureListInDateRange);
        if (!entries.isEmpty()) {
            PieDataSet dataSet = new PieDataSet(entries, "");
            dataSet.setColors(ColorTemplate.VORDIPLOM_COLORS);
            dataSet.setValueTextSize(20f);

            dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
            dataSet.setValueFormatter(new PercentFormatter());
//            dataSet.setSliceSpace(5f);
            PieData data = new PieData(dataSet);
            dataSet.setSelectionShift(10);
            dataSet.setIconsOffset(new MPPointF(0, 20));
            pieChart.setData(data);

            switch (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) {

                case Configuration.UI_MODE_NIGHT_YES:
                    dataSet.setValueTextColor(Color.WHITE);
                    legend.setTextColor(Color.WHITE);
                    break;
                case Configuration.UI_MODE_NIGHT_NO:
                    break;
            }
        } else {
            // Remove previous dataSet if postureListInDateRange is empty
            pieChart.clear();
        }

        // Disable labels on the chart slices (labels won't appear on the slices)
        pieChart.setDrawSliceText(false);
        ViewPortHandler handler = pieChart.getViewPortHandler();

        // Styling
        pieChart.getDescription().setEnabled(false); // Disable the description text

        float y = 10f;
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(y);

        pieChart.invalidate(); // Refresh chart
    }

    /**
     * @noinspection DataFlowIssue
     */
    private ArrayList<PieEntry> addDataToPieChart(List<Posture> postureListInDateRange) {
        // Count occurrences of each posture type
        Map<String, Integer> postureCountMap = new HashMap<>();
        Map<String, Integer> postureResourceMap = new HashMap<>(); // To store resource codes for each posture type
        for (Posture posture : postureListInDateRange) {
            String postureType = posture.getClass().getSimpleName(); // Assuming each posture has a distinct class
            postureCountMap.compute(postureType, (key, currentValue) -> (currentValue == null ? 0 : currentValue) + 1);
            postureResourceMap.put(postureType, posture.getPictureCode()); // Save resource code
        }

        int totalPostures = postureListInDateRange.size();
        ArrayList<PieEntry> pieEntries = new ArrayList<>();
        float sizeMultiplier = 2;
        int newSizeX = (int) (75 * sizeMultiplier);
        int newSizeY = (int) (94 * sizeMultiplier);

        for (Map.Entry<String, Integer> entry : postureCountMap.entrySet()) {
            String postureType = entry.getKey();
            int count = entry.getValue();
            // Calculate percentages
            float percentage = ((float) count / totalPostures) * 100;
            // Fetch the resource code for this posture type
            int resourceCode = postureResourceMap.get(postureType);
            // Add a PieEntry with the percentage and corresponding label
            pieEntries.add(new PieEntry(percentage, postureType, createRescaledDrawable(resourceCode, newSizeX, newSizeY)));
        }

        return pieEntries;
    }

    private void datePickerDialog() {

        // Creating a MaterialDatePicker builder for selecting a date range
        MaterialDatePicker.Builder<Pair<Long, Long>> builder = MaterialDatePicker.Builder.dateRangePicker();
        builder.setTitleText("Select a date range");

        // Building the date picker dialog
        MaterialDatePicker<Pair<Long, Long>> datePicker = builder.build();
        datePicker.addOnPositiveButtonClickListener(selection -> {
            // Adjust startDate to the first time (00:00:00) of the selected date
            long startDateProcessed  = adjustToStartOfDay(selection.first);
            // Adjust endDate to the latest time (23:59:59) of the selected date
            long endDateProcessed  = adjustToEndOfDay(selection.second);

            updateWithDateRange(startDateProcessed, endDateProcessed);
            // Disable default behaviour
            showDefaultData = false;

            // Remove any existing observer before adding a new one
            viewModel.getDataForDeviceInRange(startDate, endDate).removeObservers(this);
            // Custom BEHAVIOUR
            // Observe the data from the ViewModel
            viewModel.getDataForDeviceInRange(startDate, endDate).observe(this, receivedBtDataEntities -> {
                if (!showDefaultData) {
                    this.onSavedDataChange(receivedBtDataEntities);
                }
            });

        });

        // Showing the date picker dialog
        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    private long adjustToStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0); // Optional for precision
        return calendar.getTimeInMillis();
    }

    private long adjustToEndOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999); // Optional for precision
        return calendar.getTimeInMillis();
    }

    private void updateWithDateRange(long selectedStartDate, long selectedEndDate) {
        // Retrieving the selected start and end dates
        startDate = selectedStartDate;
        endDate = selectedEndDate;


        // Format both date and time (dd/MM/yyyy HH:mm:ss)
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String startDateString = sdf.format(new Date(startDate));
        String endDateString = sdf.format(new Date(endDate));

        // Creating the date range string
        String selectedDateRange = startDateString + " - " + endDateString;

        // Displaying the selected date range in the TextView
        binding.selectedDateRange.setText(selectedDateRange);
        createPieChart(new ArrayList<>());
    }

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void onSavedDataChange(List<ReceivedBtDataEntity> receivedBtDataEntities) {
        // Convert entities to postures
        List<Posture> postureListInDateRange = new ArrayList<>();
        for (ReceivedBtDataEntity entity : receivedBtDataEntities) {
            postureListInDateRange.add(PostureFactory.createPosture(entity.getReceivedMsg()));
        }
        // Update the pie chart
        createPieChart(postureListInDateRange);
    }
}