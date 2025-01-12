package com.melisa.innovamotionapp.activities;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.melisa.innovamotionapp.databinding.TimelapsActivityBinding;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.util.ArrayList;
import java.util.List;

public class TimeLapseActivity extends AppCompatActivity {
    private TimelapsActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();


    private List<String> imagePaths; // List of image file paths or resource ids
    private List<String> imageTimestamps; // List of timestamps for the images
    private int currentIndex = 0; // Track the current image
    private Handler handler = new Handler(); // To post delayed tasks
    private Runnable showImageRunnable;
    private boolean isRunning = false; // Control the playback state

    private int interval = 1000; // Default interval (milliseconds) for showing each image
    private int speed = 5; // Default speed (1 to 10)


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TimelapsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init database with current context
//        database = InnovaDatabase.getInstance(this);


        // Initialize image paths and timestamps (example)
        imagePaths = new ArrayList<>();
        imageTimestamps = new ArrayList<>();

        // Add images and corresponding timestamps (this should come from your data)
        imagePaths.add("path_to_image1.jpg");
        imagePaths.add("path_to_image2.jpg");
        imageTimestamps.add("12:00:01");
        imageTimestamps.add("12:00:02");

        // Speed seek bar listener
        binding.speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                speed = progress;
                binding.speedLabel.setText("Speed: " + speed);
                interval = 1000 / speed; // Adjust interval based on speed
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Start button click listener
        binding.startButton.setOnClickListener(v -> startTimeLapse());

        // Stop button click listener
        binding.stopButton.setOnClickListener(v -> stopTimeLapse());

        // Runnable for showing images at intervals
        showImageRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentIndex < imagePaths.size()) {
                    String imagePath = imagePaths.get(currentIndex);
                    String timestamp = imageTimestamps.get(currentIndex);

                    // Set the image and timestamp
                    binding.imageView.setImageBitmap(BitmapFactory.decodeFile(imagePath));
                    binding.photoTime.setText("Time: " + timestamp);

                    // Move to the next image
                    currentIndex++;

                    // Post the next task with the adjusted interval
                    handler.postDelayed(this, interval);
                } else {
                    // If we reach the end of the list, reset and stop
                    currentIndex = 0;
                    stopTimeLapse();
                }
            }
        };
    }

    private void startTimeLapse() {
        if (isRunning) {
            Toast.makeText(this, "Time-lapse already running", Toast.LENGTH_SHORT).show();
            return;
        }

        isRunning = true;
        binding.startButton.setEnabled(false); // Disable start button when running
        binding.stopButton.setEnabled(true);  // Enable stop button

        // Start the time-lapse
        handler.post(showImageRunnable);
    }

    private void stopTimeLapse() {
        isRunning = false;
        binding.startButton.setEnabled(true);
        binding.stopButton.setEnabled(false);

        // Stop the time-lapse
        handler.removeCallbacks(showImageRunnable);
        currentIndex = 0; // Reset to the first image
    }

}
