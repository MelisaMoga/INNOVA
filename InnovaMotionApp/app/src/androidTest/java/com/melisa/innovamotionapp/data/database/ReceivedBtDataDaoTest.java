package com.melisa.innovamotionapp.data.database;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented tests for ReceivedBtDataDao.
 * 
 * Tests sensor-specific queries using an in-memory Room database.
 * 
 * Tests cover:
 * - Insert with sensorId
 * - getLatestForSensor
 * - getAllForSensor
 * - getDistinctSensorIds
 * - getLatestForEachSensor
 * - getLatestForOwnerAndSensor
 * - getDistinctSensorIdsForOwner
 * - Multiple sensors differentiation
 */
@RunWith(AndroidJUnit4.class)
public class ReceivedBtDataDaoTest {

    private InnovaDatabase database;
    private ReceivedBtDataDao dao;

    private static final String DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String OWNER_1 = "owner1";
    private static final String OWNER_2 = "owner2";

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, InnovaDatabase.class)
                .allowMainThreadQueries() // For testing only
                .build();
        dao = database.receivedBtDataDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ========== Helper Methods ==========

    private ReceivedBtDataEntity createEntity(String sensorId, long timestamp, String msg, String owner) {
        return new ReceivedBtDataEntity(DEVICE_ADDRESS, timestamp, msg, owner, sensorId);
    }

    private ReceivedBtDataEntity createEntity(String sensorId, long timestamp, String msg) {
        return createEntity(sensorId, timestamp, msg, OWNER_1);
    }

    /**
     * Helper to get value from LiveData synchronously for testing.
     */
    private <T> T getLiveDataValue(LiveData<T> liveData) throws InterruptedException {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Must observe on main thread
        liveData.observeForever(value -> {
            result.set(value);
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        return result.get();
    }

    // ========== Insert Tests ==========

    @Test
    public void testInsertWithSensorId() {
        ReceivedBtDataEntity entity = createEntity("sensor001", 1000L, "0xAB3311");
        
        long id = dao.insert(entity);
        
        assertTrue("Insert should return positive ID", id > 0);
    }

    @Test
    public void testInsertMultipleSensors() {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor002", 1000L, "0xEF0112"));
        dao.insert(createEntity("sensor003", 1000L, "0xBA3311"));
        
        int count = dao.countAll();
        
        assertEquals(3, count);
    }

    @Test
    public void testInsertDuplicateIsIgnored() {
        ReceivedBtDataEntity e1 = createEntity("sensor001", 1000L, "0xAB3311");
        ReceivedBtDataEntity e2 = createEntity("sensor001", 1000L, "0xAB3311"); // Same data
        
        long id1 = dao.insert(e1);
        long id2 = dao.insert(e2);
        
        assertTrue("First insert should succeed", id1 > 0);
        assertEquals("Duplicate insert should be ignored (return -1)", -1, id2);
        assertEquals("Should only have 1 row", 1, dao.countAll());
    }

    @Test
    public void testInsertSameSensorDifferentTimestamp() {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xBA3311"));
        
        assertEquals("Should have 2 rows for same sensor at different times", 2, dao.countAll());
    }

    // ========== getLatestForSensor Tests ==========

    @Test
    public void testGetLatestForSensor() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112")); // Latest
        dao.insert(createEntity("sensor001", 1500L, "0xBA3311"));
        
        ReceivedBtDataEntity latest = getLiveDataValue(dao.getLatestForSensor("sensor001"));
        
        assertNotNull(latest);
        assertEquals(2000L, latest.getTimestamp());
        assertEquals("0xEF0112", latest.getReceivedMsg());
    }

    @Test
    public void testGetLatestForSensorReturnsNullForUnknown() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        
        ReceivedBtDataEntity latest = getLiveDataValue(dao.getLatestForSensor("unknown"));
        
        assertNull(latest);
    }

    @Test
    public void testGetLatestForSensorIgnoresOtherSensors() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor002", 3000L, "0xEF0112")); // Newer but different sensor
        
        ReceivedBtDataEntity latest = getLiveDataValue(dao.getLatestForSensor("sensor001"));
        
        assertNotNull(latest);
        assertEquals("sensor001", latest.getSensorId());
        assertEquals(1000L, latest.getTimestamp());
    }

    // ========== getAllForSensor Tests ==========

    @Test
    public void testGetAllForSensor() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112"));
        dao.insert(createEntity("sensor001", 1500L, "0xBA3311"));
        dao.insert(createEntity("sensor002", 1000L, "0xAB3311")); // Different sensor
        
        List<ReceivedBtDataEntity> all = getLiveDataValue(dao.getAllForSensor("sensor001"));
        
        assertNotNull(all);
        assertEquals(3, all.size());
        // Should be ordered by timestamp ASC
        assertEquals(1000L, all.get(0).getTimestamp());
        assertEquals(1500L, all.get(1).getTimestamp());
        assertEquals(2000L, all.get(2).getTimestamp());
    }

    @Test
    public void testGetAllForSensorReturnsEmptyForUnknown() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        
        List<ReceivedBtDataEntity> all = getLiveDataValue(dao.getAllForSensor("unknown"));
        
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    // ========== getDistinctSensorIds Tests ==========

    @Test
    public void testGetDistinctSensorIds() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112")); // Same sensor, different time
        dao.insert(createEntity("sensor002", 1000L, "0xBA3311"));
        dao.insert(createEntity("sensor003", 1000L, "0xAB3311"));
        
        List<String> sensorIds = getLiveDataValue(dao.getDistinctSensorIds());
        
        assertNotNull(sensorIds);
        assertEquals(3, sensorIds.size());
        assertTrue(sensorIds.contains("sensor001"));
        assertTrue(sensorIds.contains("sensor002"));
        assertTrue(sensorIds.contains("sensor003"));
    }

    @Test
    public void testGetDistinctSensorIdsReturnsEmptyWhenNoData() throws InterruptedException {
        List<String> sensorIds = getLiveDataValue(dao.getDistinctSensorIds());
        
        assertNotNull(sensorIds);
        assertTrue(sensorIds.isEmpty());
    }

    // ========== getLatestForEachSensor Tests ==========

    @Test
    public void testGetLatestForEachSensor() throws InterruptedException {
        // Sensor 1: readings at 1000, 2000, 1500 -> latest is 2000
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112"));
        dao.insert(createEntity("sensor001", 1500L, "0xBA3311"));
        
        // Sensor 2: readings at 500, 1500 -> latest is 1500
        dao.insert(createEntity("sensor002", 500L, "0xAB3311"));
        dao.insert(createEntity("sensor002", 1500L, "0xEF0112"));
        
        // Sensor 3: single reading at 3000
        dao.insert(createEntity("sensor003", 3000L, "0xBA3311"));
        
        List<ReceivedBtDataEntity> latestList = getLiveDataValue(dao.getLatestForEachSensor());
        
        assertNotNull(latestList);
        assertEquals(3, latestList.size());
        
        // Find each sensor's latest
        ReceivedBtDataEntity s1Latest = findBySensorId(latestList, "sensor001");
        ReceivedBtDataEntity s2Latest = findBySensorId(latestList, "sensor002");
        ReceivedBtDataEntity s3Latest = findBySensorId(latestList, "sensor003");
        
        assertNotNull(s1Latest);
        assertNotNull(s2Latest);
        assertNotNull(s3Latest);
        
        assertEquals(2000L, s1Latest.getTimestamp());
        assertEquals(1500L, s2Latest.getTimestamp());
        assertEquals(3000L, s3Latest.getTimestamp());
    }

    private ReceivedBtDataEntity findBySensorId(List<ReceivedBtDataEntity> list, String sensorId) {
        for (ReceivedBtDataEntity e : list) {
            if (sensorId.equals(e.getSensorId())) {
                return e;
            }
        }
        return null;
    }

    // ========== getLatestForOwnerAndSensor Tests ==========

    @Test
    public void testGetLatestForOwnerAndSensor() throws InterruptedException {
        // Owner 1, Sensor 1
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311", OWNER_1));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112", OWNER_1));
        
        // Owner 2, same Sensor 1 (different owner)
        dao.insert(createEntity("sensor001", 3000L, "0xBA3311", OWNER_2));
        
        ReceivedBtDataEntity latest = getLiveDataValue(
                dao.getLatestForOwnerAndSensor(OWNER_1, "sensor001"));
        
        assertNotNull(latest);
        assertEquals(OWNER_1, latest.getOwnerUserId());
        assertEquals("sensor001", latest.getSensorId());
        assertEquals(2000L, latest.getTimestamp()); // Not 3000 which belongs to owner2
    }

    @Test
    public void testGetLatestForOwnerAndSensorReturnsNullForMismatch() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311", OWNER_1));
        
        // Query with wrong owner
        ReceivedBtDataEntity result = getLiveDataValue(
                dao.getLatestForOwnerAndSensor(OWNER_2, "sensor001"));
        assertNull(result);
        
        // Query with wrong sensor
        ReceivedBtDataEntity result2 = getLiveDataValue(
                dao.getLatestForOwnerAndSensor(OWNER_1, "sensor999"));
        assertNull(result2);
    }

    // ========== getDistinctSensorIdsForOwner Tests ==========

    @Test
    public void testGetDistinctSensorIdsForOwner() throws InterruptedException {
        // Owner 1 has sensor001, sensor002
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311", OWNER_1));
        dao.insert(createEntity("sensor002", 1000L, "0xEF0112", OWNER_1));
        
        // Owner 2 has sensor003
        dao.insert(createEntity("sensor003", 1000L, "0xBA3311", OWNER_2));
        
        List<String> owner1Sensors = getLiveDataValue(dao.getDistinctSensorIdsForOwner(OWNER_1));
        List<String> owner2Sensors = getLiveDataValue(dao.getDistinctSensorIdsForOwner(OWNER_2));
        
        assertNotNull(owner1Sensors);
        assertNotNull(owner2Sensors);
        
        assertEquals(2, owner1Sensors.size());
        assertEquals(1, owner2Sensors.size());
        
        assertTrue(owner1Sensors.contains("sensor001"));
        assertTrue(owner1Sensors.contains("sensor002"));
        assertFalse(owner1Sensors.contains("sensor003"));
        
        assertTrue(owner2Sensors.contains("sensor003"));
    }

    // ========== getLatestForEachSensorByOwner Tests ==========

    @Test
    public void testGetLatestForEachSensorByOwner() throws InterruptedException {
        // Owner 1: sensor001 (latest at 2000), sensor002 (latest at 1500)
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311", OWNER_1));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112", OWNER_1));
        dao.insert(createEntity("sensor002", 1500L, "0xBA3311", OWNER_1));
        
        // Owner 2: sensor001 at 3000 (should not appear for owner1)
        dao.insert(createEntity("sensor001", 3000L, "0xAB3311", OWNER_2));
        
        List<ReceivedBtDataEntity> latestList = getLiveDataValue(
                dao.getLatestForEachSensorByOwner(OWNER_1));
        
        assertNotNull(latestList);
        assertEquals(2, latestList.size());
        
        ReceivedBtDataEntity s1 = findBySensorId(latestList, "sensor001");
        ReceivedBtDataEntity s2 = findBySensorId(latestList, "sensor002");
        
        assertNotNull(s1);
        assertNotNull(s2);
        
        assertEquals(2000L, s1.getTimestamp()); // Not 3000
        assertEquals(1500L, s2.getTimestamp());
        assertEquals(OWNER_1, s1.getOwnerUserId());
        assertEquals(OWNER_1, s2.getOwnerUserId());
    }

    // ========== getAllForOwnerAndSensor Tests ==========

    @Test
    public void testGetAllForOwnerAndSensor() throws InterruptedException {
        // Owner 1, Sensor 1: 3 readings
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311", OWNER_1));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112", OWNER_1));
        dao.insert(createEntity("sensor001", 1500L, "0xBA3311", OWNER_1));
        
        // Owner 1, Sensor 2: should not appear
        dao.insert(createEntity("sensor002", 1000L, "0xAB3311", OWNER_1));
        
        // Owner 2, Sensor 1: should not appear
        dao.insert(createEntity("sensor001", 3000L, "0xAB3311", OWNER_2));
        
        List<ReceivedBtDataEntity> list = getLiveDataValue(
                dao.getAllForOwnerAndSensor(OWNER_1, "sensor001"));
        
        assertNotNull(list);
        assertEquals(3, list.size());
        
        // Should be ordered by timestamp ASC
        assertEquals(1000L, list.get(0).getTimestamp());
        assertEquals(1500L, list.get(1).getTimestamp());
        assertEquals(2000L, list.get(2).getTimestamp());
        
        // All should be owner1 and sensor001
        for (ReceivedBtDataEntity e : list) {
            assertEquals(OWNER_1, e.getOwnerUserId());
            assertEquals("sensor001", e.getSensorId());
        }
    }

    // ========== countBySensor Tests ==========

    @Test
    public void testCountBySensor() {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor001", 2000L, "0xEF0112"));
        dao.insert(createEntity("sensor002", 1000L, "0xBA3311"));
        dao.insert(createEntity("sensor003", 1000L, "0xAB3311"));
        dao.insert(createEntity("sensor003", 2000L, "0xEF0112"));
        dao.insert(createEntity("sensor003", 3000L, "0xBA3311"));
        
        List<ReceivedBtDataDao.SensorCount> counts = dao.countBySensor();
        
        assertNotNull(counts);
        assertEquals(3, counts.size());
        
        // Find counts per sensor
        int s1Count = 0, s2Count = 0, s3Count = 0;
        for (ReceivedBtDataDao.SensorCount sc : counts) {
            if ("sensor001".equals(sc.sensorId)) s1Count = sc.count;
            else if ("sensor002".equals(sc.sensorId)) s2Count = sc.count;
            else if ("sensor003".equals(sc.sensorId)) s3Count = sc.count;
        }
        
        assertEquals(2, s1Count);
        assertEquals(1, s2Count);
        assertEquals(3, s3Count);
    }

    // ========== UUID SensorId Format Tests ==========

    @Test
    public void testUuidSensorIdFormat() throws InterruptedException {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        dao.insert(createEntity(uuid, 1000L, "0xAB3311"));
        
        ReceivedBtDataEntity latest = getLiveDataValue(dao.getLatestForSensor(uuid));
        
        assertNotNull(latest);
        assertEquals(uuid, latest.getSensorId());
    }

    @Test
    public void testMixedSensorIdFormats() throws InterruptedException {
        dao.insert(createEntity("sensor001", 1000L, "0xAB3311"));
        dao.insert(createEntity("5d6d75ee-b6c8-42d4-a233-b13d137fea38", 1000L, "0xEF0112"));
        dao.insert(createEntity("12345", 1000L, "0xBA3311"));
        
        List<String> sensorIds = getLiveDataValue(dao.getDistinctSensorIds());
        
        assertEquals(3, sensorIds.size());
        assertTrue(sensorIds.contains("sensor001"));
        assertTrue(sensorIds.contains("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
        assertTrue(sensorIds.contains("12345"));
    }
}
