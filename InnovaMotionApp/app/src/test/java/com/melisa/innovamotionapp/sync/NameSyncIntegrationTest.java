package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.models.Sensor;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for sensor name save/get logic during sync.
 * 
 * Focus on the name saving and retrieval logic that occurs during data sync.
 * These tests simulate the name resolution pipeline without Firebase dependencies.
 * 
 * Tests cover:
 * - Sensor has displayName field
 * - Save then load preserves name (round-trip)
 * - Update name, verify change
 * - Default to sensorId when no name
 * - Multiple sensors with different names
 * - Name cached in local database simulation
 */
public class NameSyncIntegrationTest {

    private MockNameCache nameCache;

    @Before
    public void setUp() {
        nameCache = new MockNameCache();
    }

    // ========== Sensor Name Field Tests ==========

    @Test
    public void testSensorNameInInventory() {
        // Sensors in the inventory should have a displayName field
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Living Room");

        assertEquals("Living Room", sensor.getDisplayName());
        assertNotNull(sensor.getSensorId());
    }

    @Test
    public void testSensorNameRoundTrip() {
        // Save then load should preserve the name
        Sensor original = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Kitchen Sensor");

        // Simulate saving to Firestore
        Map<String, Object> doc = original.toFirestoreDocument();

        // Simulate loading from Firestore
        Sensor restored = new Sensor();
        restored.setSensorId("sensor001"); // From document ID
        restored.setDeviceAddress((String) doc.get("deviceAddress"));
        restored.setOwnerUid((String) doc.get("ownerUid"));
        restored.setDisplayName((String) doc.get("displayName"));

        assertEquals(original.getDisplayName(), restored.getDisplayName());
        assertEquals("Kitchen Sensor", restored.getDisplayName());
    }

    @Test
    public void testSensorNameUpdate() {
        // Update name and verify the change
        nameCache.put("sensor001", "Old Name");
        assertEquals("Old Name", nameCache.get("sensor001"));

        // Update the name
        nameCache.put("sensor001", "New Name");
        assertEquals("New Name", nameCache.get("sensor001"));
    }

    @Test
    public void testSensorNameDefaultsToSensorId() {
        // When displayName is null or empty, should fallback to sensorId
        Sensor sensorNoName = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", null);
        Sensor sensorEmptyName = new Sensor("sensor002", "AA:BB:CC:DD:EE:FF", "owner123", "");

        String displayName1 = getDisplayNameWithFallback(sensorNoName);
        String displayName2 = getDisplayNameWithFallback(sensorEmptyName);

        assertEquals("sensor001", displayName1);
        assertEquals("sensor002", displayName2);
    }

    @Test
    public void testMultipleSensorNames() {
        // Multiple sensors with different names
        List<Sensor> sensors = new ArrayList<>();
        sensors.add(new Sensor("sensor001", "AA:BB:CC:DD:EE:01", "owner", "Child 1 Room"));
        sensors.add(new Sensor("sensor002", "AA:BB:CC:DD:EE:02", "owner", "Child 2 Room"));
        sensors.add(new Sensor("sensor003", "AA:BB:CC:DD:EE:03", "owner", "Living Room"));

        // Cache all names
        for (Sensor s : sensors) {
            nameCache.put(s.getSensorId(), s.getDisplayName());
        }

        // Verify each name is stored correctly
        assertEquals("Child 1 Room", nameCache.get("sensor001"));
        assertEquals("Child 2 Room", nameCache.get("sensor002"));
        assertEquals("Living Room", nameCache.get("sensor003"));
        assertEquals(3, nameCache.size());
    }

    @Test
    public void testSensorNameInLocalDb() {
        // Simulate caching names in local Room database (via MockNameCache)
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Bedroom");

        // Simulate what downloadToLocal does
        nameCache.put(sensor.getSensorId(), sensor.getDisplayName());

        // Verify the name is cached
        assertTrue(nameCache.contains("sensor001"));
        assertEquals("Bedroom", nameCache.get("sensor001"));
    }

    // ========== Name Sync Workflow Tests ==========

