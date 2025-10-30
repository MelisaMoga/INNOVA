package com.melisa.innovamotionapp.ui.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.InnovaDatabase;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.ui.adapters.RawMessageAdapter;

import java.util.List;

/**
 * Raw Log Fragment
 * 
 * Displays a scrolling list of recent Bluetooth messages from all children.
 * Shows: timestamp, child ID, friendly name, hex code, device address.
 */
public class RawLogFragment extends Fragment {
    private static final String TAG = "RawLogFragment";
    private static final int MAX_MESSAGES = 100;
    
    private RecyclerView recyclerView;
    private TextView tvEmptyState;
    private RawMessageAdapter adapter;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_raw_log, container, false);
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        Log.i(TAG, "RawLogFragment view created");
        
        // Initialize views
        recyclerView = view.findViewById(R.id.recyclerView);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);
        
        // Setup RecyclerView
        setupRecyclerView();
        
        // Observe recent messages from database
        observeRecentMessages();
    }
    
    private void setupRecyclerView() {
        adapter = new RawMessageAdapter(requireContext());
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        // Auto-scroll to top when new messages arrive
        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                if (positionStart == 0) {
                    recyclerView.scrollToPosition(0);
                }
            }
        });
    }
    
    private void observeRecentMessages() {
        InnovaDatabase database = InnovaDatabase.getInstance(requireContext());
        
        database.receivedBtDataDao()
                .getRecentMessages(MAX_MESSAGES)
                .observe(getViewLifecycleOwner(), new Observer<List<ReceivedBtDataEntity>>() {
                    @Override
                    public void onChanged(List<ReceivedBtDataEntity> messages) {
                        if (messages != null && !messages.isEmpty()) {
                            Log.d(TAG, "Received " + messages.size() + " messages from database");
                            adapter.submitList(messages);
                            tvEmptyState.setVisibility(View.GONE);
                            recyclerView.setVisibility(View.VISIBLE);
                        } else {
                            Log.d(TAG, "No messages in database");
                            tvEmptyState.setVisibility(View.VISIBLE);
                            recyclerView.setVisibility(View.GONE);
                        }
                    }
                });
    }
}

