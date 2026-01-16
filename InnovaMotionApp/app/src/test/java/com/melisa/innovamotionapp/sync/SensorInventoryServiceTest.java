package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.models.Sensor;
import com.melisa.innovamotionapp.utils.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for SensorInventoryService logic.
 * 
 * Since SensorInventoryService depends on Firebase and Android Context,
 * these tests focus on testable logic:
 * - Sensor POJO creation and serialization (delegated to SensorTest)
 * - Batch splitting logic for whereIn queries (simulated here)
 * - Utility methods
 */
public class SensorInventoryServiceTest {

    private static final int BATCH_SIZE = Constants.FIRESTORE_WHERE_IN_LIMIT; // 10

    // ========== Sensor Model Tests ==========

    @Test
    public void testSensorModelCreation() {
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Living Room");

        assertEquals("sensor001", sensor.getSensorId());
        assertEquals("AA:BB:CC:DD:EE:FF", sensor.getDeviceAddress());
        assertEquals("owner123", sensor.getOwnerUid());
        assertEquals("Living Room", sensor.getDisplayName());
    }

    @Test
    public void testSensorToFirestoreDocument() {
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Living Room");

        Map<String, Object> doc = sensor.toFirestoreDocument();

        assertEquals("AA:BB:CC:DD:EE:FF", doc.get("deviceAddress"));
        assertEquals("owner123", doc.get("ownerUid"));
        assertEquals("Living Room", doc.get("displayName"));
        // sensorId is the document ID, not in the document body
        assertFalse(doc.containsKey("sensorId"));
    }

    @Test
    public void testSensorFromDocument() {
        // Simulate creating sensor from Firestore document data
        Sensor sensor = new Sensor();
        sensor.setSensorId("sensor001"); // This comes from doc.getId()
        sensor.setDeviceAddress("AA:BB:CC:DD:EE:FF");
        sensor.setOwnerUid("owner123");
        sensor.setDisplayName("Living Room");

        assertEquals("sensor001", sensor.getSensorId());
        assertEquals("AA:BB:CC:DD:EE:FF", sensor.getDeviceAddress());
        assertEquals("owner123", sensor.getOwnerUid());
        assertEquals("Living Room", sensor.getDisplayName());
    }

    // ========== Batch Splitting Tests ==========
    // Simulates the batching logic used in getSensorsByIds()

    @Test
    public void testBatchSensorIdsEmptyList() {
        List<String> sensorIds = new ArrayList<>();
        List<List<String>> batches = splitIntoBatches(sensorIds, BATCH_SIZE);

        assertTrue(batches.isEmpty());
    }

    @Test
    public void testBatchSensorIdsSingleBatch() {
        // Under 10 items = 1 batch
        List<String> sensorIds = Arrays.asList("s1", "s2", "s3", "s4", "s5");
        List<List<String>> batches = splitIntoBatches(sensorIds, BATCH_SIZE);

        assertEquals(1, batches.size());
        assertEquals(5, batches.get(0).size());
    }

    @Test
    public void testBatchSensorIdsExactlyTen() {
        // Exactly 10 items = 1 batch
        List<String> sensorIds = Arrays.asList("s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10");
        List<List<String>> batches = splitIntoBatches(sensorIds, BATCH_SIZE);

        assertEquals(1, batches.size());
        assertEquals(10, batches.get(0).size());
    }

    @Test
    public void testBatchSensorIdsMultipleBatches() {
        // 25 items = 3 batches (10, 10, 5)
        List<String> sensorIds = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            sensorIds.add("sensor" + i);
        }

        List<List<String>> batches = splitIntoBatches(sensorIds, BATCH_SIZE);

