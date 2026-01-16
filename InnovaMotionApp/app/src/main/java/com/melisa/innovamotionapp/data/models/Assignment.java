package com.melisa.innovamotionapp.data.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore POJO for supervisor-sensor assignments.
 * 
 * Collection: assignments
 * Document ID: {supervisorUid}_{sensorId}
 * 
 * Represents a link between a supervisor and a sensor they can monitor.
 * This is the SINGLE source of truth for permissions - no arrays on user profiles.
 */
public class Assignment {
    private String supervisorUid;
    private String sensorId;
    private String assignedBy;
    private long assignedAt;

    // Default constructor required for Firestore
    public Assignment() {}

    public Assignment(String supervisorUid, String sensorId, String assignedBy) {
        this.supervisorUid = supervisorUid;
        this.sensorId = sensorId;
        this.assignedBy = assignedBy;
        this.assignedAt = System.currentTimeMillis();
    }

    /**
     * Generate the document ID for this assignment.
     * Format: {supervisorUid}_{sensorId}
     */
    public static String generateDocumentId(String supervisorUid, String sensorId) {
        return supervisorUid + "_" + sensorId;
    }

    /**
     * Get the document ID for this assignment.
     */
    public String getDocumentId() {
        return generateDocumentId(supervisorUid, sensorId);
    }

    /**
     * Create from Firestore document.
     */
    public static Assignment fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Assignment assignment = new Assignment();
        assignment.supervisorUid = doc.getString("supervisorUid");
        assignment.sensorId = doc.getString("sensorId");
        assignment.assignedBy = doc.getString("assignedBy");
        
        Long assignedAtVal = doc.getLong("assignedAt");
        assignment.assignedAt = assignedAtVal != null ? assignedAtVal : 0L;
        
        return assignment;
    }

    /**
     * Convert to Firestore document format.
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("supervisorUid", supervisorUid);
        doc.put("sensorId", sensorId);
        doc.put("assignedBy", assignedBy);
        doc.put("assignedAt", assignedAt);
        return doc;
    }

    // ========== Getters and Setters ==========

    public String getSupervisorUid() {
        return supervisorUid;
    }

    public void setSupervisorUid(String supervisorUid) {
        this.supervisorUid = supervisorUid;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public long getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(long assignedAt) {
        this.assignedAt = assignedAt;
    }
}
