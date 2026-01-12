package com.melisa.innovamotionapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.types.UnknownPosture;
import com.melisa.innovamotionapp.data.posture.types.UnusedFootwearPosture;
import com.melisa.innovamotionapp.databinding.FragmentLivePostureBinding;
import com.melisa.innovamotionapp.ui.viewmodels.LivePostureViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fragment for viewing live posture of a selected monitored person.
 * 
 * Displays:
 * - Person selector dropdown
 * - Current posture video/animation
 * - Posture description
 * - Risk level
 * - Last update timestamp
 * 
 * Reuses posture display logic from BtConnectedActivity.
 */
public class LivePostureFragment extends Fragment {

    private FragmentLivePostureBinding binding;
    private LivePostureViewModel viewModel;
    
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault());

    // Track if this is the first posture shown (for initial state handling)
    private boolean isFirstPosture = true;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLivePostureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(LivePostureViewModel.class);

        setupPersonSelector();
        observePosture();
        
        // Show initial state
        showNoDataState();
    }

    private void setupPersonSelector() {
        // Observe available persons and populate spinner
        viewModel.getAvailablePersons().observe(getViewLifecycleOwner(), this::updatePersonSpinner);

        // Handle selection changes
        binding.personSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object item = parent.getItemAtPosition(position);
                if (item instanceof MonitoredPerson) {
                    MonitoredPerson selected = (MonitoredPerson) item;
                    viewModel.selectPerson(selected.getSensorId());
                    isFirstPosture = true; // Reset for new person
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.selectPerson(null);
                showNoDataState();
            }
        });
    }

    private void updatePersonSpinner(List<MonitoredPerson> persons) {
        if (persons == null || persons.isEmpty()) {
            binding.personSpinner.setAdapter(null);
            showNoDataState();
            return;
        }

        ArrayAdapter<MonitoredPerson> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                persons
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.personSpinner.setAdapter(adapter);

        // If we have a current selection, try to maintain it
        String currentSensorId = viewModel.getSelectedSensorId();
        if (currentSensorId != null) {
            for (int i = 0; i < persons.size(); i++) {
                if (persons.get(i).getSensorId().equals(currentSensorId)) {
                    binding.personSpinner.setSelection(i);
                    break;
                }
            }
        }
    }

    private void observePosture() {
        // Observe current posture
        viewModel.getCurrentPosture().observe(getViewLifecycleOwner(), posture -> {
            if (posture != null) {
                displayPosture(posture);
            } else {
                showNoDataState();
            }
        });

        // Observe last update timestamp
        viewModel.getLastUpdateTime().observe(getViewLifecycleOwner(), timestamp -> {
            if (timestamp != null) {
                binding.lastUpdateText.setText(getString(R.string.last_update, formatTime(timestamp)));
                binding.lastUpdateText.setVisibility(View.VISIBLE);
            } else {
                binding.lastUpdateText.setVisibility(View.GONE);
            }
        });

        // Observe selected person name
        viewModel.getSelectedPersonName().observe(getViewLifecycleOwner(), name -> {
            if (name != null && !name.isEmpty()) {
                binding.personNameText.setText(name);
                binding.personNameText.setVisibility(View.VISIBLE);
            } else {
                binding.personNameText.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Display posture using existing logic from BtConnectedActivity.
     * Handles video playback and text display.
     */
    private void displayPosture(Posture posture) {
        if (posture == null) {
            showNoDataState();
            return;
        }

        // Handle initial unknown posture (show unused footwear instead)
        Posture postureToDisplay = posture;
        if (isFirstPosture && posture instanceof UnknownPosture) {
            postureToDisplay = new UnusedFootwearPosture();
        } else if (!(posture instanceof UnknownPosture)) {
            isFirstPosture = false;
        }

        // Hide no-data overlay
        binding.noDataOverlay.setVisibility(View.GONE);

        // Show posture description with person name
        String personName = viewModel.getSelectedPersonName().getValue();
        if (personName == null || personName.isEmpty()) {
            personName = getString(R.string.unknown_person);
        }
        String description = getString(postureToDisplay.getTextCode(), personName);
        binding.descriptionText.setText(description);

        // Show risk level
        binding.riskText.setText(getString(postureToDisplay.getRisc()));

        // Play posture video/animation
        if (postureToDisplay instanceof UnknownPosture) {
            binding.videoView.stopPlayback();
            binding.videoView.setVideoURI(null);
        } else {
            String videoPath = "android.resource://" +
                    requireContext().getPackageName() + "/" + postureToDisplay.getVideoCode();
            binding.videoView.setVideoPath(videoPath);
            binding.videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                mp.start();
            });
            binding.videoView.start();
        }
    }

    /**
     * Show the no-data state when no person is selected or no data available.
     */
    private void showNoDataState() {
        binding.noDataOverlay.setVisibility(View.VISIBLE);
        binding.descriptionText.setText(R.string.no_data_available);
        binding.riskText.setText("-");
        binding.lastUpdateText.setVisibility(View.GONE);
        binding.videoView.stopPlayback();
    }

    /**
     * Format timestamp for display.
     */
    private String formatTime(long timestamp) {
        Date date = new Date(timestamp);
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;
        
        // If within last 24 hours, show only time
        if (now - timestamp < dayInMillis) {
            return TIME_FORMAT.format(date);
        } else {
            return DATE_TIME_FORMAT.format(date);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume video playback if we have a posture
        Posture currentPosture = viewModel.getCurrentPosture().getValue();
        if (currentPosture != null && !(currentPosture instanceof UnknownPosture)) {
            binding.videoView.start();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Pause video playback
        if (binding.videoView.isPlaying()) {
            binding.videoView.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.videoView.stopPlayback();
        binding = null;
    }
}