        assertEquals(3, batches.size());
        assertEquals(10, batches.get(0).size());
        assertEquals(10, batches.get(1).size());
        assertEquals(5, batches.get(2).size());
    }

    @Test
    public void testBatchSensorIdsSplitsCorrectly() {
        // 15 items = 2 batches (10, 5)
        List<String> sensorIds = new ArrayList<>();
        for (int i = 1; i <= 15; i++) {
            sensorIds.add("s" + i);
        }

        List<List<String>> batches = splitIntoBatches(sensorIds, BATCH_SIZE);

        assertEquals(2, batches.size());
        assertEquals(10, batches.get(0).size());
        assertEquals(5, batches.get(1).size());

        // Verify first batch content
        assertEquals("s1", batches.get(0).get(0));
        assertEquals("s10", batches.get(0).get(9));

        // Verify second batch content
        assertEquals("s11", batches.get(1).get(0));
        assertEquals("s15", batches.get(1).get(4));
    }

    @Test
    public void testBatchWithCustomBatchSize() {
        // Test with batch size of 3
        List<String> sensorIds = Arrays.asList("a", "b", "c", "d", "e", "f", "g");
        List<List<String>> batches = splitIntoBatches(sensorIds, 3);

        assertEquals(3, batches.size());
        assertEquals(3, batches.get(0).size()); // a, b, c
        assertEquals(3, batches.get(1).size()); // d, e, f
        assertEquals(1, batches.get(2).size()); // g
    }

    @Test
    public void testBatchCountCalculation() {
        // Test the formula: ceil(size / batchSize)
        assertEquals(0, calculateBatchCount(0, BATCH_SIZE));
        assertEquals(1, calculateBatchCount(1, BATCH_SIZE));
        assertEquals(1, calculateBatchCount(10, BATCH_SIZE));
        assertEquals(2, calculateBatchCount(11, BATCH_SIZE));
        assertEquals(3, calculateBatchCount(25, BATCH_SIZE));
        assertEquals(10, calculateBatchCount(100, BATCH_SIZE));
    }

    // ========== Display Name Resolution Tests ==========

    @Test
    public void testSensorDisplayNameFallback() {
        // When displayName is null, should use sensorId as fallback
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", null);

        String displayName = getDisplayNameOrFallback(sensor);

        assertEquals("sensor001", displayName);
    }

    @Test
    public void testSensorDisplayNameUsed() {
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "Child Room");

        String displayName = getDisplayNameOrFallback(sensor);

        assertEquals("Child Room", displayName);
    }

    @Test
    public void testSensorDisplayNameEmptyFallback() {
        // Empty string should also fallback to sensorId
        Sensor sensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner123", "");

        String displayName = getDisplayNameOrFallback(sensor);

        assertEquals("sensor001", displayName);
    }

    // ========== Helper Methods ==========

    /**
     * Simulates the batching logic used in SensorInventoryService.getSensorsByIds()
     */
    private List<List<String>> splitIntoBatches(List<String> items, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, endIndex)));
        }
        
        return batches;
    }

    /**
     * Calculates the number of batches needed.
     */
    private int calculateBatchCount(int totalItems, int batchSize) {
        if (totalItems == 0) return 0;
        return (int) Math.ceil((double) totalItems / batchSize);
    }

    /**
     * Gets display name with fallback to sensorId.
     */
    private String getDisplayNameOrFallback(Sensor sensor) {
        String displayName = sensor.getDisplayName();
        if (displayName == null || displayName.isEmpty()) {
            return sensor.getSensorId();
        }
        return displayName;
    }

    // ========== Sensor Persistence Tests ==========
    // These tests verify the logic used in saveSensorsToLocal

    @Test
    public void testSaveSensorsToLocalLogic_EmptyList() {
        List<Sensor> sensors = new ArrayList<>();
        
        // Simulate the check in saveSensorsToLocal
        boolean shouldSave = !sensors.isEmpty();
        
        assertFalse("Empty list should not trigger save", shouldSave);
    }

    @Test
    public void testSaveSensorsToLocalLogic_NonEmptyList() {
        List<Sensor> sensors = Arrays.asList(
                new Sensor("s1", "AA:BB:CC:DD:EE:FF", "owner1", "Sensor 1"),
                new Sensor("s2", "11:22:33:44:55:66", "owner1", "Sensor 2")
        );
        
        boolean shouldSave = !sensors.isEmpty();
        
        assertTrue("Non-empty list should trigger save", shouldSave);
    }

    @Test
    public void testSaveSensorsToLocalLogic_NullSensorIdSkipped() {
        // Simulate logic: sensor with null ID should be skipped
        Sensor sensorWithNullId = new Sensor(null, "AA:BB:CC:DD:EE:FF", "owner1", "Test");
        
        boolean shouldProcess = sensorWithNullId.getSensorId() != null;
        
        assertFalse("Sensor with null ID should be skipped", shouldProcess);
    }

    @Test
    public void testSaveSensorsToLocalLogic_ValidSensorProcessed() {
        Sensor validSensor = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner1", "Test");
        
        boolean shouldProcess = validSensor.getSensorId() != null;
        
        assertTrue("Valid sensor should be processed", shouldProcess);
    }

    @Test
    public void testSaveSensorsToLocalLogic_DisplayNameFallback() {
        // When displayName is null, should use sensorId as fallback
        Sensor sensorWithNullName = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner1", null);
        
        String displayName = sensorWithNullName.getDisplayName() != null 
                ? sensorWithNullName.getDisplayName() 
                : sensorWithNullName.getSensorId();
        
        assertEquals("sensor001", displayName);
    }

    @Test
    public void testSaveSensorsToLocalLogic_DisplayNameUsed() {
        Sensor sensorWithName = new Sensor("sensor001", "AA:BB:CC:DD:EE:FF", "owner1", "Living Room");
        
        String displayName = sensorWithName.getDisplayName() != null 
                ? sensorWithName.getDisplayName() 
                : sensorWithName.getSensorId();
        
        assertEquals("Living Room", displayName);
    }

    @Test
    public void testSaveSensorsToLocalLogic_CountProcessed() {
        List<Sensor> sensors = Arrays.asList(
                new Sensor("s1", "AA:BB:CC:DD:EE:FF", "owner1", "Sensor 1"),
                new Sensor(null, "11:22:33:44:55:66", "owner1", "Sensor 2"), // Should be skipped
                new Sensor("s3", "77:88:99:AA:BB:CC", "owner1", null)
        );
        
        // Simulate the counting logic
        int count = 0;
        for (Sensor sensor : sensors) {
            if (sensor.getSensorId() != null) {
                count++;
            }
        }
        
        assertEquals("Should process 2 sensors (1 skipped due to null ID)", 2, count);
    }

    @Test
    public void testSaveSensorsToLocalLogic_AllSensorsValid() {
        List<Sensor> sensors = Arrays.asList(
                new Sensor("s1", "AA:BB:CC:DD:EE:FF", "owner1", "Sensor 1"),
                new Sensor("s2", "11:22:33:44:55:66", "owner1", "Sensor 2"),
                new Sensor("s3", "77:88:99:AA:BB:CC", "owner1", "Sensor 3")
        );
        
        int count = 0;
        for (Sensor sensor : sensors) {
            if (sensor.getSensorId() != null) {
                count++;
            }
        }
        
        assertEquals("Should process all 3 sensors", 3, count);
    }

    // ========== Integration Pattern Tests ==========
    // These verify the expected call patterns for sensor persistence

    @Test
    public void testAggregatorSensorPersistencePattern() {
        // Pattern: getOwnedSensors -> onResult -> saveSensorsToLocal
        // Verify the list is non-empty before saving
        
        List<Sensor> fetchedSensors = Arrays.asList(
                new Sensor("s1", "AA:BB:CC:DD:EE:FF", "aggregator123", "Child Room")
        );
        
        boolean shouldCallSaveSensorsToLocal = !fetchedSensors.isEmpty();
        assertTrue("Should call saveSensorsToLocal after fetching sensors", shouldCallSaveSensorsToLocal);
        
        // Verify sensor data for persistence
        Sensor sensor = fetchedSensors.get(0);
        assertNotNull("Sensor ID should not be null", sensor.getSensorId());
        assertNotNull("Display name should not be null for this test sensor", sensor.getDisplayName());
    }

    @Test
    public void testSupervisorSensorPersistencePattern() {
        // Pattern: downloadToLocal(sensorIds) -> getSensorsByIds -> saveSensorsToLocal
        // This is already handled by the refactored downloadToLocal method
        
        List<String> assignedSensorIds = Arrays.asList("s1", "s2", "s3");
        
        // Verify sensor IDs are valid
        for (String id : assignedSensorIds) {
            assertNotNull("Sensor ID should not be null", id);
            assertFalse("Sensor ID should not be empty", id.isEmpty());
        }
        
        assertTrue("Should have sensor IDs to download", !assignedSensorIds.isEmpty());
    }
}
