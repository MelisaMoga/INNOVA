package com.melisa.pedonovation.AppActivities;

import  android.os.Bundle;

import android.view.View;
import android.widget.Button;
import android.content.Intent;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.melisa.pedonovation.R;
import com.melisa.pedonovation.databinding.ActivityMainBinding;
import com.melisa.pedonovation.databinding.ActivityStartBinding;

public class StartActivity extends AppCompatActivity {

    public ActivityStartBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Inflate the layout using view binding
        binding = ActivityStartBinding.inflate(getLayoutInflater());
        // Set the content view to the root of the binding
        setContentView(binding.getRoot());
        EdgeToEdge.enable(this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    // Launches the BtSettingsActivity when called
    public void LaunchLogin(View view) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }

    public void mainActivity(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }
}