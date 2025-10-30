package com.melisa.innovamotionapp.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.ui.fragments.LivePostureFragment;
import com.melisa.innovamotionapp.ui.fragments.RawLogFragment;
import com.melisa.innovamotionapp.utils.GlobalData;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Data Aggregator Activity (formerly Supervised User)
 * 
 * Debug-friendly UI for aggregator phone that collects multi-user data.
 * 
 * Features:
 * - Two tabs: Raw Log and Live Posture
 * - Connection status display
 * - Packet statistics
 * - Multi-child monitoring
 */
public class DataAggregatorActivity extends AppCompatActivity {
    private static final String TAG = "DataAggregatorActivity";
    
    private TextView tvDeviceAddress;
    private TextView tvConnectionStatus;
    private TextView tvPacketStats;
    
    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_aggregator);
        
        Log.i(TAG, "DataAggregatorActivity created");
        
        // Initialize views
        initializeViews();
        
        // Setup ViewPager and TabLayout
        setupTabs();
        
        // Observe connection and packet statistics
        observeStatistics();
    }
    
    private void initializeViews() {
        tvDeviceAddress = findViewById(R.id.tvDeviceAddress);
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus);
        tvPacketStats = findViewById(R.id.tvPacketStats);
        
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
    }
    
    private void setupTabs() {
        // Create pager adapter with 2 fragments
        FragmentStateAdapter pagerAdapter = new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }
            
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return new RawLogFragment();
                } else {
                    return new LivePostureFragment();
                }
            }
        };
        
        viewPager.setAdapter(pagerAdapter);
        
        // Link TabLayout with ViewPager2
        new TabLayoutMediator(tabLayout, viewPager, 
            (tab, position) -> {
                if (position == 0) {
                    tab.setText("Raw Log");
                } else {
                    tab.setText("Live Posture");
                }
            }
        ).attach();
    }
    
    private void observeStatistics() {
        GlobalData globalData = GlobalData.getInstance();
        
        // Observe connection status
        globalData.getIsConnectedDevice().observe(this, isConnected -> {
            if (isConnected != null && isConnected) {
                tvConnectionStatus.setText("Status: Connected");
                tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_dark));
            } else {
                tvConnectionStatus.setText("Status: Disconnected");
                tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_dark));
            }
        });
        
        // Observe device address
        globalData.getBtDeviceAddress().observe(this, address -> {
            if (address != null && !address.isEmpty()) {
                tvDeviceAddress.setText("Device: " + address);
            } else {
                tvDeviceAddress.setText("Device: Not Connected");
            }
        });
        
        // Observe packet statistics
        globalData.getPacketCount().observe(this, packetCount -> {
            updatePacketStats(packetCount, globalData.getLastPacketTimestamp().getValue());
        });
        
        globalData.getLastPacketTimestamp().observe(this, timestamp -> {
            updatePacketStats(globalData.getPacketCount().getValue(), timestamp);
        });
    }
    
    private void updatePacketStats(Integer packetCount, Long lastTimestamp) {
        StringBuilder stats = new StringBuilder("Packets: ");
        
        if (packetCount != null) {
            stats.append(packetCount);
        } else {
            stats.append("0");
        }
        
        stats.append(" | Last: ");
        
        if (lastTimestamp != null && lastTimestamp > 0) {
            stats.append(timeFormat.format(new Date(lastTimestamp)));
        } else {
            stats.append("--");
        }
        
        tvPacketStats.setText(stats.toString());
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "DataAggregatorActivity destroyed");
    }
}

