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
import com.melisa.innovamotionapp.utils.GlobalData;


public class BtConnectedActivity extends AppCompatActivity {
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BtConnectedActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = BtConnectedActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.descriptionTextView.setText(globalData.userName);

        // Observe LiveData for device data
        globalData.getReceivedPosture().observe(this, this::displayPostureData);

        GlobalData.getInstance().getIsConnectedDevice().observe(this, isConnected -> {
            if (!isConnected) {
                // Exit this activity because the device connection is terminated
                finish();
            }
        });

    }

    public void displayPostureData(Posture livePosture) {
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