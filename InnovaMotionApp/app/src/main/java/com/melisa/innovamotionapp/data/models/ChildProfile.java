package com.melisa.innovamotionapp.data.models;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Profile information for a monitored child/person.
 * 
 * This is NOT a user account - just metadata associated with a childId
 * from the Bluetooth device messages.
 * 
 * Stored in Firestore under: aggregators/{aggregatorId}/children/{childId}
 */
public class ChildProfile {
    @NonNull
    private String childId;         // The ID from Bluetooth messages (e.g., "sensor001")
    
    @Nullable
    private String name;            // Friendly name (e.g., "John Doe")
    
    @Nullable
    private String location;        // Location info (e.g., "Room 201")
    
    @Nullable
    private String notes;           // Additional notes
    
    private long addedAt;           // Timestamp when first seen/registered
    
    private long lastSeen;          // Timestamp of last message received
    
    // Default constructor required for Firestore serialization
    public ChildProfile() {
        this.childId = "";
        this.addedAt = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }
    
    public ChildProfile(@NonNull String childId) {
        this.childId = childId;
        this.addedAt = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }
    
    public ChildProfile(@NonNull String childId, @Nullable String name, @Nullable String location, @Nullable String notes) {
        this.childId = childId;
        this.name = name;
        this.location = location;
        this.notes = notes;
        this.addedAt = System.currentTimeMillis();
        this.lastSeen = System.currentTimeMillis();
    }
    
    // Getters
    @NonNull
    public String getChildId() {
        return childId;
    }
    
    @Nullable
    public String getName() {
        return name;
    }
    
    @Nullable
    public String getLocation() {
        return location;
    }
    
    @Nullable
    public String getNotes() {
        return notes;
    }
    
    public long getAddedAt() {
        return addedAt;
    }
    
    public long getLastSeen() {
        return lastSeen;
    }
    
    // Setters
    public void setChildId(@NonNull String childId) {
        this.childId = childId;
    }
    
    public void setName(@Nullable String name) {
        this.name = name;
    }
    
    public void setLocation(@Nullable String location) {
        this.location = location;
    }
    
    public void setNotes(@Nullable String notes) {
        this.notes = notes;
    }
    
    public void setAddedAt(long addedAt) {
        this.addedAt = addedAt;
    }
    
    public void setLastSeen(long lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    /**
     * Get display name - returns name if set, otherwise returns childId
     */
    @NonNull
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        return childId;
    }
    
    /**
     * Update last seen timestamp to current time
     */
    public void updateLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }
    
    @Override
    public String toString() {
        return "ChildProfile{" +
                "childId='" + childId + '\'' +
                ", name='" + name + '\'' +
                ", location='" + location + '\'' +
                ", notes='" + notes + '\'' +
                ", addedAt=" + addedAt +
                ", lastSeen=" + lastSeen +
                '}';
    }
}

