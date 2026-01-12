package com.melisa.innovamotionapp.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.DialogPersonNameEditBinding;

/**
 * Dialog for editing a monitored person's display name.
 * 
 * Shows the sensor ID (readonly) and allows editing the display name.
 * Uses OnSaveListener callback to notify when the user saves.
 */
public class PersonNameEditDialog extends DialogFragment {

    private static final String ARG_SENSOR_ID = "sensor_id";
    private static final String ARG_CURRENT_NAME = "current_name";

    private DialogPersonNameEditBinding binding;
    private OnSaveListener saveListener;

    /**
     * Callback interface for save events.
     */
    public interface OnSaveListener {
        void onSave(String sensorId, String newName);
    }

    /**
     * Create a new instance of the dialog.
     * 
     * @param sensorId    The sensor ID (displayed as readonly)
     * @param currentName The current display name (pre-filled in input)
     */
    public static PersonNameEditDialog newInstance(@NonNull String sensorId, @NonNull String currentName) {
        PersonNameEditDialog dialog = new PersonNameEditDialog();
        Bundle args = new Bundle();
        args.putString(ARG_SENSOR_ID, sensorId);
        args.putString(ARG_CURRENT_NAME, currentName);
        dialog.setArguments(args);
        return dialog;
    }

    /**
     * Set the listener for save events.
     */
    public void setOnSaveListener(OnSaveListener listener) {
        this.saveListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        binding = DialogPersonNameEditBinding.inflate(getLayoutInflater());

        String sensorId = requireArguments().getString(ARG_SENSOR_ID, "");
        String currentName = requireArguments().getString(ARG_CURRENT_NAME, "");

        // Display sensor ID (readonly)
        binding.sensorIdText.setText(sensorId);

        // Pre-fill current name and position cursor at end
        binding.nameInput.setText(currentName);
        binding.nameInput.setSelection(currentName.length());

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.edit_person_name)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    String newName = binding.nameInput.getText().toString().trim();
                    if (!newName.isEmpty() && saveListener != null) {
                        saveListener.onSave(sensorId, newName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
