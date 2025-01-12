package com.melisa.innovamotionapp.activities;

import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.databinding.TimelapsActivityBinding;
import com.melisa.innovamotionapp.ui.viewmodels.TimeLapseViewModel;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeLapseActivity extends AppCompatActivity {
    private TimelapsActivityBinding binding;
    private final GlobalData globalData = GlobalData.getInstance();


    private List<Integer> imageIds; // List of image file paths or resource ids
    private List<String> imageTimestamps; // List of timestamps for the images
    private int currentIndex = 0; // Track the current image
    private Handler handler = new Handler(); // To post delayed tasks
    private Runnable showImageRunnable;
    private boolean isRunning = false; // Control the playback state

    private int interval = 1000; // Default interval (milliseconds) for showing each image
    private int speed = 5; // Default speed (1 to 10)
    private InnovaDatabase database;
    private TimeLapseViewModel viewModel;

    private boolean displayedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = TimelapsActivityBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Init database with current context
        database = InnovaDatabase.getInstance(this);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(TimeLapseViewModel.class);


        // Initialize image paths and timestamps (example)
        imageIds = new ArrayList<>();
        imageTimestamps = new ArrayList<>();

        // Boolean init to display data only once
        displayedOnce = false;

        // Observe latestDevice's saved postures
        viewModel.getDataForLatestDevice().observe(this, receivedBtDataEntities -> {
            if (displayedOnce) {
                return;
            }
            if (receivedBtDataEntities != null && !receivedBtDataEntities.isEmpty()) {
                // Boolean set to display data only once
                displayedOnce = true;

                // Add images and corresponding timestamps (this should come from your data)
                for (ReceivedBtDataEntity receivedBtDataEntity : receivedBtDataEntities) {
                    // Add posture's picture
                    Posture posture = PostureFactory.createPosture(receivedBtDataEntity.getReceivedMsg());

                    // Save posture's picture
                    imageIds.add(posture.getPictureCode());

                    // Format posture's timestamp
                    // Format both date and time (dd/MM/yyyy HH:mm:ss)
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
                    String postureFormatedDate = sdf.format(new Date(receivedBtDataEntity.getTimestamp()));

                    // Add posture's formated timestamp
                    imageTimestamps.add(postureFormatedDate);
                }
                setupUi();

                // Initialize with the first image
                updateImageDisplay();
            }
        });

        // Initialize with the first image
        updateImageDisplay();
    }

    private void setupUi() {
        // Set max value of SeekBar to the number of images
        binding.seekBar.setMax(imageIds.size() - 1);

        // Listener for SeekBar drag to navigate images
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    currentIndex = progress;
                    updateImageDisplay(); // Update the image display when dragged
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        // Speed SeekBar listener
        binding.speedSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                }
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
                if (currentIndex < imageIds.size()) {
                    updateImageDisplay(); // Display the current image
                    currentIndex++;
                    binding.seekBar.setProgress(currentIndex); // Update SeekBar
                    handler.postDelayed(this, interval); // Post next task
                } else {
                    stopTimeLapse(); // Stop at the end of the list
                }
            }
        };
    }

    private void updateImageDisplay() {
        if (currentIndex >= 0 && currentIndex < imageIds.size()) {
            int imageId = imageIds.get(currentIndex);
            String timestamp = imageTimestamps.get(currentIndex);

            // Set the image and timestamp
            binding.imageView.setImageBitmap(BitmapFactory.decodeResource(getResources(), imageId));
            binding.photoTime.setText("Time: " + timestamp);
        }
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
