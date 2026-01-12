package com.melisa.innovamotionapp.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.databinding.ItemMessageLogBinding;
import com.melisa.innovamotionapp.ui.models.MessageLogItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RecyclerView adapter for the message log list.
 * Uses ListAdapter with DiffUtil for efficient updates.
 * Highlights fall messages with a red background.
 */
public class MessageLogAdapter extends ListAdapter<MessageLogItem, MessageLogAdapter.ViewHolder> {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault());

    public MessageLogAdapter() {
        super(new DiffCallback());
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemMessageLogBinding binding = ItemMessageLogBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemMessageLogBinding binding;

        ViewHolder(ItemMessageLogBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(MessageLogItem item) {
            // Display name (person name or sensor ID)
            binding.sensorNameText.setText(item.getDisplayName());
            
            // Hex code
            binding.hexCodeText.setText(item.getHexCode());
            
            // Timestamp
            binding.timestampText.setText(formatTimestamp(item.getTimestamp()));
            
            // Posture icon
            binding.postureIcon.setImageResource(item.getPostureIconRes());
            
            // Highlight falls in red
            if (item.isFall()) {
                binding.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.fall_alert_background));
                binding.sensorNameText.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.fall_alert_text));
            } else {
                binding.cardView.setCardBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.card_background));
                binding.sensorNameText.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.card_text_primary));
            }
        }

        private String formatTimestamp(long timestamp) {
            Date date = new Date(timestamp);
            // If today, show only time; otherwise show date + time
            long now = System.currentTimeMillis();
            long dayInMillis = 24 * 60 * 60 * 1000;
            if (now - timestamp < dayInMillis) {
                return TIME_FORMAT.format(date);
            } else {
                return DATE_TIME_FORMAT.format(date);
            }
        }
    }

    static class DiffCallback extends DiffUtil.ItemCallback<MessageLogItem> {
        @Override
        public boolean areItemsTheSame(@NonNull MessageLogItem oldItem, @NonNull MessageLogItem newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull MessageLogItem oldItem, @NonNull MessageLogItem newItem) {
            return oldItem.equals(newItem);
        }
    }
}
