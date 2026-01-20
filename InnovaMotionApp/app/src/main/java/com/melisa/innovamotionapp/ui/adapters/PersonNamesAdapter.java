package com.melisa.innovamotionapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.databinding.ItemPersonNameBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RecyclerView adapter for displaying monitored persons with their display names
 * and supervisor assignments.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
public class PersonNamesAdapter extends ListAdapter<MonitoredPerson, PersonNamesAdapter.ViewHolder> {

    private final OnItemClickListener clickListener;
    private Map<String, List<String>> supervisorMap = new HashMap<>();

    /**
     * Callback interface for item click events.
     */
    public interface OnItemClickListener {
        void onClick(MonitoredPerson person);
    }

    public PersonNamesAdapter(@NonNull OnItemClickListener listener) {
        super(new DiffCallback());
        this.clickListener = listener;
    }

    /**
     * Set the supervisor map (sensorId -> list of supervisor emails).
     * This triggers a rebind of all items.
     */
    public void setSupervisorMap(@Nullable Map<String, List<String>> map) {
        this.supervisorMap = map != null ? map : new HashMap<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonNameBinding binding = ItemPersonNameBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MonitoredPerson person = getItem(position);
        List<String> supervisorEmails = supervisorMap.get(person.getSensorId());
        holder.bind(person, supervisorEmails, clickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonNameBinding binding;

        ViewHolder(ItemPersonNameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MonitoredPerson person, @Nullable List<String> supervisorEmails, OnItemClickListener listener) {
            // Display name (bold, primary text)
            binding.displayNameText.setText(person.getDisplayName());
            
            // Sensor ID (smaller, gray text)
            binding.sensorIdText.setText(person.getSensorId());
            
            // Supervisor info - show all assigned supervisors
            if (supervisorEmails != null && !supervisorEmails.isEmpty()) {
                String supervisorText;
                if (supervisorEmails.size() == 1) {
                    // Single supervisor
                    supervisorText = binding.getRoot().getContext().getString(R.string.supervisor_label) 
                            + " " + supervisorEmails.get(0);
                } else {
                    // Multiple supervisors - show all comma-separated
                    supervisorText = binding.getRoot().getContext().getString(R.string.supervisors_label) 
                            + " " + String.join(", ", supervisorEmails);
                }
                binding.supervisorText.setText(supervisorText);
                binding.supervisorText.setVisibility(View.VISIBLE);
            } else {
                binding.supervisorText.setVisibility(View.GONE);
            }

            // Click handlers
            binding.editButton.setOnClickListener(v -> listener.onClick(person));
            binding.getRoot().setOnClickListener(v -> listener.onClick(person));
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    static class DiffCallback extends DiffUtil.ItemCallback<MonitoredPerson> {
        @Override
        public boolean areItemsTheSame(@NonNull MonitoredPerson oldItem, @NonNull MonitoredPerson newItem) {
            // Items are the same if they have the same sensor ID
            return oldItem.getSensorId().equals(newItem.getSensorId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull MonitoredPerson oldItem, @NonNull MonitoredPerson newItem) {
            // Contents are the same if display name matches
            return oldItem.getDisplayName().equals(newItem.getDisplayName());
        }
    }
}
