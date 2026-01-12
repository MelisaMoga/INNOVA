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
import com.melisa.innovamotionapp.ui.dialogs.PersonNameEditDialog;
import com.melisa.innovamotionapp.ui.viewmodels.PersonNamesViewModel;
import com.melisa.innovamotionapp.utils.Logger;

/**
 * Activity for managing person display names.
 * 
 * Allows aggregator users to assign friendly names to sensor IDs.
 * Example: "sensor001" can be renamed to "Ion Popescu".
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
        adapter = new PersonNamesAdapter(this::showEditDialog);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
    }

    private void observeData() {
        viewModel.getAllPersons().observe(this, persons -> {
            adapter.submitList(persons);

            // Show/hide empty state
            boolean isEmpty = persons == null || persons.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            Logger.d(TAG, "Loaded " + (persons != null ? persons.size() : 0) + " persons");
        });
    }

    /**
     * Show dialog to edit a person's display name.
     */
    private void showEditDialog(MonitoredPerson person) {
        PersonNameEditDialog dialog = PersonNameEditDialog.newInstance(
                person.getSensorId(),
                person.getDisplayName()
        );
        dialog.setOnSaveListener((sensorId, newName) -> {
            viewModel.updateDisplayName(sensorId, newName);
            showToast(getString(R.string.name_updated, newName));
            Logger.userAction(TAG, "Updated name for " + sensorId + " to " + newName);
        });
        dialog.show(getSupportFragmentManager(), "edit_name");
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
