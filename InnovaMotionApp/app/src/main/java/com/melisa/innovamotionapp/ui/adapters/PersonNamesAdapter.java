package com.melisa.innovamotionapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.data.database.MonitoredPerson;
import com.melisa.innovamotionapp.databinding.ItemPersonNameBinding;

/**
 * RecyclerView adapter for displaying monitored persons with their display names.
 * Uses ListAdapter with DiffUtil for efficient updates.
 */
public class PersonNamesAdapter extends ListAdapter<MonitoredPerson, PersonNamesAdapter.ViewHolder> {

    private final OnItemClickListener clickListener;

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonNameBinding binding = ItemPersonNameBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonNameBinding binding;

        ViewHolder(ItemPersonNameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MonitoredPerson person, OnItemClickListener listener) {
            // Display name (bold, primary text)
            binding.displayNameText.setText(person.getDisplayName());
            
            // Sensor ID (smaller, gray text)
            binding.sensorIdText.setText(person.getSensorId());

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
