package com.melisa.innovamotionapp.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.DialogSensorSettingsBinding;
import com.melisa.innovamotionapp.sync.SensorAssignmentService;
import com.melisa.innovamotionapp.ui.helpers.SupervisorEmailAutocomplete;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialog for editing sensor settings including display name and supervisor assignment.
 * 
 * Combines the functionality of:
 * - Editing the display name for a sensor
 * - Assigning/unassigning a supervisor to the sensor
 * 
 * Uses SupervisorEmailAutocomplete for email input with dropdown suggestions.
 */
public class SensorSettingsDialog extends DialogFragment {

    private static final String ARG_SENSOR_ID = "sensor_id";
    private static final String ARG_CURRENT_NAME = "current_name";
    private static final String ARG_CURRENT_SUPERVISORS = "current_supervisors";

    private DialogSensorSettingsBinding binding;
    private SupervisorEmailAutocomplete emailAutocomplete;
    private OnSaveListener saveListener;
    private OnUnassignListener unassignListener;
    
    private String sensorId;
    private List<String> currentSupervisors;

    /**
     * Callback for when the user saves changes.
     */
    public interface OnSaveListener {
        /**
         * Called when user saves the dialog.
         * 
         * @param sensorId The sensor ID
         * @param newName The new display name
         * @param supervisorEmail The new supervisor email to add (may be null if not adding)
         */
        void onSave(String sensorId, String newName, @Nullable String supervisorEmail);
    }

    /**
     * Callback for when the user unassigns a specific supervisor.
     */
    public interface OnUnassignListener {
        /**
         * Called when user removes a supervisor assignment.
         * 
         * @param sensorId The sensor ID
         * @param supervisorEmail The supervisor email being unassigned
         */
        void onUnassign(String sensorId, String supervisorEmail);
    }

    /**
     * Create a new instance of the dialog.
     * 
     * @param sensorId            The sensor ID (displayed as readonly)
     * @param currentName         The current display name (pre-filled in input)
     * @param currentSupervisors  List of currently assigned supervisor emails (or null/empty)
     */
    public static SensorSettingsDialog newInstance(@NonNull String sensorId, 
                                                    @NonNull String currentName,
                                                    @Nullable ArrayList<String> currentSupervisors) {
        SensorSettingsDialog dialog = new SensorSettingsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SENSOR_ID, sensorId);
        args.putString(ARG_CURRENT_NAME, currentName);
        args.putStringArrayList(ARG_CURRENT_SUPERVISORS, currentSupervisors);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Set the listener for save events.
     */
    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    /**
     * Set the listener for unassign events.
     */
    public void setOnUnassignListener(OnUnassignListener listener) {
        this.unassignListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogSensorSettingsBinding.inflate(getLayoutInflater());

        sensorId = requireArguments().getString(ARG_SENSOR_ID, "");
        String currentName = requireArguments().getString(ARG_CURRENT_NAME, "");
        ArrayList<String> supervisorsList = requireArguments().getStringArrayList(ARG_CURRENT_SUPERVISORS);
        currentSupervisors = supervisorsList != null ? new ArrayList<>(supervisorsList) : new ArrayList<>();

        setupUI(currentName);
        setupAutocomplete();

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sensor_settings_title)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.save, (dialog, which) -> onSave())
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    private void setupUI(String currentName) {
        // Display sensor ID (readonly)
        binding.sensorIdText.setText(sensorId);

        // Pre-fill current name and position cursor at end
        binding.nameInput.setText(currentName);
        binding.nameInput.setSelection(currentName.length());

        // Populate supervisor chips
        populateSupervisorChips();
    }

    /**
     * Populate the ChipGroup with chips for each assigned supervisor.
     * Each chip has a close icon that triggers unassignment.
     */
    private void populateSupervisorChips() {
        binding.supervisorChipGroup.removeAllViews();
        
        if (currentSupervisors == null || currentSupervisors.isEmpty()) {
            // No supervisors - show status text
            binding.supervisorChipGroup.setVisibility(View.GONE);
            binding.supervisorStatusText.setVisibility(View.VISIBLE);
        } else {
            // Show chips for each supervisor
            binding.supervisorChipGroup.setVisibility(View.VISIBLE);
            binding.supervisorStatusText.setVisibility(View.GONE);
            
            for (String email : currentSupervisors) {
                Chip chip = new Chip(requireContext());
                chip.setText(email);
                chip.setCloseIconVisible(true);
                chip.setCheckable(false);
                chip.setOnCloseIconClickListener(v -> {
                    // Remove from local list
                    currentSupervisors.remove(email);
                    // Notify listener
                    if (unassignListener != null) {
                        unassignListener.onUnassign(sensorId, email);
                    }
                    // Refresh chips
                    populateSupervisorChips();
                });
                binding.supervisorChipGroup.addView(chip);
            }
        }
    }

    private void setupAutocomplete() {
        emailAutocomplete = new SupervisorEmailAutocomplete(requireContext());
        emailAutocomplete.attachTo(
                binding.supervisorEmailInput,
                binding.supervisorEmailLayout,
                email -> {
                    // Email selected from dropdown - nothing special needed here
                    // The text is already set by the autocomplete
                }
        );
    }

    private void onSave() {
        String newName = "";
        if (binding.nameInput.getText() != null) {
            newName = binding.nameInput.getText().toString().trim();
        }
        
        String supervisorEmail = "";
        if (binding.supervisorEmailInput.getText() != null) {
            supervisorEmail = binding.supervisorEmailInput.getText().toString().trim();
        }

        // Only trigger save if name is not empty
        if (!newName.isEmpty() && saveListener != null) {
            // Only pass supervisor email if it's a new one (not already in the list)
            String emailToSave = null;
            if (!supervisorEmail.isEmpty()) {
                // Check if email is not already assigned
                boolean alreadyAssigned = currentSupervisors != null && 
                        currentSupervisors.contains(supervisorEmail);
                if (!alreadyAssigned) {
                    emailToSave = supervisorEmail;
                }
            }
            saveListener.onSave(sensorId, newName, emailToSave);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (emailAutocomplete != null) {
            emailAutocomplete.detach();
            emailAutocomplete = null;
        }
        binding = null;
    }
}
