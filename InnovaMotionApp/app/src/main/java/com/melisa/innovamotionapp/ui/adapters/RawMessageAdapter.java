package com.melisa.innovamotionapp.ui.adapters;

import android.content.Context;
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
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;
import com.melisa.innovamotionapp.data.posture.Posture;
import com.melisa.innovamotionapp.data.posture.PostureFactory;
import com.melisa.innovamotionapp.data.posture.types.FallingPosture;
import com.melisa.innovamotionapp.sync.ChildRegistryManager;
import com.melisa.innovamotionapp.sync.UserSession;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Adapter for displaying raw Bluetooth messages in a RecyclerView.
 * 
 * Shows: timestamp, child ID, friendly name, hex code, device address, posture description.
 * Color-codes messages by posture type (red for falls).
 */
public class RawMessageAdapter extends ListAdapter<ReceivedBtDataEntity, RawMessageAdapter.MessageViewHolder> {
    
    private final Context context;
    private final ChildRegistryManager childRegistry;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    
    public RawMessageAdapter(Context context) {
        super(new MessageDiffCallback());
        this.context = context;
        
        // Get child registry for name lookups
        UserSession userSession = UserSession.getInstance(context);
        this.childRegistry = userSession.getChildRegistry();
    }
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_raw_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ReceivedBtDataEntity message = getItem(position);
        holder.bind(message);
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView cardView;
        private final TextView tvTimestamp;
        private final TextView tvChildId;
        private final TextView tvFriendlyName;
        private final TextView tvHexCode;
        private final TextView tvDeviceAddress;
        private final TextView tvPostureDescription;
        
        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvChildId = itemView.findViewById(R.id.tvChildId);
            tvFriendlyName = itemView.findViewById(R.id.tvFriendlyName);
            tvHexCode = itemView.findViewById(R.id.tvHexCode);
            tvDeviceAddress = itemView.findViewById(R.id.tvDeviceAddress);
            tvPostureDescription = itemView.findViewById(R.id.tvPostureDescription);
        }
        
        public void bind(ReceivedBtDataEntity message) {
            // Format timestamp
            String timeStr = timeFormat.format(new Date(message.getTimestamp()));
            tvTimestamp.setText(timeStr);
            
            // Display child ID
            String childId = message.getOwnerUserId();
            if (childId != null && !childId.isEmpty()) {
                tvChildId.setText(childId);
                tvChildId.setVisibility(View.VISIBLE);
                
                // Look up friendly name from registry
                if (childRegistry != null) {
                    String friendlyName = childRegistry.getChildName(childId);
                    if (friendlyName != null && !friendlyName.equals(childId)) {
                        tvFriendlyName.setText(friendlyName);
                        tvFriendlyName.setVisibility(View.VISIBLE);
                    } else {
                        tvFriendlyName.setVisibility(View.GONE);
                    }
                } else {
                    tvFriendlyName.setVisibility(View.GONE);
                }
            } else {
                tvChildId.setText("(no ID)");
                tvChildId.setVisibility(View.VISIBLE);
                tvFriendlyName.setVisibility(View.GONE);
            }
            
            // Display hex code
            tvHexCode.setText(message.getReceivedMsg());
            
            // Display device address
            tvDeviceAddress.setText(message.getDeviceAddress());
            
            // Parse and display posture description
            try {
                Posture posture = PostureFactory.createPosture(message.getReceivedMsg());
                tvPostureDescription.setText(posture.getDescription());
                
                // Color-code by posture type
                if (posture instanceof FallingPosture) {
                    // Red background for falls
                    cardView.setCardBackgroundColor(context.getColor(android.R.color.holo_red_light));
                } else {
                    // Default background for normal postures
                    cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
                }
            } catch (Exception e) {
                tvPostureDescription.setText("");
                cardView.setCardBackgroundColor(context.getColor(android.R.color.white));
            }
        }
    }
    
    /**
     * DiffUtil callback for efficient list updates
     */
    static class MessageDiffCallback extends DiffUtil.ItemCallback<ReceivedBtDataEntity> {
        @Override
        public boolean areItemsTheSame(@NonNull ReceivedBtDataEntity oldItem, @NonNull ReceivedBtDataEntity newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull ReceivedBtDataEntity oldItem, @NonNull ReceivedBtDataEntity newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp() &&
                   oldItem.getReceivedMsg().equals(newItem.getReceivedMsg()) &&
                   oldItem.getOwnerUserId().equals(newItem.getOwnerUserId());
        }
    }
}

