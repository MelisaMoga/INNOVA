package com.melisa.innovamotionapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.FragmentMessageLogBinding;
import com.melisa.innovamotionapp.ui.adapters.MessageLogAdapter;
import com.melisa.innovamotionapp.ui.models.MessageLogItem;
import com.melisa.innovamotionapp.ui.viewmodels.MessageLogViewModel;
import com.melisa.innovamotionapp.utils.PersonNameManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fragment displaying real-time message log from all sensors.
 * Shows incoming Bluetooth messages with person names, timestamps, and posture icons.
 * Falls are highlighted in red.
 */
public class MessageLogFragment extends Fragment {

    private FragmentMessageLogBinding binding;
    private MessageLogViewModel viewModel;
    private MessageLogAdapter adapter;
    private PersonNameManager personNameManager;
    
    // Filter spinner items
    private final List<String> sensorFilterList = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMessageLogBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        personNameManager = PersonNameManager.getInstance(requireContext());
        viewModel = new ViewModelProvider(this).get(MessageLogViewModel.class);
        
        setupRecyclerView();
        setupFilters();
        observeData();
    }

    private void setupRecyclerView() {
        adapter = new MessageLogAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        
        // Auto-scroll to bottom when new messages arrive
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (binding.autoScrollSwitch.isChecked() && adapter.getItemCount() > 0) {
                    // Scroll to top since messages are ordered DESC (newest first)
                    binding.recyclerView.smoothScrollToPosition(0);
                }
            }
        });
    }

    private void setupFilters() {
        // Initialize spinner adapter with "All" option
        sensorFilterList.add(getString(R.string.filter_all_sensors));
        spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                sensorFilterList
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.filterSpinner.setAdapter(spinnerAdapter);
        
        // Handle filter selection
        binding.filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    // "All" selected
                    viewModel.setFilterSensor(null);
                } else {
                    String selectedSensor = sensorFilterList.get(position);
                    // Extract sensor ID from display name if needed
                    viewModel.setFilterSensor(extractSensorId(selectedSensor));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                viewModel.setFilterSensor(null);
            }
        });
    }

    /**
     * Extract sensor ID from the display string (which may include display name).
     */
    private String extractSensorId(String displayString) {
        // If the string contains " - ", extract the sensor ID part
        // Format: "Display Name (sensorId)" or just "sensorId"
        if (displayString.contains(" (") && displayString.endsWith(")")) {
            int start = displayString.lastIndexOf(" (") + 2;
            int end = displayString.length() - 1;
            return displayString.substring(start, end);
        }
        return displayString;
    }

    private void observeData() {
        // Observe messages
        viewModel.getMessages().observe(getViewLifecycleOwner(), messages -> {
            adapter.submitList(messages);
            
            // Show/hide empty state
            boolean isEmpty = messages == null || messages.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        });
        
        // Observe available sensors for filter dropdown
        viewModel.getAvailableSensors().observe(getViewLifecycleOwner(), sensors -> {
            updateFilterSpinner(sensors);
        });
        
        // Observe message counts for summary header
        viewModel.getMessageCountsPerSensor().observe(getViewLifecycleOwner(), this::updateSummaryHeader);
        
        // Observe person name changes to update spinner labels
        personNameManager.getAllPersonsLive().observe(getViewLifecycleOwner(), persons -> {
            // Re-trigger spinner update with current sensors
            List<String> currentSensors = viewModel.getAvailableSensors().getValue();
            if (currentSensors != null) {
                updateFilterSpinner(currentSensors);
            }
        });
    }

    private void updateFilterSpinner(List<String> sensors) {
        sensorFilterList.clear();
        sensorFilterList.add(getString(R.string.filter_all_sensors));
        
        if (sensors != null) {
            for (String sensorId : sensors) {
                // Get display name for sensor
                String displayName = viewModel.getDisplayName(sensorId);
                if (displayName.equals(sensorId)) {
                    sensorFilterList.add(sensorId);
                } else {
                    sensorFilterList.add(displayName + " (" + sensorId + ")");
                }
            }
        }
        
        spinnerAdapter.notifyDataSetChanged();
    }

    private void updateSummaryHeader(Map<String, Integer> counts) {
        binding.summaryHeader.removeAllViews();
        
        if (counts == null || counts.isEmpty()) {
            binding.summaryScrollView.setVisibility(View.GONE);
            return;
        }
        
        binding.summaryScrollView.setVisibility(View.VISIBLE);
        
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String sensorId = entry.getKey();
            int count = entry.getValue();
            
            // Get display name
            String displayName = viewModel.getDisplayName(sensorId);
            
            // Create chip-like view for each sensor
            TextView chip = new TextView(requireContext());
            chip.setText(displayName + ": " + count);
            chip.setBackgroundResource(R.drawable.chip_background);
            chip.setPadding(16, 8, 16, 8);
            chip.setTextSize(12);
            
            ViewGroup.MarginLayoutParams params = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(4, 4, 4, 4);
            chip.setLayoutParams(params);
            
            binding.summaryHeader.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
