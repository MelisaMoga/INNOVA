package com.melisa.innovamotionapp.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.melisa.innovamotionapp.R;

/**
 * Placeholder fragment for the Live Posture Viewer tab.
 * Will be implemented in Task 6.
 */
public class LivePostureFragment extends Fragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Simple placeholder view
        TextView textView = new TextView(requireContext());
        textView.setText(R.string.live_posture_placeholder);
        textView.setTextSize(18);
        textView.setGravity(android.view.Gravity.CENTER);
        textView.setPadding(32, 32, 32, 32);
        return textView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // To be implemented in Task 6
    }
}
