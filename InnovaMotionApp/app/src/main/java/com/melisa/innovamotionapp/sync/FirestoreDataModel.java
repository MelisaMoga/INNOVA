package com.melisa.innovamotionapp.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore data model for synchronized Bluetooth messages.
 * This class represents the structure of documents stored in Firestore.
 * 
 * Supports the multi-user protocol where each reading includes a sensorId
 * identifying the monitored person (e.g., "sensor001", UUID).
 */
public class FirestoreDataModel {
    private String deviceAddress;
    private long timestamp;
    private String receivedMsg;
    private String userId; // The aggregator user ID
    private String sensorId; // The monitored person's ID from hardware
    private long syncTimestamp; // When this was synced to Firestore
    private String documentId; // Unique document ID: userId_deviceAddress_sensorId_timestamp

    // Default constructor required for Firestore
    public FirestoreDataModel() {}

    /**
     * Full constructor for multi-user protocol data.
     *
     * @param deviceAddress Bluetooth MAC address of the hardware device
     * @param timestamp     When the reading was received (epoch millis)
     * @param receivedMsg   The hex code payload (e.g., "0xAB3311")
     * @param userId        The aggregator user ID who owns this data
     * @param sensorId      The monitored person's ID from hardware (e.g., "sensor001")
     */
    public FirestoreDataModel(String deviceAddress, long timestamp, String receivedMsg, String userId, String sensorId) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.userId = userId;
        this.sensorId = sensorId;
        this.syncTimestamp = System.currentTimeMillis();
        this.documentId = generateDocumentId(userId, deviceAddress, sensorId, timestamp);
    }

    /**
     * Generate a unique document ID using userId, deviceAddress, sensorId, and timestamp.
     * Format: userId_deviceAddress_sensorId_timestamp
     * 
     * Including sensorId ensures uniqueness even when multiple sensors report at the same timestamp.
     */
    public static String generateDocumentId(String userId, String deviceAddress, String sensorId, long timestamp) {
        String cleanDeviceAddress = deviceAddress != null ? deviceAddress.replace(":", "") : "unknown";
        String cleanSensorId = sensorId != null ? sensorId : "unknown";
        return userId + "_" + cleanDeviceAddress + "_" + cleanSensorId + "_" + timestamp;
    }

    /**
     * Convert to Firestore document format
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", deviceAddress);
        doc.put("timestamp", timestamp);
        doc.put("receivedMsg", receivedMsg);
        doc.put("userId", userId);
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
        model.userId = (String) doc.get("userId");
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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
