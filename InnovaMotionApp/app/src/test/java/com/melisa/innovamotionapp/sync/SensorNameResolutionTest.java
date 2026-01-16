package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.models.Sensor;
import com.melisa.innovamotionapp.utils.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for sensor name resolution pipeline.
 * 
 * Tests cover:
 * - getSensorsByIds resolves names
 * - downloadToLocal caches names
 * - Name resolution fallback to sensorId
 * - Name lookup by batch (respecting whereIn limit)
 */
public class SensorNameResolutionTest {

    private static final int WHEREIN_LIMIT = Constants.FIRESTORE_WHERE_IN_LIMIT; // 10
    
    private MockSensorRepository sensorRepository;
    private MockLocalCache localCache;

    @Before
    public void setUp() {
        sensorRepository = new MockSensorRepository();
        localCache = new MockLocalCache();
        
        // Pre-populate repository with test sensors
        sensorRepository.addSensor(new Sensor("sensor001", "AA:BB:CC:DD:EE:01", "owner1", "Child 1"));
        sensorRepository.addSensor(new Sensor("sensor002", "AA:BB:CC:DD:EE:02", "owner1", "Child 2"));
        sensorRepository.addSensor(new Sensor("sensor003", "AA:BB:CC:DD:EE:03", "owner1", "Living Room"));
        sensorRepository.addSensor(new Sensor("sensor004", "AA:BB:CC:DD:EE:04", "owner1", null)); // No name
    }

    // ========== getSensorsByIds Tests ==========

    @Test
    public void testGetSensorsByIdsResolvesNames() {
        List<String> sensorIds = Arrays.asList("sensor001", "sensor002", "sensor003");

        List<Sensor> sensors = sensorRepository.getSensorsByIds(sensorIds);

        assertEquals(3, sensors.size());
        
        // Verify names are resolved
        Map<String, String> nameMap = new HashMap<>();
        for (Sensor s : sensors) {
            nameMap.put(s.getSensorId(), s.getDisplayName());
        }
        
        assertEquals("Child 1", nameMap.get("sensor001"));
        assertEquals("Child 2", nameMap.get("sensor002"));
        assertEquals("Living Room", nameMap.get("sensor003"));
    }

    @Test
    public void testGetSensorsByIdsEmptyList() {
        List<String> sensorIds = new ArrayList<>();

        List<Sensor> sensors = sensorRepository.getSensorsByIds(sensorIds);

        assertTrue(sensors.isEmpty());
    }

    @Test
    public void testGetSensorsByIdsPartialMatch() {
        // Some IDs exist, some don't
        List<String> sensorIds = Arrays.asList("sensor001", "nonexistent", "sensor003");

        List<Sensor> sensors = sensorRepository.getSensorsByIds(sensorIds);

        assertEquals(2, sensors.size());
        
        List<String> foundIds = new ArrayList<>();
        for (Sensor s : sensors) {
            foundIds.add(s.getSensorId());
        }
        
        assertTrue(foundIds.contains("sensor001"));
        assertTrue(foundIds.contains("sensor003"));
        assertFalse(foundIds.contains("nonexistent"));
    }

    // ========== downloadToLocal Tests ==========

    @Test
    public void testDownloadToLocalCachesNames() {
        List<String> sensorIds = Arrays.asList("sensor001", "sensor002");

        // Simulate downloadToLocal
        List<Sensor> sensors = sensorRepository.getSensorsByIds(sensorIds);
        for (Sensor s : sensors) {
            if (s.getSensorId() != null && s.getDisplayName() != null) {
                localCache.put(s.getSensorId(), s.getDisplayName());
            }
        }

        // Verify local cache has the names
        assertEquals("Child 1", localCache.get("sensor001"));
        assertEquals("Child 2", localCache.get("sensor002"));
        assertNull(localCache.get("sensor003")); // Not downloaded
    }

    @Test
    public void testDownloadToLocalWithNullNames() {
        List<String> sensorIds = Arrays.asList("sensor004"); // Has null displayName

        List<Sensor> sensors = sensorRepository.getSensorsByIds(sensorIds);
        for (Sensor s : sensors) {
            if (s.getSensorId() != null && s.getDisplayName() != null) {
                localCache.put(s.getSensorId(), s.getDisplayName());
            }
        }

        // Should not cache sensors with null names
        assertFalse(localCache.contains("sensor004"));
    }

    // ========== Name Resolution Fallback Tests ==========

    @Test
    public void testNameResolutionFallbackToSensorId() {
        // When name is not available, fallback to sensorId
        List<String> sensorIds = Arrays.asList("sensor001", "sensor004", "unknown");

        List<String> resolvedNames = new ArrayList<>();
        for (String id : sensorIds) {
            String name = resolveDisplayName(id);
            resolvedNames.add(name);
        }

        assertEquals("Child 1", resolvedNames.get(0));       // Has name
        assertEquals("sensor004", resolvedNames.get(1));     // Name is null, fallback
        assertEquals("unknown", resolvedNames.get(2));       // Not found, fallback
    }

