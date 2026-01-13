package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for sensor-based query optimization.
 * 
 * Tests whereIn batching, sensor list handling, and query construction logic.
 */
public class SensorQueryOptimizationTest {

    private static final int WHEREIN_LIMIT = 10; // Firestore whereIn limit

    // ========== Batch Calculation Tests ==========

    @Test
    public void batchSensorIds_emptyList_returnsEmptyBatches() {
        List<List<String>> batches = batchSensorIds(Collections.emptyList(), WHEREIN_LIMIT);
        assertTrue(batches.isEmpty());
    }

    @Test
    public void batchSensorIds_singleSensor_returnsSingleBatch() {
        List<String> sensors = Arrays.asList("sensor001");
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        assertEquals(1, batches.size());
        assertEquals(1, batches.get(0).size());
        assertEquals("sensor001", batches.get(0).get(0));
    }

    @Test
    public void batchSensorIds_atLimit_returnsSingleBatch() {
        List<String> sensors = createSensorList(10);
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        assertEquals(1, batches.size());
        assertEquals(10, batches.get(0).size());
    }

    @Test
    public void batchSensorIds_overLimit_returnsTwoBatches() {
        List<String> sensors = createSensorList(11);
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        assertEquals(2, batches.size());
        assertEquals(10, batches.get(0).size());
        assertEquals(1, batches.get(1).size());
    }

    @Test
    public void batchSensorIds_largeList_returnsCorrectBatchCount() {
        List<String> sensors = createSensorList(35);
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        // 35 sensors / 10 per batch = 4 batches (10 + 10 + 10 + 5)
        assertEquals(4, batches.size());
        assertEquals(10, batches.get(0).size());
        assertEquals(10, batches.get(1).size());
        assertEquals(10, batches.get(2).size());
        assertEquals(5, batches.get(3).size());
    }

    @Test
    public void batchSensorIds_exactMultiple_returnsExactBatches() {
        List<String> sensors = createSensorList(30);
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        assertEquals(3, batches.size());
        for (List<String> batch : batches) {
            assertEquals(10, batch.size());
        }
    }

    // ========== Query Strategy Selection Tests ==========

    @Test
    public void shouldUseCompoundListener_underLimit_returnsTrue() {
        assertTrue(shouldUseCompoundListener(5));
        assertTrue(shouldUseCompoundListener(1));
        assertTrue(shouldUseCompoundListener(10));
    }

    @Test
    public void shouldUseCompoundListener_overLimit_returnsFalse() {
        assertFalse(shouldUseCompoundListener(11));
        assertFalse(shouldUseCompoundListener(50));
        assertFalse(shouldUseCompoundListener(100));
    }

    @Test
    public void shouldUseCompoundListener_atLimit_returnsTrue() {
        // Exactly 10 should still use compound listener
        assertTrue(shouldUseCompoundListener(10));
    }

    @Test
    public void shouldUseCompoundListener_zero_returnsFalse() {
        // No sensors means no listener needed
        assertFalse(shouldUseCompoundListener(0));
    }

    // ========== Sensor ID Validation Tests ==========

