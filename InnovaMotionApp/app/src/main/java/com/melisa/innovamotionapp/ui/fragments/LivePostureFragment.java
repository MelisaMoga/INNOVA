package com.melisa.innovamotionapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.models.ChildProfile;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.sync.ChildRegistryManager;
import com.melisa.innovamotionapp.sync.UserSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Live Posture Fragment
 * 
 * Allows selecting a child and viewing their live posture with animation.
 */
public class LivePostureFragment extends Fragment {
    private static final String TAG = "LivePostureFragment";
    
    private Spinner spinnerChildSelector;
    private VideoView videoViewPosture;
    private TextView tvPostureDescription;
    private TextView tvRiskIndicator;
    private TextView tvLastUpdate;
    private TextView tvNoChildren;
    
    private ChildRegistryManager childRegistry;
    private InnovaDatabase database;
    
    private String selectedChildId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_live_posture, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.i(TAG, "LivePostureFragment view created");
        
        // Initialize views
        spinnerChildSelector = view.findViewById(R.id.spinnerChildSelector);
        videoViewPosture = view.findViewById(R.id.videoViewPosture);
        tvPostureDescription = view.findViewById(R.id.tvPostureDescription);
        tvRiskIndicator = view.findViewById(R.id.tvRiskIndicator);
        tvLastUpdate = view.findViewById(R.id.tvLastUpdate);
        tvNoChildren = view.findViewById(R.id.tvNoChildren);
        
        // Get services
        UserSession userSession = UserSession.getInstance(requireContext());
        childRegistry = userSession.getChildRegistry();
        database = InnovaDatabase.getInstance(requireContext());
        
        // Setup child selector
        setupChildSelector();
    }
    
    private void setupChildSelector() {
        if (childRegistry == null) {
            Log.w(TAG, "Child registry not available");
            showNoChildrenState();
            return;
        }
        
        List<ChildProfile> children = childRegistry.getAllChildren();
        
        if (children.isEmpty()) {
            Log.d(TAG, "No children registered yet");
            showNoChildrenState();
            return;
        }
        
        // Create display names list
        List<String> displayNames = new ArrayList<>();
        for (ChildProfile child : children) {
            displayNames.add(child.getDisplayName());
        }
        
        // Setup spinner adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            displayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerChildSelector.setAdapter(adapter);
        
        // Handle child selection
        spinnerChildSelector.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ChildProfile selectedChild = children.get(position);
                selectedChildId = selectedChild.getChildId();
                Log.i(TAG, "Selected child: " + selectedChildId);
                observeChildPosture(selectedChildId);
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedChildId = null;
            }
        });
        
        // Show content, hide empty state
        tvNoChildren.setVisibility(View.GONE);
    }
    
    private void showNoChildrenState() {
        tvNoChildren.setVisibility(View.VISIBLE);
        spinnerChildSelector.setVisibility(View.GONE);
        videoViewPosture.setVisibility(View.GONE);
        tvPostureDescription.setText("No posture data");
        tvRiskIndicator.setText("");
        tvLastUpdate.setText("Last update: --");
    }
    
    private void observeChildPosture(String childId) {
        database.receivedBtDataDao()
                .getLatestForOwner(childId)
                .observe(getViewLifecycleOwner(), new Observer<ReceivedBtDataEntity>() {
                    @Override
                    public void onChanged(ReceivedBtDataEntity entity) {
                        if (entity != null) {
                            displayPosture(entity);
                        } else {
                            Log.d(TAG, "No posture data for child: " + childId);
                            tvPostureDescription.setText("No posture data yet");
                            tvRiskIndicator.setText("");
                            tvLastUpdate.setText("Last update: --");
                        }
                    }
                });
    }
    
    private void displayPosture(ReceivedBtDataEntity entity) {
        try {
            // Parse posture from hex code
            Posture posture = PostureFactory.createPosture(entity.getReceivedMsg());
            
            // Update description
            String description = posture.getDescription();
            tvPostureDescription.setText(description);
            
            // Update risk indicator
            if (posture instanceof FallingPosture) {
                tvRiskIndicator.setText("⚠️ FALL DETECTED");
                tvRiskIndicator.setTextColor(requireContext().getColor(android.R.color.holo_red_dark));
            } else {
                tvRiskIndicator.setText("Normal");
                tvRiskIndicator.setTextColor(requireContext().getColor(android.R.color.holo_green_dark));
            }
            
            // Update last update timestamp
            String timeStr = timeFormat.format(new Date(entity.getTimestamp()));
            tvLastUpdate.setText("Last update: " + timeStr);
            
            // Play posture video/animation
            playPostureVideo(posture);
            
        } catch (Exception e) {
            Log.e(TAG, "Error displaying posture", e);
            tvPostureDescription.setText("Error parsing posture");
        }
    }
    
    private void playPostureVideo(Posture posture) {
        // Get video resource for posture
        int videoResId = posture.getVideoResourceID(requireContext());
        
        if (videoResId != 0) {
            String videoPath = "android.resource://" + requireContext().getPackageName() + "/" + videoResId;
            videoViewPosture.setVideoPath(videoPath);
            videoViewPosture.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                videoViewPosture.start();
            });
            videoViewPosture.setVisibility(View.VISIBLE);
        } else {
            Log.w(TAG, "No video resource for posture: " + posture.getClass().getSimpleName());
            videoViewPosture.setVisibility(View.GONE);
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        if (videoViewPosture.isPlaying()) {
            videoViewPosture.pause();
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        if (videoViewPosture.isPlaying()) {
            videoViewPosture.start();
        }
    }
}

