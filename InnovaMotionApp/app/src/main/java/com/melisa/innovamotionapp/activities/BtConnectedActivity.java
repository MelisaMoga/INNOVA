package com.melisa.innovamotionapp.activities;

import static android.content.ContentValues.TAG;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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

        binding.ChildName.setText(globalData.childName);

        // Observe LiveData for device data
        globalData.getReceivedData().observe(this, data -> {
            // Update UI elements on the main thread
            binding.editTextTextMultiLine.append(data + '\n');
        });

        GlobalData.getInstance().getIsConnectedDevice().observe(this, isConnected -> {
            if (!isConnected) {
                // Exit this activity because the device connection is terminated
                finish();
            }
        });
    }


    public void log(String msg) {
        Log.d(TAG, msg);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}