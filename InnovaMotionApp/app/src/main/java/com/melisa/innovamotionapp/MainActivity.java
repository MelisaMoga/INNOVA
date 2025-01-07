package com.melisa.innovamotionapp;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.databinding.MainActivityBinding;


public class MainActivity extends AppCompatActivity {

    private MainActivityBinding binding;

    private final GlobalData globalData = GlobalData.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "Sunt in Resume in main");
        Log.d(TAG, Integer.toString(globalData.nearbyBtDevices.size()));
        for (BluetoothDevice nearbyDevice:globalData.nearbyBtDevices) {
            Log.d(TAG, nearbyDevice.getName());
        }
        Log.d(TAG, "STOP in Resume in main");
    }

    public void LunchMoniotring(View view) {
        Intent i = new Intent(this, BtSettingsActivity.class);
        startActivity(i);
    }
}