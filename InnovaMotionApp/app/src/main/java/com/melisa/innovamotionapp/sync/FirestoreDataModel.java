package com.melisa.innovamotionapp.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore data model for synchronized Bluetooth messages.
 * This class represents the structure of documents stored in Firestore.
 * 
 * Supports the multi-user protocol where each reading includes a sensorId
 * identifying the monitored person (e.g., "sensor001", UUID).
 * 
 * Document ID format: {deviceAddress}_{sensorId}_{timestamp}
 * This format is aggregator-agnostic, allowing data to be queried by sensorId only.
 */
public class FirestoreDataModel {
    private String deviceAddress;
    private long timestamp;
    private String receivedMsg;
    private String uploadedBy; // The aggregator user ID who uploaded this (metadata only, not part of ID)
    private String sensorId; // The monitored person's ID from hardware
    private long syncTimestamp; // When this was synced to Firestore
    private String documentId; // Unique document ID: deviceAddress_sensorId_timestamp

    // Default constructor required for Firestore
    public FirestoreDataModel() {}

    /**
     * Full constructor for multi-user protocol data.
     *
     * @param deviceAddress Bluetooth MAC address of the hardware device
     * @param timestamp     When the reading was received (epoch millis)
     * @param receivedMsg   The hex code payload (e.g., "0xAB3311")
     * @param uploadedBy    The aggregator user ID who uploaded this data (metadata)
     * @param sensorId      The monitored person's ID from hardware (e.g., "sensor001")
     */
    public FirestoreDataModel(String deviceAddress, long timestamp, String receivedMsg, String uploadedBy, String sensorId) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.uploadedBy = uploadedBy;
        this.sensorId = sensorId;
        this.syncTimestamp = System.currentTimeMillis();
        this.documentId = generateDocumentId(deviceAddress, sensorId, timestamp);
    }

    /**
     * Generate a unique document ID using deviceAddress, sensorId, and timestamp.
     * Format: deviceAddress_sensorId_timestamp
     * 
     * This format is aggregator-agnostic - the same sensor always produces the same
     * document ID regardless of which aggregator uploads the data.
     */
    public static String generateDocumentId(String deviceAddress, String sensorId, long timestamp) {
        String cleanDeviceAddress = deviceAddress != null ? deviceAddress.replace(":", "") : "unknown";
        String cleanSensorId = sensorId != null ? sensorId : "unknown";
        return cleanDeviceAddress + "_" + cleanSensorId + "_" + timestamp;
    }
    
    /**
     * @deprecated Use {@link #generateDocumentId(String, String, long)} instead.
     * Kept for backward compatibility during migration.
     */
    @Deprecated
    public static String generateDocumentId(String userId, String deviceAddress, String sensorId, long timestamp) {
        // Delegate to new format (ignoring userId)
        return generateDocumentId(deviceAddress, sensorId, timestamp);
    }

    /**
     * Convert to Firestore document format
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", deviceAddress);
        doc.put("timestamp", timestamp);
        doc.put("receivedMsg", receivedMsg);
        doc.put("uploadedBy", uploadedBy);
        doc.put("sensorId", sensorId);
        doc.put("syncTimestamp", syncTimestamp);
        doc.put("documentId", documentId);
        return doc;
    }

    /**
     * Create from Firestore document
     */
    public static FirestoreDataModel fromFirestoreDocument(Map<String, Object> doc) {
        FirestoreDataModel model = new FirestoreDataModel();
        model.deviceAddress = (String) doc.get("deviceAddress");
        
        // Handle timestamp - could be Long or null
        Object tsObj = doc.get("timestamp");
        model.timestamp = tsObj instanceof Long ? (Long) tsObj : 0L;
        
        model.receivedMsg = (String) doc.get("receivedMsg");
        
        // Handle uploadedBy with fallback to legacy userId field
        model.uploadedBy = (String) doc.get("uploadedBy");
        if (model.uploadedBy == null) {
            model.uploadedBy = (String) doc.get("userId"); // Legacy fallback
        }
        
        model.sensorId = (String) doc.get("sensorId");
        
        // Handle syncTimestamp - could be Long or null
        Object syncTsObj = doc.get("syncTimestamp");
        model.syncTimestamp = syncTsObj instanceof Long ? (Long) syncTsObj : 0L;
        
        model.documentId = (String) doc.get("documentId");
        return model;
    }

    // ========== Getters and Setters ==========

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getReceivedMsg() {
        return receivedMsg;
    }

    public void setReceivedMsg(String receivedMsg) {
        this.receivedMsg = receivedMsg;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
    
    /**
     * @deprecated Use {@link #getUploadedBy()} instead.
     */
    @Deprecated
    public String getUserId() {
        return uploadedBy;
    }

    /**
     * @deprecated Use {@link #setUploadedBy(String)} instead.
     */
    @Deprecated
    public void setUserId(String userId) {
        this.uploadedBy = userId;
    }

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public long getSyncTimestamp() {
        return syncTimestamp;
    }

    public void setSyncTimestamp(long syncTimestamp) {
        this.syncTimestamp = syncTimestamp;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }
}
