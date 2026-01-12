package com.melisa.innovamotionapp.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;

import com.google.android.material.tabs.TabLayoutMediator;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.ActivityAggregatorDashboardBinding;
import com.melisa.innovamotionapp.ui.adapters.AggregatorDashboardPagerAdapter;
import com.melisa.innovamotionapp.utils.Logger;

/**
 * Aggregator Dashboard Activity.
 * 
 * Provides a tabbed interface for aggregator users to:
 * - Tab 1: View real-time message log from all sensors
 * - Tab 2: View live posture visualization for a selected person
 * 
 * This is the main debugging/monitoring interface for aggregator phones
 * that collect data from Bluetooth sensors.
 */
public class AggregatorDashboardActivity extends BaseActivity {

    private ActivityAggregatorDashboardBinding binding;
    private AggregatorDashboardPagerAdapter pagerAdapter;

    @Override
    protected void onBaseCreate(@Nullable Bundle savedInstanceState) {
        binding = ActivityAggregatorDashboardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        setupViewPager();
        setupTabLayout();
        
        Logger.i(TAG, "AggregatorDashboardActivity initialized");
    }

    private void setupViewPager() {
        pagerAdapter = new AggregatorDashboardPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);
        
        // Disable swipe if needed (uncomment if tabs should only be changed via TabLayout)
        // binding.viewPager.setUserInputEnabled(false);
    }

    private void setupTabLayout() {
        new TabLayoutMediator(binding.tabLayout, binding.viewPager, (tab, position) -> {
            switch (position) {
                case AggregatorDashboardPagerAdapter.TAB_MESSAGE_LOG:
                    tab.setText(R.string.tab_message_log);
                    tab.setIcon(R.drawable.ic_list);
                    break;
                case AggregatorDashboardPagerAdapter.TAB_LIVE_POSTURE:
                    tab.setText(R.string.tab_live_posture);
                    tab.setIcon(R.drawable.ic_person);
                    break;
            }
        }).attach();
    }

    @Override
    protected void onBaseResume() {
        super.onBaseResume();
        Logger.d(TAG, "AggregatorDashboardActivity resumed");
    }

    @Override
    protected void onBaseDestroy() {
        super.onBaseDestroy();
        binding = null;
    }
}
