package com.melisa.innovamotionapp.data.models;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

/**
 * Firestore POJO for sensor inventory.
 * 
 * Collection: sensors
 * Document ID: {sensorId}
 * 
 * Represents a physical sensor device owned by an aggregator.
 */
public class Sensor {
    private String sensorId;
    private String deviceAddress;
    private String ownerUid;
    private String displayName;

    // Default constructor required for Firestore
    public Sensor() {}

    public Sensor(String sensorId, String deviceAddress, String ownerUid, String displayName) {
        this.sensorId = sensorId;
        this.deviceAddress = deviceAddress;
        this.ownerUid = ownerUid;
        this.displayName = displayName;
    }

    /**
     * Create from Firestore document.
     */
    public static Sensor fromDocument(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) {
            return null;
        }
        Sensor sensor = new Sensor();
        sensor.sensorId = doc.getId();
        sensor.deviceAddress = doc.getString("deviceAddress");
        sensor.ownerUid = doc.getString("ownerUid");
        sensor.displayName = doc.getString("displayName");
        return sensor;
    }

    /**
     * Convert to Firestore document format.
     */
    public Map<String, Object> toFirestoreDocument() {
        Map<String, Object> doc = new HashMap<>();
        doc.put("deviceAddress", deviceAddress);
        doc.put("ownerUid", ownerUid);
        doc.put("displayName", displayName);
        return doc;
    }

    // ========== Getters and Setters ==========

    public String getSensorId() {
        return sensorId;
    }

    public void setSensorId(String sensorId) {
        this.sensorId = sensorId;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public String getOwnerUid() {
        return ownerUid;
    }

    public void setOwnerUid(String ownerUid) {
        this.ownerUid = ownerUid;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
