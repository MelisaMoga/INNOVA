package com.melisa.innovamotionapp.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore data model for synchronized Bluetooth messages.
 * This class represents the structure of documents stored in Firestore.
 */
public class FirestoreDataModel {
    private String deviceAddress;
    private long timestamp;
    private String receivedMsg;
    private String userId; // The supervised user ID
    private long syncTimestamp; // When this was synced to Firestore
    private String documentId; // Unique document ID: userId_deviceAddress_timestamp

    // Default constructor required for Firestore
    public FirestoreDataModel() {}

    public FirestoreDataModel(String deviceAddress, long timestamp, String receivedMsg, String userId) {
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.userId = userId;
        this.syncTimestamp = System.currentTimeMillis();
        this.documentId = generateDocumentId(userId, deviceAddress, timestamp);
    }

    /**
     * Generate a unique document ID using userId, deviceAddress, and timestamp
     * Format: userId_deviceAddress_timestamp
     */
    public static String generateDocumentId(String userId, String deviceAddress, long timestamp) {
        return userId + "_" + deviceAddress.replace(":", "") + "_" + timestamp;
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
        model.timestamp = (Long) doc.get("timestamp");
        model.receivedMsg = (String) doc.get("receivedMsg");
        model.userId = (String) doc.get("userId");
        model.syncTimestamp = (Long) doc.get("syncTimestamp");
        model.documentId = (String) doc.get("documentId");
        return model;
    }

    // Getters and Setters
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
