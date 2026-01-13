package com.melisa.innovamotionapp.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.data.PieEntry;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.databinding.EnergyConsumptionActivityBinding;
import com.melisa.innovamotionapp.ui.viewmodels.EnergyConsumptionViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.Logger;
import com.melisa.innovamotionapp.utils.TargetUserResolver;
import com.melisa.innovamotionapp.sync.UserSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Activity for displaying energy consumption breakdown.
 * 
 * Supports two modes:
 * 1. User-based filtering: Shows data for the current user
 * 2. Sensor-based filtering: Shows data for a specific sensor (via intent extras)
 */
public class EnergyConsumptionActivity extends AppCompatActivity {
    
    private static final String TAG = "EnergyConsumptionActivity";
    
    // Intent extras for sensor-specific viewing
    public static final String EXTRA_SENSOR_ID = "extra_sensor_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";
    
    private EnergyConsumptionActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();
    private InnovaDatabase database;
    private EnergyConsumptionViewModel viewModel;
    private boolean displayedOnce;
    
    // Sensor-specific fields
    private String sensorId;
    private String personName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = EnergyConsumptionActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init database with current context
        database = InnovaDatabase.getInstance(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(EnergyConsumptionViewModel.class);

        // Boolean init to display data only once
        displayedOnce = false;

        // Extract intent extras
        sensorId = getIntent().getStringExtra(EXTRA_SENSOR_ID);
        personName = getIntent().getStringExtra(EXTRA_PERSON_NAME);

        // Update title to show person name if available
        updateTitle();

        // Choose filtering mode based on whether sensorId is provided
        if (sensorId != null && !sensorId.isEmpty()) {
            // Sensor-specific mode: Show data for this sensor only
            Logger.i(TAG, "Sensor-specific mode: sensorId=" + sensorId + ", name=" + personName);
            viewModel.setSensorId(sensorId);
        } else {
            // User-based mode: Resolve and set target user once session is loaded
            Logger.d(TAG, "User-based mode: resolving target user");
            if (UserSession.getInstance(getApplicationContext()).isLoaded()) {
                String target = TargetUserResolver.resolveTargetUserId(getApplicationContext());
                Logger.i(TAG, "Resolved targetUserId=" + target);
                viewModel.setTargetUserId(target);
            } else {
                UserSession.getInstance(getApplicationContext()).loadUserSession(new UserSession.SessionLoadCallback() {
                    @Override
                    public void onSessionLoaded(String uid, String role, java.util.List<String> kids) {
                        String target = TargetUserResolver.resolveTargetUserId(getApplicationContext());
                        Logger.i(TAG, "Resolved targetUserId=" + target);
                        viewModel.setTargetUserId(target);
                    }

                    @Override
                    public void onSessionLoadError(String error) {
                        Logger.w(TAG, "Session load error: " + error);
                    }
                });
            }
        }

        // Fetch all data for target user or sensor
        viewModel.getAllForUser().observe(this, list -> {
//            if (displayedOnce) {
//                return;
//            }
            int size = (list != null ? list.size() : 0);
            Logger.i(TAG, "Received data: listSize=" + size);
            if (list != null && !list.isEmpty()) {

                binding.parentLayout.removeAllViews();
                // Boolean set to display data only once
                displayedOnce = true;

                // Display the savedData's dates interval
                long startDate = list.get(0).getTimestamp(); // Set startDate as the timestamp of the first entry
                long endDate = list.get(list.size() - 1).getTimestamp(); // Set endDate as the timestamp of the last entry
                // Format both date and time (dd/MM/yyyy HH:mm:ss)
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                String startDateString = sdf.format(new Date(startDate));
                String endDateString = sdf.format(new Date(endDate));
                // Creating the date range string
                String selectedDateRange = startDateString + " - " + endDateString;
                binding.selectedDateRange.setText(selectedDateRange);


                // Convert entities to postures
                List<Posture> postureListInDateRange = new ArrayList<>();
                for (ReceivedBtDataEntity entity : list) {
                    postureListInDateRange.add(PostureFactory.createPosture(entity.getReceivedMsg()));
                }
                // TODO: change here
                // Calculate
                func(postureListInDateRange);
            }
        });
    }

    private void func(List<Posture> postureListInDateRange) {
        int sum = 0;
        // Count occurrences of each posture type
        Map<String, Integer> postureResourceMap = new HashMap<>(); // To store resource codes for each posture type
        Map<String, Integer> postureCaloriesMap = new HashMap<>(); // To store sum calories for each posture type
        for (Posture posture : postureListInDateRange) {
            sum += posture.getCalories();
            String postureType = posture.getClass().getSimpleName(); // Assuming each posture has a distinct class
            postureCaloriesMap.compute(postureType, (key, currentValue) -> (currentValue == null ? 0 : currentValue) + posture.getCalories());
            postureResourceMap.put(postureType, posture.getPictureCode()); // Save resource code
        }


        // Add rows dynamically
        for (Map.Entry<String, Integer> entry : postureResourceMap.entrySet()) {
            String postureName = entry.getKey();
            int resourceId = entry.getValue();

            // Create a horizontal LinearLayout for each row
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(8, 8, 8, 8);

            // Set background color for alternating rows (optional)
            int backgroundColor = postureResourceMap.size() % 2 == 0 ? 0xFFEEEEEE : 0xFFFFFFFF;
            rowLayout.setBackgroundColor(backgroundColor);

            // Add ImageView for the posture image
            ImageView imageView = new ImageView(this);
            imageView.setLayoutParams(new LinearLayout.LayoutParams(100, 100));
            imageView.setImageResource(resourceId);
            rowLayout.addView(imageView);

            // Add TextView for the posture name or calories
            TextView textView = new TextView(this);
            textView.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            textView.setText(postureName + " - " + postureCaloriesMap.get(postureName) + " cal");
            textView.setPadding(16, 0, 0, 0);
            rowLayout.addView(textView);

            // Add the row to the parent layout
            binding.parentLayout.addView(rowLayout);
        }

        // Add a total consumption TextView
        TextView totalTextView = new TextView(this);
        totalTextView.setText("Total consum: " + sum + " calorii");
        totalTextView.setTextSize(18);
        totalTextView.setPadding(8, 16, 0, 0);
        binding.parentLayout.addView(totalTextView);

    }

    /**
     * Updates the title to show the person's name when viewing sensor-specific data.
     */
    private void updateTitle() {
        if (personName != null && !personName.isEmpty()) {
            binding.textView3.setText(getString(R.string.energy_title_for_person, personName));
            Logger.d(TAG, "Title updated for person: " + personName);
        } else {
            binding.textView3.setText(R.string.energy_title);
            Logger.d(TAG, "Using default title");
        }
    }

}
