package com.melisa.innovamotionapp.ui.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.melisa.innovamotionapp.ui.fragments.LivePostureFragment;
import com.melisa.innovamotionapp.ui.fragments.MessageLogFragment;

/**
 * ViewPager2 adapter for the Aggregator Dashboard.
 * Manages two tabs:
 * - Tab 0: Message Log (real-time message stream)
 * - Tab 1: Live Posture (posture visualization for selected person)
 */
public class AggregatorDashboardPagerAdapter extends FragmentStateAdapter {

    public static final int TAB_MESSAGE_LOG = 0;
    public static final int TAB_LIVE_POSTURE = 1;
    public static final int TAB_COUNT = 2;

    public AggregatorDashboardPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case TAB_MESSAGE_LOG:
                return new MessageLogFragment();
            case TAB_LIVE_POSTURE:
                return new LivePostureFragment();
            default:
                throw new IllegalArgumentException("Invalid tab position: " + position);
        }
    }

    @Override
    public int getItemCount() {
        return TAB_COUNT;
    }
}
