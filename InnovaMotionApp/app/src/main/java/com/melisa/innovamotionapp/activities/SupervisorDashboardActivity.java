package com.melisa.innovamotionapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.models.ChildPostureData;
import com.melisa.innovamotionapp.ui.adapters.ChildCardAdapter;
import com.melisa.innovamotionapp.ui.viewmodels.SupervisorDashboardViewModel;

/**
 * Supervisor Dashboard Activity
 * 
 * Displays all children from the linked aggregator in a grid.
 * Shows each child's:
 * - Name (or ID if name not set)
 * - Location (if available)
 * - Latest posture
 * - Last update time
 * - Risk indicators (e.g., falls)
 * 
 * Click on a child card to view detailed monitoring for that individual.
 */
public class SupervisorDashboardActivity extends AppCompatActivity {
    private static final String TAG = "SupervisorDashboard";
    
    private TextView tvAggregatorId;
    private TextView tvChildCount;
    private RecyclerView recyclerViewChildren;
    private LinearLayout emptyStateLayout;
    
    private SupervisorDashboardViewModel viewModel;
    private ChildCardAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_supervisor_dashboard);
        
        Log.i(TAG, "SupervisorDashboardActivity created");
        
        // Initialize views
        initializeViews();
        
        // Setup ViewModel
        setupViewModel();
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Observe data
        observeData();
    }
    
    private void initializeViews() {
        tvAggregatorId = findViewById(R.id.tvAggregatorId);
        tvChildCount = findViewById(R.id.tvChildCount);
        recyclerViewChildren = findViewById(R.id.recyclerViewChildren);
        emptyStateLayout = findViewById(R.id.emptyStateLayout);
    }
    
    private void setupViewModel() {
        viewModel = new ViewModelProvider(this).get(SupervisorDashboardViewModel.class);
        
        // Display linked aggregator ID
        String aggregatorId = viewModel.getLinkedAggregatorId();
        if (aggregatorId != null && !aggregatorId.isEmpty()) {
            tvAggregatorId.setText(aggregatorId);
        } else {
            tvAggregatorId.setText("Not linked to aggregator");
        }
    }
    
    private void setupRecyclerView() {
        // Use grid layout with 2 columns
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        recyclerViewChildren.setLayoutManager(layoutManager);
        
        // Create adapter
        adapter = new ChildCardAdapter(this);
        recyclerViewChildren.setAdapter(adapter);
        
        // Handle child card clicks
        adapter.setOnChildClickListener(child -> {
            Log.i(TAG, "Child clicked: " + child.getChildId());
            openChildDetail(child);
        });
    }
    
    private void observeData() {
        viewModel.getChildrenData().observe(this, children -> {
            if (children != null && !children.isEmpty()) {
                Log.d(TAG, "Received " + children.size() + " children");
                
                // Update child count
                tvChildCount.setText("Children: " + children.size());
                
                // Show data
                adapter.submitList(children);
                recyclerViewChildren.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
            } else {
                Log.d(TAG, "No children data");
                
                // Show empty state
                tvChildCount.setText("Children: 0");
                recyclerViewChildren.setVisibility(View.GONE);
                emptyStateLayout.setVisibility(View.VISIBLE);
            }
        });
    }
    
    /**
     * Open child detail view (reuse existing BtConnectedActivity for individual monitoring)
     */
    private void openChildDetail(ChildPostureData child) {
        // For now, we can reuse BtConnectedActivity or create a new ChildDetailActivity
        // Let's create an intent to BtConnectedActivity with child ID as extra
        Intent intent = new Intent(this, BtConnectedActivity.class);
        intent.putExtra("child_id", child.getChildId());
        intent.putExtra("child_name", child.getDisplayName());
        startActivity(intent);
    }
}

