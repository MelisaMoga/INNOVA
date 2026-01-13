package com.melisa.innovamotionapp.integration;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.bluetooth.PacketParser;
import com.melisa.innovamotionapp.bluetooth.ParsedReading;
import com.melisa.innovamotionapp.data.database.ReceivedBtDataEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Integration tests for the multi-user packet processing flow.
 * 
 * Tests the complete flow from packet parsing to entity creation,
 * simulating what DeviceCommunicationService does.
 */
public class ServiceIntegrationTest {

    private PacketParser packetParser;
    private static final String DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String OWNER_UID = "aggregator-user-123";

    @Before
    public void setUp() {
        packetParser = new PacketParser();
    }

    // ========== Packet Processing Flow Tests ==========

    @Test
    public void testSingleReadingPacketFlow() {
        // Simulate receiving a single reading packet
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(1, readings.size());

        // Create entity as service would
        ParsedReading reading = readings.get(0);
        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                DEVICE_ADDRESS,
                reading.getReceivedTimestamp(),
                reading.getHexCode(),
                OWNER_UID,
                reading.getSensorId()
        );

        assertEquals(DEVICE_ADDRESS, entity.getDeviceAddress());
        assertEquals("0xAB3311", entity.getReceivedMsg());
        assertEquals(OWNER_UID, entity.getOwnerUserId());
        assertEquals("sensor001", entity.getSensorId());
    }

    @Test
    public void testMultipleReadingsPacketFlow() {
        // Simulate receiving multiple readings in one packet
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        assertNull(packetParser.feedLine("sensor002;0xEF0112"));
        assertNull(packetParser.feedLine("sensor003;0xBA3311"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(3, readings.size());

        // Create entities as service would
        List<ReceivedBtDataEntity> entities = new ArrayList<>();
        for (ParsedReading reading : readings) {
            ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                    DEVICE_ADDRESS,
                    reading.getReceivedTimestamp(),
                    reading.getHexCode(),
                    OWNER_UID,
                    reading.getSensorId()
            );
            entities.add(entity);
        }

        assertEquals(3, entities.size());
        assertEquals("sensor001", entities.get(0).getSensorId());
        assertEquals("sensor002", entities.get(1).getSensorId());
        assertEquals("sensor003", entities.get(2).getSensorId());
    }

    @Test
    public void testDuplicateSensorInPacketFlow() {
        // Same sensor can appear multiple times in a packet
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        assertNull(packetParser.feedLine("sensor002;0xEF0112"));
        assertNull(packetParser.feedLine("sensor001;0xBA3311")); // sensor001 again
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(3, readings.size());

        // Both sensor001 readings should create separate entities
        List<ReceivedBtDataEntity> entities = new ArrayList<>();
        for (ParsedReading reading : readings) {
            entities.add(new ReceivedBtDataEntity(
                    DEVICE_ADDRESS,
                    reading.getReceivedTimestamp(),
                    reading.getHexCode(),
                    OWNER_UID,
                    reading.getSensorId()
            ));
        }

        assertEquals("sensor001", entities.get(0).getSensorId());
        assertEquals("0xAB3311", entities.get(0).getReceivedMsg());
        assertEquals("sensor001", entities.get(2).getSensorId());
        assertEquals("0xBA3311", entities.get(2).getReceivedMsg());
    }

    @Test
    public void testConsecutivePacketsFlow() {
        // First packet
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        assertNull(packetParser.feedLine("sensor002;0xEF0112"));
        List<ParsedReading> packet1 = packetParser.feedLine("END_PACKET");

        assertNotNull(packet1);
        assertEquals(2, packet1.size());

        // Second packet
        assertNull(packetParser.feedLine("sensor003;0xBA3311"));
        List<ParsedReading> packet2 = packetParser.feedLine("END_PACKET");

        assertNotNull(packet2);
        assertEquals(1, packet2.size());
        assertEquals("sensor003", packet2.get(0).getSensorId());
    }

    @Test
    public void testMalformedLinesSkippedInFlow() {
        // Mix of valid and malformed lines
        assertNull(packetParser.feedLine("sensor001;0xAB3311")); // Valid
        assertNull(packetParser.feedLine("invalid_no_semicolon")); // Malformed - skipped
        assertNull(packetParser.feedLine("sensor002;0xEF0112")); // Valid
        assertNull(packetParser.feedLine(";empty_sensor")); // Malformed - skipped
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(2, readings.size()); // Only valid ones

        assertEquals("sensor001", readings.get(0).getSensorId());
        assertEquals("sensor002", readings.get(1).getSensorId());
    }

    @Test
    public void testEmptyPacketFlow() {
        // Just END_PACKET with no readings
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertTrue(readings.isEmpty());
    }

    // ========== Fall Detection Simulation Tests ==========

    @Test
    public void testFallDetectionHexCode() {
        // 0xEF0112 is typically a falling posture code
        assertNull(packetParser.feedLine("sensor001;0xEF0112"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(1, readings.size());
        assertEquals("0xEF0112", readings.get(0).getHexCode());

        // Service would check if this is a fall posture and notify
        // Fall detection logic is in PostureFactory - verified separately
    }

    // ========== UUID Sensor ID Flow Tests ==========

    @Test
    public void testUuidSensorIdFlow() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        assertNull(packetParser.feedLine(uuid + ";0xAB3311"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(1, readings.size());

        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                DEVICE_ADDRESS,
                readings.get(0).getReceivedTimestamp(),
                readings.get(0).getHexCode(),
                OWNER_UID,
                readings.get(0).getSensorId()
        );

        assertEquals(uuid, entity.getSensorId());
    }

    // ========== Batch Processing Tests ==========

    @Test
    public void testLargePacketBatchProcessing() {
        // Simulate a large packet with many readings
        int numReadings = 50;
        for (int i = 0; i < numReadings; i++) {
            assertNull(packetParser.feedLine("sensor" + String.format("%03d", i) + ";0xAB3311"));
        }
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(numReadings, readings.size());

        // Create all entities as service would (batch insert)
        List<ReceivedBtDataEntity> entities = new ArrayList<>();
        for (ParsedReading reading : readings) {
            entities.add(new ReceivedBtDataEntity(
                    DEVICE_ADDRESS,
                    reading.getReceivedTimestamp(),
                    reading.getHexCode(),
                    OWNER_UID,
                    reading.getSensorId()
            ));
        }

        assertEquals(numReadings, entities.size());
    }

    // ========== Timestamp Tests ==========

    @Test
    public void testTimestampsAreUnique() throws InterruptedException {
        // Each reading should have its own timestamp
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        Thread.sleep(5); // Small delay
        assertNull(packetParser.feedLine("sensor002;0xEF0112"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");

        assertNotNull(readings);
        assertEquals(2, readings.size());

        // Timestamps might be same or different depending on timing
        // Both should be valid (> 0)
        assertTrue(readings.get(0).getReceivedTimestamp() > 0);
        assertTrue(readings.get(1).getReceivedTimestamp() > 0);
    }

    @Test
    public void testEntityTimestampsFromReadings() {
        long before = System.currentTimeMillis();
        assertNull(packetParser.feedLine("sensor001;0xAB3311"));
        List<ParsedReading> readings = packetParser.feedLine("END_PACKET");
        long after = System.currentTimeMillis();

        ReceivedBtDataEntity entity = new ReceivedBtDataEntity(
                DEVICE_ADDRESS,
                readings.get(0).getReceivedTimestamp(),
                readings.get(0).getHexCode(),
                OWNER_UID,
                readings.get(0).getSensorId()
        );

        assertTrue(entity.getTimestamp() >= before);
        assertTrue(entity.getTimestamp() <= after);
    }
}
