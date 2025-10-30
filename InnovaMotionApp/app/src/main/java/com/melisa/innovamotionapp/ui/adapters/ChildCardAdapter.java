package com.melisa.innovamotionapp.ui.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.melisa.innovamotionapp.R;
import com.melisa.innovamotionapp.data.models.ChildPostureData;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.data.posture.types.SittingPosture;
import com.melisa.innovamotionapp.data.posture.types.StandingPosture;
import com.melisa.innovamotionapp.data.posture.types.WalkingPosture;

/**
 * Adapter for displaying child cards in supervisor dashboard.
 * Shows: name, location, latest posture, risk indicators.
 */
public class ChildCardAdapter extends ListAdapter<ChildPostureData, ChildCardAdapter.ChildViewHolder> {
    
    private final Context context;
    private OnChildClickListener clickListener;
    
    public interface OnChildClickListener {
        void onChildClick(ChildPostureData child);
    }
    
    public ChildCardAdapter(Context context) {
        super(new ChildDiffCallback());
        this.context = context;
    }
    
    public void setOnChildClickListener(OnChildClickListener listener) {
        this.clickListener = listener;
    }
    
    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_child_card, parent, false);
        return new ChildViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        ChildPostureData child = getItem(position);
        holder.bind(child);
    }
    
    class ChildViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvChildName;
        private final TextView tvChildId;
        private final TextView tvLocation;
        private final TextView tvPostureIcon;
        private final TextView tvPostureDescription;
        private final TextView tvLastUpdate;
        private final TextView tvRiskIndicator;
        
        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvChildName = itemView.findViewById(R.id.tvChildName);
            tvChildId = itemView.findViewById(R.id.tvChildId);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvPostureIcon = itemView.findViewById(R.id.tvPostureIcon);
            tvPostureDescription = itemView.findViewById(R.id.tvPostureDescription);
            tvLastUpdate = itemView.findViewById(R.id.tvLastUpdate);
            tvRiskIndicator = itemView.findViewById(R.id.tvRiskIndicator);
        }
        
        public void bind(ChildPostureData child) {
            // Display name
            String displayName = child.getDisplayName();
            tvChildName.setText(displayName);
            
            // Show child ID if different from display name
            if (!child.getChildId().equals(displayName)) {
                tvChildId.setText(child.getChildId());
                tvChildId.setVisibility(View.VISIBLE);
            } else {
                tvChildId.setVisibility(View.GONE);
            }
            
            // Show location if available
            String location = child.getLocation();
            if (location != null && !location.isEmpty()) {
                tvLocation.setText(location);
                tvLocation.setVisibility(View.VISIBLE);
            } else {
                tvLocation.setVisibility(View.GONE);
            }
            
            // Display posture information
            Posture posture = child.getLatestPosture();
            if (posture != null) {
                // Set posture icon
                tvPostureIcon.setText(getPostureEmoji(posture));
                
                // Set posture description
                tvPostureDescription.setText(posture.getDescription());
                
                // Check for fall risk
                if (posture instanceof FallingPosture) {
                    tvRiskIndicator.setText("⚠️ FALL DETECTED");
                    tvRiskIndicator.setTextColor(context.getColor(android.R.color.holo_red_dark));
                    tvRiskIndicator.setVisibility(View.VISIBLE);
                    cardView.setCardBackgroundColor(context.getColor(android.R.color.holo_red_light));
                } else {
                    tvRiskIndicator.setVisibility(View.GONE);
                    cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
                }
            } else {
                tvPostureIcon.setText("?");
                tvPostureDescription.setText("No data");
                tvRiskIndicator.setVisibility(View.GONE);
                cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
            }
            
            // Display last update time
            if (child.getLastUpdateTimestamp() > 0) {
                CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    child.getLastUpdateTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                );
                tvLastUpdate.setText("Last update: " + timeAgo);
                
                // Dim card if data is stale (>5 min)
                if (!child.hasRecentData()) {
                    itemView.setAlpha(0.6f);
                } else {
                    itemView.setAlpha(1.0f);
                }
            } else {
                tvLastUpdate.setText("Last update: --");
                itemView.setAlpha(0.6f);
            }
            
            // Click listener
            cardView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onChildClick(child);
                }
            });
        }
        
        private String getPostureEmoji(Posture posture) {
            if (posture instanceof StandingPosture) {
                return "🧍";
            } else if (posture instanceof SittingPosture) {
                return "🪑";
            } else if (posture instanceof WalkingPosture) {
                return "🚶";
            } else if (posture instanceof FallingPosture) {
                return "🔴";
            } else {
                return "❓";
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    static class ChildDiffCallback extends DiffUtil.ItemCallback<ChildPostureData> {
        @Override
        public boolean areItemsTheSame(@NonNull ChildPostureData oldItem, @NonNull ChildPostureData newItem) {
            return oldItem.getChildId().equals(newItem.getChildId());
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ChildPostureData oldItem, @NonNull ChildPostureData newItem) {
            // Compare relevant fields
            boolean samePosture = (oldItem.getLatestPosture() == null && newItem.getLatestPosture() == null) ||
                                (oldItem.getLatestPosture() != null && newItem.getLatestPosture() != null &&
                                 oldItem.getLatestPosture().getClass().equals(newItem.getLatestPosture().getClass()));
            
            return samePosture && 
                   oldItem.getLastUpdateTimestamp() == newItem.getLastUpdateTimestamp() &&
                   oldItem.getDisplayName().equals(newItem.getDisplayName());
        }
    }
}

