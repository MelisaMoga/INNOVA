package com.melisa.innovamotionapp.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
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
 * - Tap to view detail (navigates to PersonDetailActivity)
 */
public class SupervisorDashboardActivity extends BaseActivity {

    public static final String EXTRA_SENSOR_ID = "extra_sensor_id";
    public static final String EXTRA_PERSON_NAME = "extra_person_name";

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
        
        // Request notification permission for alerts (Android 13+)
        requestNotificationPermission();

        Logger.i(TAG, "SupervisorDashboardActivity initialized");
    }
    
    /**
     * Request POST_NOTIFICATIONS permission on Android 13+ (Tiramisu).
     * Required for supervisors to receive posture alert notifications.
     */
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Logger.d(TAG, "Requesting POST_NOTIFICATIONS permission");
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
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

        // Navigate to PersonDetailActivity with person info
        Intent intent = new Intent(this, PersonDetailActivity.class);
        intent.putExtra(EXTRA_SENSOR_ID, person.getSensorId());
        intent.putExtra(EXTRA_PERSON_NAME, person.getDisplayName());
        startActivity(intent);
    }

    @Override
    protected void onBaseDestroy() {
        super.onBaseDestroy();
        binding = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.supervisor_dashboard_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            Logger.userAction(TAG, "Sign out menu item clicked");
            signOut();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
