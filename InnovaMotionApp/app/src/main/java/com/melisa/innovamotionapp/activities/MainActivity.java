package com.melisa.innovamotionapp.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.databinding.MainActivityBinding;
import com.melisa.innovamotionapp.utils.GlobalData;


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
    }

    public void LunchMoniotring(View view) {
        Intent i = new Intent(this, BtSettingsActivity.class);
        startActivity(i);
    }

    public void LaunchStatistics(View view) {
        Intent i = new Intent(this, StatisticsActivity.class);
        startActivity(i);
    }

    public void LaunchTimelaps(View view) {
        Intent i = new Intent(this, TimeLapseActivity.class);
        startActivity(i);
    }
}