package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.databinding.StartActivityBinding;


public class StartActivity extends AppCompatActivity {

    // Used to load the 'innovamotionapp' library on application startup.
//    static {
//        System.loadLibrary("innovamotionapp");
//    }

    private StartActivityBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = StartActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
    }

    public void mainActivity(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

}