    @Test
    public void testNameSyncFromFirestoreSensor() {
        // Simulate syncing sensor info from Firestore to local cache
        Map<String, Object> firestoreDoc = new HashMap<>();
        firestoreDoc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        firestoreDoc.put("ownerUid", "owner123");
        firestoreDoc.put("displayName", "Synced Sensor Name");

        // Parse sensor
        Sensor sensor = new Sensor();
        sensor.setSensorId("syncedSensor001");
        sensor.setDeviceAddress((String) firestoreDoc.get("deviceAddress"));
        sensor.setOwnerUid((String) firestoreDoc.get("ownerUid"));
        sensor.setDisplayName((String) firestoreDoc.get("displayName"));

        // Cache locally
        nameCache.put(sensor.getSensorId(), sensor.getDisplayName());

        // Verify
        assertEquals("Synced Sensor Name", nameCache.get("syncedSensor001"));
    }

    @Test
    public void testNameSyncPreservesExistingOnConflict() {
        // If local has a newer name, should it be preserved?
        // This depends on sync strategy - for now we test overwrite behavior
        nameCache.put("sensor001", "Local Name");

        // Simulate receiving older data from Firestore
        String firestoreName = "Firestore Name";
        nameCache.put("sensor001", firestoreName);

        // Last write wins (simple strategy)
        assertEquals("Firestore Name", nameCache.get("sensor001"));
    }

    @Test
    public void testBulkNameSync() {
        // Simulate syncing multiple sensor names at once
        List<Sensor> sensorsFromFirestore = new ArrayList<>();
        sensorsFromFirestore.add(new Sensor("s1", null, "owner", "Name 1"));
        sensorsFromFirestore.add(new Sensor("s2", null, "owner", "Name 2"));
        sensorsFromFirestore.add(new Sensor("s3", null, "owner", "Name 3"));
        sensorsFromFirestore.add(new Sensor("s4", null, "owner", null)); // No name

        // Bulk sync
        for (Sensor s : sensorsFromFirestore) {
            String name = getDisplayNameWithFallback(s);
            nameCache.put(s.getSensorId(), name);
        }

        assertEquals("Name 1", nameCache.get("s1"));
        assertEquals("Name 2", nameCache.get("s2"));
        assertEquals("Name 3", nameCache.get("s3"));
        assertEquals("s4", nameCache.get("s4")); // Fallback to sensorId
    }

    // ========== Name Lookup During Data Display Tests ==========

    @Test
    public void testNameLookupForDataDisplay() {
        // When displaying sensor data, we need to resolve the sensor name
        nameCache.put("sensor001", "Child's Room");
        nameCache.put("sensor002", "Living Room");

        // Simulate data records that only have sensorId
        List<String> dataRecordSensorIds = List.of("sensor001", "sensor002", "sensor003");

        // Resolve names
        List<String> displayNames = new ArrayList<>();
        for (String sensorId : dataRecordSensorIds) {
            String name = nameCache.getOrDefault(sensorId, sensorId);
            displayNames.add(name);
        }

        assertEquals("Child's Room", displayNames.get(0));
        assertEquals("Living Room", displayNames.get(1));
        assertEquals("sensor003", displayNames.get(2)); // Fallback - not in cache
    }

    // ========== Helper Methods ==========

    private String getDisplayNameWithFallback(Sensor sensor) {
        String displayName = sensor.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            return sensor.getSensorId();
        }
        return displayName;
    }

    // ========== Mock Name Cache ==========

    /**
     * Mock implementation of a name cache (simulates Room database or in-memory cache).
     */
    static class MockNameCache {
        private final Map<String, String> cache = new HashMap<>();

        public void put(String sensorId, String displayName) {
            cache.put(sensorId, displayName);
        }

        public String get(String sensorId) {
            return cache.get(sensorId);
        }

        public String getOrDefault(String sensorId, String defaultValue) {
            return cache.getOrDefault(sensorId, defaultValue);
        }

        public boolean contains(String sensorId) {
            return cache.containsKey(sensorId);
        }

        public int size() {
            return cache.size();
        }

        public void clear() {
            cache.clear();
        }
    }
}
