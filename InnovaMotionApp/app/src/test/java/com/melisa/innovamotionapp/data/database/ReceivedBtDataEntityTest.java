package com.melisa.innovamotionapp.data.database;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for ReceivedBtDataEntity.
 * 
 * Tests cover:
 * - Constructor field assignment
 * - All getters work correctly
 * - Various sensorId formats (simple IDs, UUIDs, hashes)
 * - Edge cases
 */
public class ReceivedBtDataEntityTest {

    // ========== Constructor Tests ==========

    @Test
    public void testConstructorSetsAllFields() {
        String deviceAddress = "AA:BB:CC:DD:EE:FF";
        long timestamp = 1234567890L;
        String receivedMsg = "0xAB3311";
        String ownerUserId = "user123";
        String sensorId = "sensor001";

        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                deviceAddress, timestamp, receivedMsg, ownerUserId, sensorId);

        assertEquals(deviceAddress, entity.getDeviceAddress());
        assertEquals(timestamp, entity.getTimestamp());
        assertEquals(receivedMsg, entity.getReceivedMsg());
        assertEquals(ownerUserId, entity.getOwnerUserId());
        assertEquals(sensorId, entity.getSensorId());
    }

    @Test
    public void testIdDefaultsToZeroBeforePersistence() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");

        // Before Room persists, id should be 0 (auto-generated)
        assertEquals(0, entity.getId());
    }

    @Test
    public void testSetIdWorks() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user1", "sensor001");

        entity.setId(42L);
        assertEquals(42L, entity.getId());
    }

    // ========== Getter Tests ==========

    @Test
    public void testGetDeviceAddress() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "11:22:33:44:55:66", 1000L, "0xEF0112", "owner", "s1");

        assertEquals("11:22:33:44:55:66", entity.getDeviceAddress());
    }

    @Test
    public void testGetTimestamp() {
        long expected = System.currentTimeMillis();
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", expected, "0xAB3311", "user", "sensor");

        assertEquals(expected, entity.getTimestamp());
    }

    @Test
    public void testGetReceivedMsg() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xBA3311", "user", "sensor");

        assertEquals("0xBA3311", entity.getReceivedMsg());
    }

    @Test
    public void testGetOwnerUserId() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "aggregator-user-id", "sensor");

        assertEquals("aggregator-user-id", entity.getOwnerUserId());
    }

    @Test
    public void testGetSensorId() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor001");

        assertEquals("sensor001", entity.getSensorId());
    }

    // ========== Various SensorId Formats ==========

    @Test
    public void testSimpleSensorId() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor001");

        assertEquals("sensor001", entity.getSensorId());
    }

    @Test
    public void testUuidSensorId() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", uuid);

        assertEquals(uuid, entity.getSensorId());
    }

    @Test
    public void testHashSensorId() {
        String hash = "a1b2c3d4e5f6";
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", hash);

        assertEquals(hash, entity.getSensorId());
    }

    @Test
    public void testNumericSensorId() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "12345");

        assertEquals("12345", entity.getSensorId());
    }

    @Test
    public void testLongSensorId() {
        String longId = "this-is-a-very-long-sensor-id-that-might-be-used-in-production-systems-with-complex-naming-schemes";
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", longId);

        assertEquals(longId, entity.getSensorId());
    }

    // ========== Edge Cases ==========

    @Test
    public void testZeroTimestamp() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 0L, "0xAB3311", "user", "sensor");

        assertEquals(0L, entity.getTimestamp());
    }

    @Test
    public void testMaxTimestamp() {
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", Long.MAX_VALUE, "0xAB3311", "user", "sensor");

        assertEquals(Long.MAX_VALUE, entity.getTimestamp());
    }

    @Test
    public void testVariousHexCodeFormats() {
        // Lowercase hex
        ReceivedBtDataEntity e1 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xab3311", "user", "sensor");
        assertEquals("0xab3311", e1.getReceivedMsg());

        // Uppercase hex
        ReceivedBtDataEntity e2 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0XAB3311", "user", "sensor");
        assertEquals("0XAB3311", e2.getReceivedMsg());

        // Without 0x prefix
        ReceivedBtDataEntity e3 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "AB3311", "user", "sensor");
        assertEquals("AB3311", e3.getReceivedMsg());
    }

    @Test
    public void testMultipleEntitiesWithDifferentSensors() {
        ReceivedBtDataEntity e1 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor001");
        ReceivedBtDataEntity e2 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xEF0112", "user", "sensor002");
        ReceivedBtDataEntity e3 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xBA3311", "user", "sensor003");

        assertEquals("sensor001", e1.getSensorId());
        assertEquals("sensor002", e2.getSensorId());
        assertEquals("sensor003", e3.getSensorId());

        // All have same device address but different sensors
        assertEquals(e1.getDeviceAddress(), e2.getDeviceAddress());
        assertEquals(e2.getDeviceAddress(), e3.getDeviceAddress());
    }

    @Test
    public void testSameSensorDifferentTimestamps() {
        ReceivedBtDataEntity e1 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user", "sensor001");
        ReceivedBtDataEntity e2 = new ReceivedBtDataEntity(
                "AA:BB:CC:DD:EE:FF", 2000L, "0xBA3311", "user", "sensor001");

        assertEquals(e1.getSensorId(), e2.getSensorId());
        assertNotEquals(e1.getTimestamp(), e2.getTimestamp());
        assertNotEquals(e1.getReceivedMsg(), e2.getReceivedMsg());
    }
}
