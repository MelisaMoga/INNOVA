package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataDao;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.UnusedFootwearPosture;
import com.melisa.innovamotionapp.databinding.BtConnectedActivityBinding;
import com.melisa.innovamotionapp.ui.viewmodels.SupervisorFeedViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.RoleProvider;


/**
 * Activity for displaying posture data for a single person.
 * 
 * Supports two modes:
 * 1. Legacy mode: Shows data from GlobalData (Bluetooth connection)
 * 2. Sensor-specific mode: Shows data for a specific sensorId (Supervisor viewing from dashboard)
 */
public class BtConnectedActivity extends AppCompatActivity {
    
    // Intent extras for sensor-specific viewing
    public static final String EXTRA_SENSOR_ID = "extra_sensor_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";
    
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BtConnectedActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();
    private SupervisorFeedViewModel supervisorFeedViewModel;
    private boolean isFirstPosture = true;
    private Posture currentPosture = null;
    
    // Sensor-specific fields
    private String sensorId;
    private String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BtConnectedActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Extract intent extras
        sensorId = getIntent().getStringExtra(EXTRA_SENSOR_ID);
        personName = getIntent().getStringExtra(EXTRA_PERSON_NAME);
        
        // Set initial title/name
        if (personName != null && !personName.isEmpty()) {
            // Supervisor viewing specific person - show their name
            binding.descriptionTextView.setText(personName);
        } else {
            // Legacy behavior - use global userName
            binding.descriptionTextView.setText(globalData.userName);
        }

        // Choose observation mode based on whether sensorId is provided
        if (sensorId != null && !sensorId.isEmpty() && RoleProvider.isSupervisor()) {
            // Supervisor viewing a specific person from dashboard
            Log.d(TAG, "Supervisor viewing specific person: sensorId=" + sensorId + ", name=" + personName);
            observeSensorSpecificData();
        } else {
            // Legacy behavior: GlobalData observer for Bluetooth connection
            observeGlobalData();
        }

        // Modified connection observer: supervisors don't need to exit on disconnect
        GlobalData.getInstance().getIsConnectedDevice().observe(this, isConnected -> {
            if (!isConnected && RoleProvider.getCurrentRole() != RoleProvider.Role.SUPERVISOR) {
                // Exit this activity only for supervised users when device connection is terminated
                // Supervisors stay in the activity to continue monitoring Room data
                finish();
            }
        });
    }

    /**
     * Observe sensor-specific data from Room database.
     * Used when supervisor views a specific person from the dashboard.
     */
    private void observeSensorSpecificData() {
        ReceivedBtDataDao dao = InnovaDatabase.getInstance(this).receivedBtDataDao();
        dao.getLatestForSensor(sensorId).observe(this, entity -> {
            if (entity != null) {
                Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
                Log.d(TAG, "Sensor-specific: received posture for " + sensorId);
                displayPostureData(posture);
            }
        });
    }

    /**
     * Observe global data from GlobalData and SupervisorFeedViewModel.
     * Legacy behavior for Bluetooth connection mode.
     */
    private void observeGlobalData() {
        // Existing observer stays: observe LiveData for device data
        globalData.getReceivedPosture().observe(this, posture -> {
            if (posture == null) return; // wait for real data
            displayPostureData(posture);
        });

        // New: if supervisor, observe latest message from Room so we always show the freshest posture
        if (RoleProvider.getCurrentRole() == RoleProvider.Role.SUPERVISOR) {
            Log.d(TAG, "Supervisor detected: setting up Room-based posture feed");
            supervisorFeedViewModel = new ViewModelProvider(this).get(SupervisorFeedViewModel.class);
            supervisorFeedViewModel.getLatestPosture().observe(this, posture -> {
                if (posture != null) {
                    Log.d(TAG, "Supervisor: received latest posture from Room");
                    displayPostureData(posture);
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentPosture != null) {
            displayPostureData(currentPosture);
        }
    }

    public void displayPostureData(Posture livePosture) {
        if (livePosture == null) return; // extra guard

        Posture postureToDisplay = livePosture;
        if (isFirstPosture && postureToDisplay instanceof UnknownPosture) {
            postureToDisplay = new UnusedFootwearPosture();
        } else if (!(livePosture instanceof UnknownPosture)) {
            isFirstPosture = false;
        }

        currentPosture = postureToDisplay;

        // Use personName if available, otherwise fall back to globalData.userName
        String displayName = (personName != null && !personName.isEmpty()) 
                ? personName 
                : globalData.userName;
        String postureMessageWithName = getString(postureToDisplay.getTextCode(), displayName);
        binding.descriptionTextView.setText(postureMessageWithName);

        binding.riskValueTextView.setText(getString(postureToDisplay.getRisc()));

        if (postureToDisplay instanceof UnknownPosture) {
            // The posture is an instance of UnknownPosture
            binding.videoView.stopPlayback(); // Stop any ongoing playback
            binding.videoView.setVideoURI(null); // Clear the video content
        } else {
            // Handle other cases
            String videoPath = "android.resource://" + getPackageName() + "/" + postureToDisplay.getVideoCode();
            binding.videoView.setVideoPath(videoPath);
            binding.videoView.start(); // Start playing the video
        }
    }

    /**
     * Get the current sensorId (for passing to child activities).
     */
    public String getSensorId() {
        return sensorId;
    }

    /**
     * Get the current personName (for passing to child activities).
     */
    public String getPersonName() {
        return personName;
    }

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
