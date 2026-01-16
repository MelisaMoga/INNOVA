package com.melisa.innovamotionapp.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.databinding.ActivityPersonNamesBinding;
import com.melisa.innovamotionapp.ui.adapters.PersonNamesAdapter;
import com.melisa.innovamotionapp.ui.dialogs.SensorSettingsDialog;
import com.melisa.innovamotionapp.ui.viewmodels.PersonNamesViewModel;
import com.melisa.innovamotionapp.utils.Logger;

import java.util.Map;

/**
 * Activity for managing person display names and supervisor assignments.
 * 
 * Allows aggregator users to:
 * - Assign friendly names to sensor IDs (e.g., "sensor001" → "Ion Popescu")
 * - Assign supervisors to sensors via email autocomplete
 */
public class PersonNamesActivity extends BaseActivity {

    private ActivityPersonNamesBinding binding;
    private PersonNamesViewModel viewModel;
    private PersonNamesAdapter adapter;

    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivityPersonNamesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PersonNamesViewModel.class);

        setupToolbar();
        setupRecyclerView();
        observeData();

        Logger.i(TAG, "PersonNamesActivity initialized");
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.manage_person_names);
        }
    }

    private void setupRecyclerView() {
        adapter = new PersonNamesAdapter(this::showSettingsDialog);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void observeData() {
        // Observe persons
        viewModel.getAllPersons().observe(this, persons -> {
            adapter.submitList(persons);

            // Show/hide empty state
            boolean isEmpty = persons == null || persons.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            Logger.d(TAG, "Loaded " + (persons != null ? persons.size() : 0) + " persons");
        });
        
        // Observe supervisor assignments
        viewModel.getSensorSupervisorMap().observe(this, map -> {
            adapter.setSupervisorMap(map);
            Logger.d(TAG, "Loaded " + (map != null ? map.size() : 0) + " supervisor assignments");
        });
    }

    /**
     * Show dialog to edit sensor settings (name and supervisor).
     */
    private void showSettingsDialog(MonitoredPerson person) {
        String supervisorEmail = viewModel.getFirstSupervisorForSensor(person.getSensorId());
        
        SensorSettingsDialog dialog = SensorSettingsDialog.newInstance(
                person.getSensorId(),
                person.getDisplayName(),
                supervisorEmail
        );
        
        dialog.setOnSaveListener((sensorId, newName, newSupervisorEmail) -> {
            // Update display name
            viewModel.updateDisplayName(sensorId, newName);
            showToast(getString(R.string.name_updated, newName));
            Logger.userAction(TAG, "Updated name for " + sensorId + " to " + newName);
            
            // Assign supervisor if email provided
            if (newSupervisorEmail != null && !newSupervisorEmail.isEmpty()) {
                viewModel.assignSupervisor(sensorId, newSupervisorEmail, 
                        new PersonNamesViewModel.AssignmentResultCallback() {
                    @Override
                    public void onSuccess() {
                        showToast(getString(R.string.supervisor_assignment_success));
                        Logger.userAction(TAG, "Assigned supervisor " + newSupervisorEmail + " to " + sensorId);
                    }

                    @Override
                    public void onError(String error) {
                        showToast(getString(R.string.supervisor_assignment_error, error));
                        Logger.e(TAG, "Failed to assign supervisor: " + error);
                    }
                });
            }
        });
        
        dialog.setOnUnassignListener(sensorId -> {
            viewModel.unassignAllFromSensor(sensorId, new PersonNamesViewModel.AssignmentResultCallback() {
                @Override
                public void onSuccess() {
                    showToast(getString(R.string.supervisor_unassigned_success));
                    Logger.userAction(TAG, "Unassigned supervisor from " + sensorId);
                }

                @Override
                public void onError(String error) {
                    showToast(getString(R.string.supervisor_assignment_error, error));
                    Logger.e(TAG, "Failed to unassign supervisor: " + error);
                }
            });
        });
        
        dialog.show(getSupportFragmentManager(), "sensor_settings");
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onBaseDestroy() {
        super.onBaseDestroy();
        binding = null;
    }
}
