package com.melisa.innovamotionapp.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.DialogSensorSettingsBinding;
import com.melisa.innovamotionapp.sync.SensorAssignmentService;
import com.melisa.innovamotionapp.ui.helpers.SupervisorEmailAutocomplete;

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
    private static final String ARG_CURRENT_SUPERVISOR = "current_supervisor";

    private DialogSensorSettingsBinding binding;
    private SupervisorEmailAutocomplete emailAutocomplete;
    private OnSaveListener saveListener;
    private OnUnassignListener unassignListener;
    
    private String sensorId;
    private String currentSupervisorEmail;

    /**
     * Callback for when the user saves changes.
     */
    public interface OnSaveListener {
        /**
         * Called when user saves the dialog.
         * 
         * @param sensorId The sensor ID
         * @param newName The new display name
         * @param supervisorEmail The supervisor email (may be empty if not changed)
         */
        void onSave(String sensorId, String newName, @Nullable String supervisorEmail);
    }

    /**
     * Callback for when the user unassigns the supervisor.
     */
    public interface OnUnassignListener {
        void onUnassign(String sensorId);
    }

    /**
     * Create a new instance of the dialog.
     * 
     * @param sensorId           The sensor ID (displayed as readonly)
     * @param currentName        The current display name (pre-filled in input)
     * @param currentSupervisor  The currently assigned supervisor email (or null)
     */
    public static SensorSettingsDialog newInstance(@NonNull String sensorId, 
                                                    @NonNull String currentName,
                                                    @Nullable String currentSupervisor) {
        SensorSettingsDialog dialog = new SensorSettingsDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SENSOR_ID, sensorId);
        args.putString(ARG_CURRENT_NAME, currentName);
        args.putString(ARG_CURRENT_SUPERVISOR, currentSupervisor);
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
        currentSupervisorEmail = requireArguments().getString(ARG_CURRENT_SUPERVISOR, null);

        setupUI(currentName);
        setupAutocomplete();
        setupUnassignButton();

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

        // Show supervisor status
        updateSupervisorStatus();
    }

    private void updateSupervisorStatus() {
        if (currentSupervisorEmail != null && !currentSupervisorEmail.isEmpty()) {
            binding.supervisorStatusText.setText(
                    getString(R.string.supervisor_assigned, currentSupervisorEmail)
            );
            binding.unassignButton.setVisibility(View.VISIBLE);
            // Pre-fill the email input with current supervisor
            binding.supervisorEmailInput.setText(currentSupervisorEmail);
        } else {
            binding.supervisorStatusText.setText(R.string.supervisor_not_assigned);
            binding.unassignButton.setVisibility(View.GONE);
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

    private void setupUnassignButton() {
        binding.unassignButton.setOnClickListener(v -> {
            if (unassignListener != null) {
                unassignListener.onUnassign(sensorId);
            }
            // Clear the UI state
            currentSupervisorEmail = null;
            binding.supervisorEmailInput.setText("");
            updateSupervisorStatus();
            dismiss();
        });
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
            // Only pass supervisor email if it's different from current or newly entered
            String emailToSave = null;
            if (!supervisorEmail.isEmpty()) {
                // If email is different from current, or if there's a new one
                if (currentSupervisorEmail == null || !supervisorEmail.equals(currentSupervisorEmail)) {
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
