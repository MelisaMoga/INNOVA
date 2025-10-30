package com.melisa.innovamotionapp.data.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.melisa.innovamotionapp.data.posture.Posture;

/**
 * Combined data model for displaying a child's current state in the supervisor dashboard.
 * 
 * Includes:
 * - Child profile (name, location, etc.)
 * - Latest posture data
 * - Timestamp information
 */
public class ChildPostureData {
    @NonNull
    private final String childId;
    
    @Nullable
    private final ChildProfile profile;
    
    @Nullable
    private final Posture latestPosture;
    
    private final long lastUpdateTimestamp;
    
    @Nullable
    private final String hexCode;
    
    public ChildPostureData(@NonNull String childId, 
                           @Nullable ChildProfile profile,
                           @Nullable Posture latestPosture,
                           long lastUpdateTimestamp,
                           @Nullable String hexCode) {
        this.childId = childId;
        this.profile = profile;
        this.latestPosture = latestPosture;
        this.lastUpdateTimestamp = lastUpdateTimestamp;
        this.hexCode = hexCode;
    }
    
    @NonNull
    public String getChildId() {
        return childId;
    }
    
    @Nullable
    public ChildProfile getProfile() {
        return profile;
    }
    
    @Nullable
    public Posture getLatestPosture() {
        return latestPosture;
    }
    
    public long getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }
    
    @Nullable
    public String getHexCode() {
        return hexCode;
    }
    
    /**
     * Get display name for this child (friendly name or ID)
     */
    @NonNull
    public String getDisplayName() {
        if (profile != null) {
            return profile.getDisplayName();
        }
        return childId;
    }
    
    /**
     * Get location if available
     */
    @Nullable
    public String getLocation() {
        if (profile != null) {
            return profile.getLocation();
        }
        return null;
    }
    
    /**
     * Check if this child has recent data (within last 5 minutes)
     */
    public boolean hasRecentData() {
        if (lastUpdateTimestamp == 0) {
            return false;
        }
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000);
        return lastUpdateTimestamp > fiveMinutesAgo;
    }
    
    /**
     * Get time since last update in milliseconds
     */
    public long getTimeSinceLastUpdate() {
        if (lastUpdateTimestamp == 0) {
            return Long.MAX_VALUE;
        }
        return System.currentTimeMillis() - lastUpdateTimestamp;
    }
}

