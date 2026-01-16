package com.melisa.innovamotionapp.data.models;

import com.google.firebase.firestore.DocumentSnapshot;
import com.melisa.innovamotionapp.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Firestore POJO for user profiles.
 * 
 * Collection: users
 * Document ID: {uid}
 * 
 * Users can have one or both roles (aggregator, supervisor).
 */
public class UserProfile {
    private String uid;
    private String email;
    private String displayName;
    private String photoUrl;
    private List<String> roles;
    private long createdAt;
    private long lastSignIn;

    // Default constructor required for Firestore
    public UserProfile() {
        this.roles = new ArrayList<>();
    }

    public UserProfile(String uid, String email, String displayName, String photoUrl, List<String> roles) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.lastSignIn = System.currentTimeMillis();
    }

    /**
     * Create from Firestore document.
     */
    public static UserProfile fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        UserProfile profile = new UserProfile();
        profile.uid = doc.getId();
        profile.email = doc.getString("email");
        profile.displayName = doc.getString("displayName");
        profile.photoUrl = doc.getString("photoUrl");
        
        // Handle roles - can be a list or a single string (legacy)
        Object rolesObj = doc.get("roles");
        if (rolesObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<String> rolesList = (List<String>) rolesObj;
            profile.roles = new ArrayList<>(rolesList);
        } else {
            // Legacy: single "role" string field
            String legacyRole = doc.getString("role");
            profile.roles = new ArrayList<>();
            if (legacyRole != null) {
                profile.roles.add(legacyRole);
            }
        }
        
        Long createdAtVal = doc.getLong("createdAt");
        profile.createdAt = createdAtVal != null ? createdAtVal : 0L;
        
        Long lastSignInVal = doc.getLong("lastSignIn");
        profile.lastSignIn = lastSignInVal != null ? lastSignInVal : 0L;
        
        return profile;
    }

    /**
     * Convert to Firestore document format.
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("email", email);
        doc.put("displayName", displayName);
        doc.put("photoUrl", photoUrl);
        doc.put("roles", roles);
        doc.put("createdAt", createdAt);
        doc.put("lastSignIn", lastSignIn);
        return doc;
    }

    // ========== Role Helpers ==========

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    public boolean isAggregator() {
        return hasRole(Constants.ROLE_AGGREGATOR);
    }

    public boolean isSupervisor() {
        return hasRole(Constants.ROLE_SUPERVISOR);
    }

    public boolean hasBothRoles() {
        return isAggregator() && isSupervisor();
    }

    // ========== Getters and Setters ==========

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<String> getRoles() {
        return roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public void setRoles(List<String> roles) {
        this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getLastSignIn() {
        return lastSignIn;
    }

    public void setLastSignIn(long lastSignIn) {
        this.lastSignIn = lastSignIn;
    }
}
