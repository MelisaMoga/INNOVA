package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.ActivitySupervisorDashboardBinding;
import com.melisa.innovamotionapp.ui.adapters.PersonCardAdapter;
import com.melisa.innovamotionapp.ui.models.PersonStatus;
import com.melisa.innovamotionapp.ui.viewmodels.SupervisorDashboardViewModel;
import com.melisa.innovamotionapp.utils.Logger;

/**
 * Dashboard showing all monitored persons in a grid.
 * 
 * Features:
 * - Grid of cards with posture icons and status indicators
 * - Alert badges for falling postures
 * - Color-coded status: green (active), yellow (stale), red (alert)
 * - Swipe-to-refresh
 * - Empty state when no persons
 * - Tap to view detail (navigates to BtConnectedActivity)
 */
public class SupervisorDashboardActivity extends BaseActivity {

    public static final String EXTRA_SENSOR_ID = "sensor_id";
    public static final String EXTRA_PERSON_NAME = "person_name";

    private ActivitySupervisorDashboardBinding binding;
    private SupervisorDashboardViewModel viewModel;
    private PersonCardAdapter adapter;

    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivitySupervisorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(SupervisorDashboardViewModel.class);

        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        observeData();

        Logger.i(TAG, "SupervisorDashboardActivity initialized");
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.supervisor_dashboard);
        }
    }

    private void setupRecyclerView() {
        adapter = new PersonCardAdapter(this::onPersonClick);

        // Grid layout for cards (2 columns on phones, 3 on tablets)
        int spanCount = getResources().getInteger(R.integer.dashboard_span_count);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(this, spanCount));
        binding.recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            Logger.d(TAG, "Swipe refresh triggered");
            viewModel.refreshData();
        });
    }

    private void observeData() {
        viewModel.getPersonStatuses().observe(this, statuses -> {
            adapter.submitList(statuses);
            binding.swipeRefresh.setRefreshing(false);

            // Show empty state
            boolean isEmpty = statuses == null || statuses.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

            Logger.d(TAG, "Loaded " + (statuses != null ? statuses.size() : 0) + " persons");
        });

        viewModel.getIsLoading().observe(this, isLoading -> {
            binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
    }

    /**
     * Handle card click - navigate to detail view.
     */
    private void onPersonClick(PersonStatus person) {
        Logger.userAction(TAG, "Clicked on person: " + person.getDisplayName());

        // Navigate to BtConnectedActivity with person info
        Intent intent = new Intent(this, BtConnectedActivity.class);
        intent.putExtra(EXTRA_SENSOR_ID, person.getSensorId());
        intent.putExtra(EXTRA_PERSON_NAME, person.getDisplayName());
        startActivity(intent);
    }

    @Override
    protected void onBaseDestroy() {
        super.onBaseDestroy();
        binding = null;
    }
}