    @Test
    public void isValidSensorId_validId_returnsTrue() {
        assertTrue(isValidSensorId("sensor001"));
        assertTrue(isValidSensorId("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
        assertTrue(isValidSensorId("abc123"));
    }

    @Test
    public void isValidSensorId_null_returnsFalse() {
        assertFalse(isValidSensorId(null));
    }

    @Test
    public void isValidSensorId_empty_returnsFalse() {
        assertFalse(isValidSensorId(""));
    }

    @Test
    public void isValidSensorId_unknown_returnsFalse() {
        // "unknown" is used as a fallback, should not be treated as valid
        assertFalse(isValidSensorId("unknown"));
    }

    // ========== Query Count Optimization Tests ==========

    @Test
    public void calculateQueryCount_underLimit_returnsOne() {
        assertEquals(1, calculateQueryCount(5, WHEREIN_LIMIT));
    }

    @Test
    public void calculateQueryCount_atLimit_returnsOne() {
        assertEquals(1, calculateQueryCount(10, WHEREIN_LIMIT));
    }

    @Test
    public void calculateQueryCount_overLimit_returnsMultiple() {
        assertEquals(2, calculateQueryCount(11, WHEREIN_LIMIT));
        assertEquals(2, calculateQueryCount(20, WHEREIN_LIMIT));
        assertEquals(3, calculateQueryCount(21, WHEREIN_LIMIT));
    }

    @Test
    public void calculateQueryCount_empty_returnsZero() {
        assertEquals(0, calculateQueryCount(0, WHEREIN_LIMIT));
    }

    // ========== Network Efficiency Tests ==========

    @Test
    public void calculateNetworkCallsSaved_multipleSensors_showsSavings() {
        // Before optimization: N queries for N sensors
        // After optimization: ceil(N/10) queries
        
        int sensorCount = 25;
        int beforeOptimization = sensorCount; // 25 queries
        int afterOptimization = calculateQueryCount(sensorCount, WHEREIN_LIMIT); // 3 queries
        
        int saved = beforeOptimization - afterOptimization;
        assertEquals(22, saved); // 25 - 3 = 22 queries saved
    }

    @Test
    public void calculateNetworkCallsSaved_fewSensors_minimalSavings() {
        int sensorCount = 3;
        int beforeOptimization = sensorCount;
        int afterOptimization = calculateQueryCount(sensorCount, WHEREIN_LIMIT);
        
        int saved = beforeOptimization - afterOptimization;
        assertEquals(2, saved); // 3 - 1 = 2 queries saved
    }

    // ========== Batch Integrity Tests ==========

    @Test
    public void batchSensorIds_preservesOrder() {
        List<String> sensors = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> batches = batchSensorIds(sensors, 3);
        
        assertEquals(2, batches.size());
        assertEquals(Arrays.asList("a", "b", "c"), batches.get(0));
        assertEquals(Arrays.asList("d", "e"), batches.get(1));
    }

    @Test
    public void batchSensorIds_doesNotModifyOriginal() {
        List<String> original = new ArrayList<>(Arrays.asList("a", "b", "c"));
        List<List<String>> batches = batchSensorIds(original, 2);
        
        // Original should be unchanged
        assertEquals(3, original.size());
        // Batches should be independent
        batches.get(0).clear();
        assertEquals(3, original.size());
    }

    @Test
    public void batchSensorIds_noDuplicatesAcrossBatches() {
        List<String> sensors = createSensorList(25);
        List<List<String>> batches = batchSensorIds(sensors, WHEREIN_LIMIT);
        
        // Flatten and check for duplicates
        List<String> allSensors = new ArrayList<>();
        for (List<String> batch : batches) {
            allSensors.addAll(batch);
        }
        
        assertEquals(sensors.size(), allSensors.size());
        assertEquals(sensors, allSensors); // Same content, same order
    }

    // ========== Edge Cases ==========

    @Test
    public void batchSensorIds_customBatchSize() {
        List<String> sensors = createSensorList(5);
        
        // Test with batch size of 2
        List<List<String>> batches = batchSensorIds(sensors, 2);
        assertEquals(3, batches.size());
        assertEquals(2, batches.get(0).size());
        assertEquals(2, batches.get(1).size());
        assertEquals(1, batches.get(2).size());
    }

    @Test
    public void batchSensorIds_batchSizeOne_returnsIndividualBatches() {
        List<String> sensors = createSensorList(3);
        List<List<String>> batches = batchSensorIds(sensors, 1);
        
        assertEquals(3, batches.size());
        for (List<String> batch : batches) {
            assertEquals(1, batch.size());
        }
    }

    // ========== Helper Methods (simulating FirestoreSyncService logic) ==========

    private List<List<String>> batchSensorIds(List<String> sensorIds, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < sensorIds.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, sensorIds.size());
            batches.add(new ArrayList<>(sensorIds.subList(i, endIndex)));
        }
        return batches;
    }

    private boolean shouldUseCompoundListener(int sensorCount) {
        return sensorCount > 0 && sensorCount <= WHEREIN_LIMIT;
    }

    private boolean isValidSensorId(String sensorId) {
        return sensorId != null && !sensorId.isEmpty() && !"unknown".equals(sensorId);
    }

    private int calculateQueryCount(int sensorCount, int batchSize) {
        if (sensorCount == 0) return 0;
        return (int) Math.ceil((double) sensorCount / batchSize);
    }

    private List<String> createSensorList(int count) {
        List<String> sensors = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            sensors.add("sensor" + String.format("%03d", i + 1));
        }
        return sensors;
    }
}
