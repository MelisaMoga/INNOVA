package com.melisa.innovamotionapp.ui.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.activities.PersonDetailActivity;
import com.melisa.innovamotionapp.databinding.FragmentLivePostureBinding;
import com.melisa.innovamotionapp.ui.adapters.PersonCardAdapter;
import com.melisa.innovamotionapp.ui.models.PersonStatus;
import com.melisa.innovamotionapp.ui.viewmodels.LivePostureViewModel;
import com.melisa.innovamotionapp.utils.Logger;

/**
 * Fragment for viewing live posture of all monitored persons.
 * 
 * Refactored to match Supervisor Dashboard style:
 * - Grid of person cards with posture icons and status indicators
 * - Real-time updates from Room database
 * - Click on person to navigate to PersonDetailActivity
 * 
 * Both Aggregator and Supervisor now share the same user experience:
 * Dashboard Grid → Click Person → PersonDetailActivity → Navigate to sub-activities
 */
public class LivePostureFragment extends Fragment {

    private static final String TAG = "UI/LivePosture";
    
    private FragmentLivePostureBinding binding;
    private LivePostureViewModel viewModel;
    private PersonCardAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentLivePostureBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Logger.d(TAG, "LivePostureFragment view created");
        
        viewModel = new ViewModelProvider(this).get(LivePostureViewModel.class);

        setupRecyclerView();
        setupSwipeRefresh();
        observeData();
        
        Logger.i(TAG, "LivePostureFragment initialized (Dashboard Grid Style)");
    }

    /**
     * Setup RecyclerView with grid layout and PersonCardAdapter.
     * Reuses the same adapter as SupervisorDashboardActivity for DRY.
     */
    private void setupRecyclerView() {
        // Handle card click - navigate to PersonDetailActivity
        adapter = new PersonCardAdapter(this::onPersonClick);

        // Grid layout (2 columns on phones, consistent with Supervisor dashboard)
        int spanCount = getResources().getInteger(R.integer.dashboard_span_count);
        binding.recyclerView.setLayoutManager(new GridLayoutManager(requireContext(), spanCount));
        binding.recyclerView.setAdapter(adapter);
    }

    /**
     * Setup swipe-to-refresh functionality.
     */
    private void setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener(() -> {
            Logger.d(TAG, "Swipe refresh triggered");
            viewModel.refreshData();
        });
    }

    /**
     * Observe ViewModel data and update UI accordingly.
     */
    private void observeData() {
        viewModel.getPersonStatuses().observe(getViewLifecycleOwner(), statuses -> {
            adapter.submitList(statuses);
            binding.swipeRefresh.setRefreshing(false);

            // Show empty state
            boolean isEmpty = statuses == null || statuses.isEmpty();
            binding.emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
            binding.recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
            
            // Hide progress bar once data is loaded
            binding.progressBar.setVisibility(View.GONE);

            Logger.d(TAG, "Loaded " + (statuses != null ? statuses.size() : 0) + " persons");
        });

        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // Only show main progress bar if list is empty, otherwise swipe refresh handles it
            if (isLoading && adapter.getItemCount() == 0) {
                binding.progressBar.setVisibility(View.VISIBLE);
            } else if (!isLoading) {
                binding.progressBar.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Handle card click - navigate to PersonDetailActivity with sensor context.
     * Same behavior as SupervisorDashboardActivity.
     */
    private void onPersonClick(PersonStatus person) {
        Logger.userAction(TAG, "Clicked on person: " + person.getDisplayName());

        // Navigate to PersonDetailActivity with person info
        Intent intent = new Intent(requireContext(), PersonDetailActivity.class);
        intent.putExtra(PersonDetailActivity.EXTRA_SENSOR_ID, person.getSensorId());
        intent.putExtra(PersonDetailActivity.EXTRA_PERSON_NAME, person.getDisplayName());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