    @Test
    public void testNameResolutionFromCache() {
        // First cache the names
        localCache.put("sensor001", "Cached Child 1");
        localCache.put("sensor002", "Cached Child 2");

        // Resolution should use cache first
        String name1 = localCache.getOrDefault("sensor001", "sensor001");
        String name2 = localCache.getOrDefault("sensor002", "sensor002");
        String name3 = localCache.getOrDefault("sensor999", "sensor999");

        assertEquals("Cached Child 1", name1);
        assertEquals("Cached Child 2", name2);
        assertEquals("sensor999", name3); // Fallback
    }

    // ========== Batch Lookup Tests ==========

    @Test
    public void testNameLookupByBatchUnderLimit() {
        // Under 10 sensors = single batch
        List<String> sensorIds = Arrays.asList("s1", "s2", "s3", "s4", "s5");
        List<List<String>> batches = splitIntoBatches(sensorIds, WHEREIN_LIMIT);

        assertEquals(1, batches.size());
        assertEquals(5, batches.get(0).size());
    }

    @Test
    public void testNameLookupByBatchExactlyAtLimit() {
        // Exactly 10 sensors = single batch
        List<String> sensorIds = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            sensorIds.add("sensor" + i);
        }

        List<List<String>> batches = splitIntoBatches(sensorIds, WHEREIN_LIMIT);

        assertEquals(1, batches.size());
        assertEquals(10, batches.get(0).size());
    }

    @Test
    public void testNameLookupByBatchOverLimit() {
        // 25 sensors = 3 batches (10, 10, 5)
        List<String> sensorIds = new ArrayList<>();
        for (int i = 1; i <= 25; i++) {
            sensorIds.add("sensor" + i);
        }

        List<List<String>> batches = splitIntoBatches(sensorIds, WHEREIN_LIMIT);

        assertEquals(3, batches.size());
        assertEquals(10, batches.get(0).size());
        assertEquals(10, batches.get(1).size());
        assertEquals(5, batches.get(2).size());
    }

    @Test
    public void testNameLookupByBatchPreservesOrder() {
        List<String> sensorIds = Arrays.asList("a", "b", "c", "d", "e");
        List<List<String>> batches = splitIntoBatches(sensorIds, 3);

        // First batch: a, b, c
        assertEquals(Arrays.asList("a", "b", "c"), batches.get(0));
        // Second batch: d, e
        assertEquals(Arrays.asList("d", "e"), batches.get(1));
    }

    // ========== Integration Scenario Tests ==========

    @Test
    public void testFullNameResolutionPipeline() {
        // Simulate the full pipeline:
        // 1. Supervisor gets assigned sensor IDs
        // 2. Downloads sensor info including names
        // 3. Caches locally
        // 4. Resolves names when displaying data

        List<String> assignedSensorIds = Arrays.asList("sensor001", "sensor002", "sensor003");

        // Step 1 & 2: Download sensors
        List<Sensor> sensors = sensorRepository.getSensorsByIds(assignedSensorIds);
        assertEquals(3, sensors.size());

        // Step 3: Cache locally
        for (Sensor s : sensors) {
            localCache.put(s.getSensorId(), s.getDisplayName());
        }

        // Step 4: Resolve names for display
        List<String> displayNames = new ArrayList<>();
        for (String id : assignedSensorIds) {
            displayNames.add(localCache.getOrDefault(id, id));
        }

        assertEquals(Arrays.asList("Child 1", "Child 2", "Living Room"), displayNames);
    }

    // ========== Helper Methods ==========

    private String resolveDisplayName(String sensorId) {
        // First check cache
        if (localCache.contains(sensorId)) {
            return localCache.get(sensorId);
        }

        // Then check repository
        Sensor sensor = sensorRepository.getSensorById(sensorId);
        if (sensor != null && sensor.getDisplayName() != null) {
            return sensor.getDisplayName();
        }

        // Fallback to sensorId
        return sensorId;
    }

    private List<List<String>> splitIntoBatches(List<String> items, int batchSize) {
        List<List<String>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, endIndex)));
        }
        return batches;
    }

    // ========== Mock Classes ==========

    /**
     * Mock sensor repository (simulates Firestore sensors collection).
     */
    static class MockSensorRepository {
        private final Map<String, Sensor> sensors = new HashMap<>();

        public void addSensor(Sensor sensor) {
            sensors.put(sensor.getSensorId(), sensor);
        }

        public Sensor getSensorById(String sensorId) {
            return sensors.get(sensorId);
        }

        public List<Sensor> getSensorsByIds(List<String> sensorIds) {
            List<Sensor> result = new ArrayList<>();
            for (String id : sensorIds) {
                Sensor sensor = sensors.get(id);
                if (sensor != null) {
                    result.add(sensor);
                }
            }
            return result;
        }
    }

    /**
     * Mock local cache (simulates Room database MonitoredPersonDao).
     */
    static class MockLocalCache {
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
    }
}
