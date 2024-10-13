package com.melisa.pedonovation.AppActivities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.melisa.pedonovation.AppActivities.Managers.MainManager;
import com.melisa.pedonovation.DEBUG_CLASS;
import com.melisa.pedonovation.GlobalData;
import com.melisa.pedonovation.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    // Used to load the 'app' library on application startup.
    static {
        System.loadLibrary("app");
    }


    // Binding object for accessing views in activity_main.xml
    public ActivityMainBinding binding;
    private MainManager mainManager;
    private GlobalData globalData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate the layout using view binding
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        // Set the content view to the root of the binding
        setContentView(binding.getRoot());

        globalData = ((GlobalData) this.getApplicationContext());

        mainManager = new MainManager(this, globalData);

        // DEBUG ONLY:
        binding.editTextTextMultiLine.setOnClickListener(v -> new DEBUG_CLASS(this, globalData));

    }


    // Launches the BtSettingsActivity when called
    public void LaunchSettings(View view) {
        Intent i = new Intent(this, BtSettingsActivity.class);
        startActivity(i);
    }


    // Launches the GraphActivity when called
    public void LaunchGraphView(View view) {
        Intent i = new Intent(this, GraphActivity.class);
        startActivity(i);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Change the manager to mainManager that handles the Thread messages
        globalData.setActivityManagerForHandler(mainManager);

        // Update Main UI elements
        mainManager.updateUI();
    }

    public void startCommand(View view) {
        String startMsg = "s";
        globalData.connectionData1.connectionThread.write(startMsg.getBytes());
        globalData.connectionData2.connectionThread.write(startMsg.getBytes());
    }

    public void startWrittingData(View view) {
        // BUTTON IS ON
        if (binding.toggleWriteData.isChecked()){
            // Delete data files
            mainManager.deleteDataFiles();
            // set boolean 'shouldWriteToFile' to true in order to activate writing to file
            mainManager.shouldWriteToFile = true;

        } else {
            // set boolean 'shouldWriteToFile' to false in order to stop writing to file
            mainManager.shouldWriteToFile = false;
        }
    }
}

