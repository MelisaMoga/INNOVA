package com.melisa.innovamotionapp.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.utils.Logger;
import com.melisa.innovamotionapp.utils.MockDataGenerator;
import com.melisa.innovamotionapp.utils.TestScenario;

import java.util.concurrent.Executors;

/**
 * Developer panel dialog for testing without Bluetooth hardware.
 * 
 * Allows selecting and running pre-defined test scenarios that inject
 * mock sensor data into the Room database.
 * 
 * Activated by shaking the device when DEV_MODE_ENABLED is true.
 */
public class DeveloperPanelDialog extends DialogFragment {

    private static final String TAG = "DeveloperPanelDialog";
    
    private MockDataGenerator mockDataGenerator;
    private Handler mainHandler;
    
    // Views
    private TextView dbStatsText;
    private RadioGroup scenarioRadioGroup;
    private SwitchMaterial switchSyncToFirestore;
    private ProgressBar progressBar;
    private TextView statusText;
    private MaterialButton btnRunScenario;
    private MaterialButton btnClearData;
    
    /**
     * Create a new instance of the developer panel dialog.
     */
    public static DeveloperPanelDialog newInstance() {
        return new DeveloperPanelDialog();
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mockDataGenerator = new MockDataGenerator(requireContext());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_developer_panel, null);
        
        initViews(view);
        setupListeners();
        updateStats();
        
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.dev_panel_title)
                .setView(view)
                .setNegativeButton(R.string.close, null)
                .create();
    }
    
    private void initViews(View view) {
        dbStatsText = view.findViewById(R.id.dbStatsText);
        scenarioRadioGroup = view.findViewById(R.id.scenarioRadioGroup);
        switchSyncToFirestore = view.findViewById(R.id.switchSyncToFirestore);
        progressBar = view.findViewById(R.id.progressBar);
        statusText = view.findViewById(R.id.statusText);
        btnRunScenario = view.findViewById(R.id.btnRunScenario);
        btnClearData = view.findViewById(R.id.btnClearData);
    }
    
    private void setupListeners() {
        btnRunScenario.setOnClickListener(v -> runSelectedScenario());
        btnClearData.setOnClickListener(v -> clearAllData());
    }
    
    /**
     * Get the currently selected test scenario.
     */
    private TestScenario getSelectedScenario() {
        int checkedId = scenarioRadioGroup.getCheckedRadioButtonId();
        
        if (checkedId == R.id.radioBasicMultiPerson) {
            return TestScenario.BASIC_MULTI_PERSON;
        } else if (checkedId == R.id.radioFallDetection) {
            return TestScenario.FALL_DETECTION;
        } else if (checkedId == R.id.radioStaleData) {
            return TestScenario.STALE_DATA;
        } else if (checkedId == R.id.radioHighVolume) {
            return TestScenario.HIGH_VOLUME;
        } else if (checkedId == R.id.radioNameResolution) {
            return TestScenario.NAME_RESOLUTION;
        } else if (checkedId == R.id.radioMixedStates) {
            return TestScenario.MIXED_STATES;
        }
        
        // Default to basic multi-person
        return TestScenario.BASIC_MULTI_PERSON;
    }
    
    /**
     * Run the selected test scenario.
     */
    private void runSelectedScenario() {
        TestScenario scenario = getSelectedScenario();
        boolean syncToFirestore = switchSyncToFirestore.isChecked();
        
        Logger.i(TAG, "Running scenario: " + scenario.getId() + ", syncToFirestore: " + syncToFirestore);
        
        // Configure generator
        mockDataGenerator.setSyncToFirestore(syncToFirestore);
        
        // Show progress
        setLoading(true);
        showStatus(getString(R.string.scenario_running));
        
        mockDataGenerator.runScenario(scenario, new MockDataGenerator.GenerationCallback() {
            @Override
            public void onComplete(int totalReadings, int totalSensors) {
                mainHandler.post(() -> {
                    if (syncToFirestore) {
                        // Keep loading while syncing to Firestore
                        showStatus(getString(R.string.syncing_to_firestore));
                    } else {
                        setLoading(false);
                        showStatus(getString(R.string.scenario_complete, totalReadings, totalSensors));
                        updateStats();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showStatus(getString(R.string.scenario_error, e.getMessage()));
                });
            }
            
            @Override
            public void onFirestoreSyncComplete(int syncedReadings) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showStatus(getString(R.string.firestore_sync_complete, syncedReadings));
                    updateStats();
                });
            }
            
            @Override
            public void onFirestoreSyncError(Exception e) {
                mainHandler.post(() -> {
                    setLoading(false);
                    showStatus(getString(R.string.firestore_sync_error, e.getMessage()));
                });
            }
        });
    }
    
    /**
     * Clear all data from the database.
     */
    private void clearAllData() {
        Logger.i(TAG, "Clearing all data");
        
        setLoading(true);
        showStatus(getString(R.string.scenario_running));
        
        mockDataGenerator.clearAllData(() -> {
            mainHandler.post(() -> {
                setLoading(false);
                showStatus(getString(R.string.data_cleared));
                updateStats();
            });
        });
    }
    
    /**
     * Update the database statistics display.
     */
    private void updateStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            int[] stats = mockDataGenerator.getStatistics();
            mainHandler.post(() -> {
                if (dbStatsText != null) {
                    dbStatsText.setText(getString(R.string.db_stats, stats[0], stats[1]));
                }
            });
        });
    }
    
    /**
     * Show or hide the loading indicator.
     */
    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRunScenario.setEnabled(!loading);
        btnClearData.setEnabled(!loading);
        scenarioRadioGroup.setEnabled(!loading);
        switchSyncToFirestore.setEnabled(!loading);
        
        // Disable radio buttons individually
        for (int i = 0; i < scenarioRadioGroup.getChildCount(); i++) {
            scenarioRadioGroup.getChildAt(i).setEnabled(!loading);
        }
    }
    
    /**
     * Show a status message.
     */
    private void showStatus(String message) {
        statusText.setText(message);
        statusText.setVisibility(View.VISIBLE);
    }
}
