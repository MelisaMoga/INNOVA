package com.melisa.innovamotionapp.utils;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

/**
 * Unit tests for MockDataGenerator and TestScenario.
 * 
 * Tests cover:
 * - Posture hex codes are valid
 * - Test scenario properties
 * - Random posture generation distribution
 */
public class MockDataGeneratorTest {

    // ========== Posture Hex Code Tests ==========

    @Test
    public void hexStanding_isCorrectFormat() {
        assertEquals("0xAB3311", MockDataGenerator.HEX_STANDING);
    }

    @Test
    public void hexSitting_isCorrectFormat() {
        assertEquals("0xAC4312", MockDataGenerator.HEX_SITTING);
    }

    @Test
    public void hexWalking_isCorrectFormat() {
        assertEquals("0xBA3311", MockDataGenerator.HEX_WALKING);
    }

    @Test
    public void hexFalling_isCorrectFormat() {
        assertEquals("0xEF0112", MockDataGenerator.HEX_FALLING);
    }

    @Test
    public void hexUnused_isCorrectFormat() {
        assertEquals("0x793248", MockDataGenerator.HEX_UNUSED);
    }

    @Test
    public void allPostureHexCodes_startWithPrefix() {
        String[] codes = {
                MockDataGenerator.HEX_STANDING,
                MockDataGenerator.HEX_SITTING,
                MockDataGenerator.HEX_WALKING,
                MockDataGenerator.HEX_FALLING,
                MockDataGenerator.HEX_UNUSED
        };
        
        for (String code : codes) {
            assertTrue("Hex code should start with 0x: " + code, code.startsWith("0x"));
        }
    }

    @Test
    public void allPostureHexCodes_areUnique() {
        Set<String> codes = new HashSet<>();
        codes.add(MockDataGenerator.HEX_STANDING);
        codes.add(MockDataGenerator.HEX_SITTING);
        codes.add(MockDataGenerator.HEX_WALKING);
        codes.add(MockDataGenerator.HEX_FALLING);
        codes.add(MockDataGenerator.HEX_UNUSED);
        
        assertEquals("All posture codes should be unique", 5, codes.size());
    }

    // ========== TestScenario Property Tests ==========

    @Test
    public void basicMultiPerson_hasCorrectProperties() {
        TestScenario scenario = TestScenario.BASIC_MULTI_PERSON;
        
        assertEquals("basic_multi_person", scenario.getId());
        assertEquals(3, scenario.getSensorCount());
        assertEquals(10, scenario.getReadingsPerSensor());
        assertFalse(scenario.shouldIncludeFall());
        assertFalse(scenario.shouldIncludeStale());
    }

    @Test
    public void fallDetection_hasCorrectProperties() {
        TestScenario scenario = TestScenario.FALL_DETECTION;
        
        assertEquals("fall_detection", scenario.getId());
        assertEquals(1, scenario.getSensorCount());
        assertTrue(scenario.shouldIncludeFall());
        assertFalse(scenario.shouldIncludeStale());
    }

    @Test
    public void staleData_hasCorrectProperties() {
        TestScenario scenario = TestScenario.STALE_DATA;
        
        assertEquals("stale_data", scenario.getId());
        assertFalse(scenario.shouldIncludeFall());
        assertTrue(scenario.shouldIncludeStale());
    }

    @Test
    public void highVolume_hasCorrectProperties() {
        TestScenario scenario = TestScenario.HIGH_VOLUME;
        
        assertEquals("high_volume", scenario.getId());
        assertEquals(10, scenario.getSensorCount());
        assertEquals(50, scenario.getReadingsPerSensor());
        assertFalse(scenario.shouldIncludeFall());
        assertFalse(scenario.shouldIncludeStale());
    }

    @Test
    public void nameResolution_hasCorrectProperties() {
        TestScenario scenario = TestScenario.NAME_RESOLUTION;
        
        assertEquals("name_resolution", scenario.getId());
        assertEquals(5, scenario.getSensorCount());
    }

    @Test
    public void mixedStates_hasCorrectProperties() {
        TestScenario scenario = TestScenario.MIXED_STATES;
        
        assertEquals("mixed_states", scenario.getId());
        assertTrue(scenario.shouldIncludeFall());
        assertTrue(scenario.shouldIncludeStale());
    }

    @Test
    public void allScenarios_haveUniqueIds() {
        Set<String> ids = new HashSet<>();
        for (TestScenario scenario : TestScenario.values()) {
            ids.add(scenario.getId());
        }
        assertEquals(TestScenario.values().length, ids.size());
    }

    @Test
    public void allScenarios_havePositiveSensorCount() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Sensor count should be positive: " + scenario.getId(),
                    scenario.getSensorCount() > 0);
        }
    }

    @Test
    public void allScenarios_havePositiveReadingsPerSensor() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Readings per sensor should be positive: " + scenario.getId(),
                    scenario.getReadingsPerSensor() > 0);
        }
    }

    @Test
    public void allScenarios_haveValidTitleResId() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Title res ID should be positive: " + scenario.getId(),
                    scenario.getTitleResId() > 0);
        }
    }

    @Test
    public void allScenarios_haveValidDescriptionResId() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Description res ID should be positive: " + scenario.getId(),
                    scenario.getDescriptionResId() > 0);
        }
    }

    @Test
    public void getTotalReadings_calculatesCorrectly() {
        TestScenario scenario = TestScenario.HIGH_VOLUME;
        int expected = scenario.getSensorCount() * scenario.getReadingsPerSensor();
        assertEquals(expected, scenario.getTotalReadings());
    }

    @Test
    public void basicMultiPerson_totalReadingsIs30() {
        // 3 sensors * 10 readings = 30
        assertEquals(30, TestScenario.BASIC_MULTI_PERSON.getTotalReadings());
    }

    @Test
    public void highVolume_totalReadingsIs500() {
        // 10 sensors * 50 readings = 500
        assertEquals(500, TestScenario.HIGH_VOLUME.getTotalReadings());
    }

    // ========== Scenario Count Tests ==========

    @Test
    public void testScenario_hasSixScenarios() {
        assertEquals(6, TestScenario.values().length);
    }

    @Test
    public void scenarioEnumValues_existInExpectedOrder() {
        TestScenario[] expected = {
                TestScenario.BASIC_MULTI_PERSON,
                TestScenario.FALL_DETECTION,
                TestScenario.STALE_DATA,
                TestScenario.HIGH_VOLUME,
                TestScenario.NAME_RESOLUTION,
                TestScenario.MIXED_STATES
        };
        
        assertArrayEquals(expected, TestScenario.values());
    }

    // ========== Boundary Tests ==========

    @Test
    public void allScenarios_sensorCountWithinReasonableRange() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Sensor count should be <= 100: " + scenario.getId(),
                    scenario.getSensorCount() <= 100);
        }
    }

    @Test
    public void allScenarios_readingsPerSensorWithinReasonableRange() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Readings per sensor should be <= 100: " + scenario.getId(),
                    scenario.getReadingsPerSensor() <= 100);
        }
    }

    @Test
    public void allScenarios_totalReadingsWithinReasonableRange() {
        for (TestScenario scenario : TestScenario.values()) {
            assertTrue("Total readings should be <= 1000: " + scenario.getId(),
                    scenario.getTotalReadings() <= 1000);
        }
    }
}
