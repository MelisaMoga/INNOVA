package com.melisa.innovamotionapp.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore data model for synchronized Bluetooth messages.
 * Multi-user architecture: aggregator collects data for multiple children.
 * 
 * Document structure:
 * - childId: The monitored person (e.g., "sensor001")
 * - aggregatorId: The aggregator account uploading (e.g., "building_a@facility.com")
 * - deviceAddress: Bluetooth device MAC address
 * - timestamp: Message timestamp
 * - receivedMsg: Posture hex code (e.g., "0xAB3311")
 */
public class FirestoreDataModel {
    private String childId;           // The monitored person
    private String aggregatorId;      // The aggregator account uploading
    private String deviceAddress;
    private long timestamp;
    private String receivedMsg;       // Hex code only
    private long syncTimestamp;       // When this was synced to Firestore
    private String documentId;        // Unique document ID: aggregatorId_childId_timestamp

    // Default constructor required for Firestore
    public FirestoreDataModel() {}

    /**
     * Constructor for multi-user architecture.
     * @param childId The monitored person ID
     * @param aggregatorId The aggregator account ID
     * @param deviceAddress Bluetooth device MAC address
     * @param timestamp Message timestamp
     * @param receivedMsg Posture hex code
     */
    public FirestoreDataModel(String childId, String aggregatorId, String deviceAddress, long timestamp, String receivedMsg) {
        this.childId = childId;
        this.aggregatorId = aggregatorId;
        this.deviceAddress = deviceAddress;
        this.timestamp = timestamp;
        this.receivedMsg = receivedMsg;
        this.syncTimestamp = System.currentTimeMillis();
        this.documentId = generateDocumentId(aggregatorId, childId, timestamp);
    }

    /**
     * Generate a unique document ID using aggregatorId, childId, and timestamp
     * Format: aggregatorId_childId_timestamp
     */
    public static String generateDocumentId(String aggregatorId, String childId, long timestamp) {
        // Sanitize IDs by removing special characters
        String sanitizedAggregator = aggregatorId.replace("@", "_").replace(".", "_").replace(":", "");
        String sanitizedChild = childId.replace(":", "").replace("@", "_").replace(".", "_");
        return sanitizedAggregator + "_" + sanitizedChild + "_" + timestamp;
    }

    /**
     * Convert to Firestore document format
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("childId", childId);
        doc.put("aggregatorId", aggregatorId);
        doc.put("deviceAddress", deviceAddress);
        doc.put("timestamp", timestamp);
        doc.put("receivedMsg", receivedMsg);
        doc.put("syncTimestamp", syncTimestamp);
        doc.put("documentId", documentId);
        return doc;
    }

    /**
     * Create from Firestore document
     */
    public static FirestoreDataModel fromFirestoreDocument(Map<String, Object> doc) {
        FirestoreDataModel model = new FirestoreDataModel();
        model.childId = (String) doc.get("childId");
        model.aggregatorId = (String) doc.get("aggregatorId");
        model.deviceAddress = (String) doc.get("deviceAddress");
        model.timestamp = (Long) doc.get("timestamp");
        model.receivedMsg = (String) doc.get("receivedMsg");
        model.syncTimestamp = (Long) doc.get("syncTimestamp");
        model.documentId = (String) doc.get("documentId");
        return model;
    }

    // Getters and Setters
    public String getChildId() {
        return childId;
    }

    public void setChildId(String childId) {
        this.childId = childId;
    }

    public String getAggregatorId() {
        return aggregatorId;
    }

    public void setAggregatorId(String aggregatorId) {
        this.aggregatorId = aggregatorId;
    }

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
