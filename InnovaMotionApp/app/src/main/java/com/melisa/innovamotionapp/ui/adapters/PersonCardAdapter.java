package com.melisa.innovamotionapp.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.ItemPersonCardBinding;
import com.melisa.innovamotionapp.ui.models.PersonStatus;

/**
 * RecyclerView adapter for displaying monitored persons in a grid.
 * 
 * Features:
 * - Status indicator: green (active), yellow (stale > 5min), red (alert/fall)
 * - Alert badge for falling postures
 * - Relative time display ("Just now", "5m ago", etc.)
 */
public class PersonCardAdapter extends ListAdapter<PersonStatus, PersonCardAdapter.ViewHolder> {

    private static final long STALE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes

    private final OnPersonClickListener clickListener;

    /**
     * Callback interface for card click events.
     */
    public interface OnPersonClickListener {
        void onClick(PersonStatus person);
    }

    public PersonCardAdapter(@NonNull OnPersonClickListener listener) {
        super(new DiffCallback());
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPersonCardBinding binding = ItemPersonCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position), clickListener);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPersonCardBinding binding;

        ViewHolder(ItemPersonCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(PersonStatus person, OnPersonClickListener listener) {
            Context ctx = itemView.getContext();

            // Person name
            binding.personNameText.setText(person.getDisplayName());

            // Posture icon
            binding.postureIcon.setImageResource(person.getPostureIconRes());

            // Last update time
            String timeAgo = formatTimeAgo(person.getLastUpdateTime());
            binding.lastUpdateText.setText(timeAgo);

            // Status indicator color
            int statusColor;
            if (person.isAlert()) {
                // Red for falls
                statusColor = ContextCompat.getColor(ctx, R.color.status_alert);
                binding.alertBadge.setVisibility(View.VISIBLE);
            } else if (isStale(person.getLastUpdateTime())) {
                // Yellow for stale data
                statusColor = ContextCompat.getColor(ctx, R.color.status_stale);
                binding.alertBadge.setVisibility(View.GONE);
            } else {
                // Green for active
                statusColor = ContextCompat.getColor(ctx, R.color.status_active);
                binding.alertBadge.setVisibility(View.GONE);
            }
            binding.statusIndicator.setBackgroundColor(statusColor);

            // Click handler
            binding.cardView.setOnClickListener(v -> listener.onClick(person));
        }

        private boolean isStale(long timestamp) {
            return System.currentTimeMillis() - timestamp > STALE_THRESHOLD_MS;
        }

        private String formatTimeAgo(long timestamp) {
            long diff = System.currentTimeMillis() - timestamp;
            if (diff < 60_000) return "Just now";
            if (diff < 3600_000) return (diff / 60_000) + "m ago";
            if (diff < 86400_000) return (diff / 3600_000) + "h ago";
            return (diff / 86400_000) + "d ago";
        }
    }

    /**
     * DiffUtil callback for efficient list updates.
     */
    static class DiffCallback extends DiffUtil.ItemCallback<PersonStatus> {
        @Override
        public boolean areItemsTheSame(@NonNull PersonStatus oldItem, @NonNull PersonStatus newItem) {
            return oldItem.getSensorId().equals(newItem.getSensorId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull PersonStatus oldItem, @NonNull PersonStatus newItem) {
            return oldItem.equals(newItem);
        }
    }

    // ========== Static utility methods for testing ==========

    /**
     * Check if a timestamp is considered stale (> 5 minutes old).
     * Exposed for unit testing.
     */
    public static boolean isStale(long timestamp, long currentTime) {
        return currentTime - timestamp > STALE_THRESHOLD_MS;
    }

    /**
     * Format a timestamp as relative time.
     * Exposed for unit testing.
     */
    public static String formatTimeAgo(long timestamp, long currentTime) {
        long diff = currentTime - timestamp;
        if (diff < 60_000) return "Just now";
        if (diff < 3600_000) return (diff / 60_000) + "m ago";
        if (diff < 86400_000) return (diff / 3600_000) + "h ago";
        return (diff / 86400_000) + "d ago";
    }

    /**
     * Get the stale threshold in milliseconds.
     * Exposed for unit testing.
     */
    public static long getStaleThresholdMs() {
        return STALE_THRESHOLD_MS;
    }
}
