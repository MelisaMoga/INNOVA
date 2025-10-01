package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.databinding.BtConnectedActivityBinding;
import com.melisa.innovamotionapp.ui.viewmodels.SupervisorFeedViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;
import com.melisa.innovamotionapp.utils.RoleProvider;


public class BtConnectedActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BtConnectedActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();
    private SupervisorFeedViewModel supervisorFeedViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BtConnectedActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.descriptionTextView.setText(globalData.userName);

        // Existing observer stays: observe LiveData for device data
        globalData.getReceivedPosture().observe(this, posture -> {
            if (posture == null) return; // wait for real data
            displayPostureData(posture);
        });

        // Modified connection observer: supervisors don't need to exit on disconnect
        GlobalData.getInstance().getIsConnectedDevice().observe(this, isConnected -> {
            if (!isConnected && RoleProvider.getCurrentRole() != RoleProvider.Role.SUPERVISOR) {
                // Exit this activity only for supervised users when device connection is terminated
                // Supervisors stay in the activity to continue monitoring Room data
                finish();
            }
        });

        // New: if supervisor, observe latest message from Room so we always show the freshest posture
        if (RoleProvider.getCurrentRole() == RoleProvider.Role.SUPERVISOR) {
            Log.d(TAG, "Supervisor detected: setting up Room-based posture feed");
            supervisorFeedViewModel = new androidx.lifecycle.ViewModelProvider(this).get(SupervisorFeedViewModel.class);
            supervisorFeedViewModel.getLatestPosture().observe(this, posture -> {
                if (posture != null) {
                    Log.d(TAG, "Supervisor: received latest posture from Room");
                    displayPostureData(posture);
                }
            });
        }

    }

    public void displayPostureData(Posture livePosture) {
        if (livePosture == null) return; // extra guard

        String postureMessageWithName = getString(livePosture.getTextCode(), globalData.userName);
        binding.descriptionTextView.setText(postureMessageWithName);

        binding.riskValueTextView.setText(getString(livePosture.getRisc()));

        if (livePosture instanceof UnknownPosture) {
            // The posture is an instance of UnknownPosture
            binding.videoView.stopPlayback(); // Stop any ongoing playback
            binding.videoView.setVideoURI(null); // Clear the video content
        } else {
            // Handle other cases
            String videoPath = "android.resource://" + getPackageName() + "/" + livePosture.getVideoCode();
            binding.videoView.setVideoPath(videoPath);
            binding.videoView.start(); // Start playing the video
        }
    }

    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}