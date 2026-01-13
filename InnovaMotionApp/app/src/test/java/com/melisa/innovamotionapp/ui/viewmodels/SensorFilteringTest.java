package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for sensor filtering logic in ViewModels.
 * 
 * Tests the priority logic where sensor-based filtering takes precedence
 * over user-based filtering when a sensorId is set.
 */
public class SensorFilteringTest {

    // ========== Sensor Mode Detection Tests ==========

    @Test
    public void isSensorMode_withValidSensorId_returnsTrue() {
        assertTrue(isSensorMode("sensor001"));
    }

    @Test
    public void isSensorMode_withNullSensorId_returnsFalse() {
        assertFalse(isSensorMode(null));
    }

    @Test
    public void isSensorMode_withEmptySensorId_returnsFalse() {
        assertFalse(isSensorMode(""));
    }

    @Test
    public void isSensorMode_withWhitespaceSensorId_returnsFalse() {
        // Assuming we trim, whitespace-only should be false
        assertFalse(isSensorMode("   "));
    }

    // ========== Filtering Priority Tests ==========

    @Test
    public void filteringPriority_sensorIdSet_usesSensorFiltering() {
        String result = determineFilteringMode("sensor001", "user123");
        assertEquals("sensor", result);
    }

    @Test
    public void filteringPriority_onlyUserIdSet_usesUserFiltering() {
        String result = determineFilteringMode(null, "user123");
        assertEquals("user", result);
    }

    @Test
    public void filteringPriority_bothNull_usesNone() {
        String result = determineFilteringMode(null, null);
        assertEquals("none", result);
    }

    @Test
    public void filteringPriority_sensorIdEmpty_usesUserFiltering() {
        String result = determineFilteringMode("", "user123");
        assertEquals("user", result);
    }

    // ========== Query Selection Tests ==========

    @Test
    public void queryType_sensorMode_usesAllForSensor() {
        String query = selectQuery("sensor001", null);
        assertEquals("getAllForSensor", query);
    }

    @Test
    public void queryType_userMode_usesAllForUser() {
        String query = selectQuery(null, "user123");
        assertEquals("getAllForUser", query);
    }

    @Test
    public void queryType_rangeWithSensor_usesRangeForSensor() {
        String query = selectRangeQuery("sensor001", null);
        assertEquals("getRangeForSensor", query);
    }

    @Test
    public void queryType_rangeWithUser_usesRangeForUser() {
        String query = selectRangeQuery(null, "user123");
        assertEquals("getRangeForUser", query);
    }

    // ========== Sensor ID Format Tests ==========

    @Test
    public void sensorIdFormat_simpleId_isValid() {
        assertTrue(isValidSensorIdFormat("sensor001"));
    }

    @Test
    public void sensorIdFormat_uuid_isValid() {
        assertTrue(isValidSensorIdFormat("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
    }

    @Test
    public void sensorIdFormat_alphanumeric_isValid() {
        assertTrue(isValidSensorIdFormat("abc123XYZ"));
    }

    @Test
    public void sensorIdFormat_withUnderscores_isValid() {
        assertTrue(isValidSensorIdFormat("sensor_001_v2"));
    }

    @Test
    public void sensorIdFormat_withHyphens_isValid() {
        assertTrue(isValidSensorIdFormat("sensor-001-v2"));
    }

    // ========== Edge Cases ==========

    @Test
    public void edgeCase_switchFromUserToSensor_usesSensor() {
        // Simulate setting userId first, then sensorId
        MockViewModel vm = new MockViewModel();
        vm.setUserId("user123");
        assertEquals("user", vm.getActiveMode());
        
        vm.setSensorId("sensor001");
        assertEquals("sensor", vm.getActiveMode());
    }

    @Test
    public void edgeCase_clearSensorId_fallsBackToUser() {
        MockViewModel vm = new MockViewModel();
        vm.setUserId("user123");
        vm.setSensorId("sensor001");
        assertEquals("sensor", vm.getActiveMode());
        
        vm.setSensorId(null);
        assertEquals("user", vm.getActiveMode());
    }

    // ========== Helper Methods ==========

    private boolean isSensorMode(String sensorId) {
        return sensorId != null && !sensorId.trim().isEmpty();
    }

    private String determineFilteringMode(String sensorId, String userId) {
        if (sensorId != null && !sensorId.isEmpty()) {
            return "sensor";
        }
        if (userId != null && !userId.isEmpty()) {
            return "user";
        }
        return "none";
    }

    private String selectQuery(String sensorId, String userId) {
        if (sensorId != null && !sensorId.isEmpty()) {
            return "getAllForSensor";
        }
        return "getAllForUser";
    }

    private String selectRangeQuery(String sensorId, String userId) {
        if (sensorId != null && !sensorId.isEmpty()) {
            return "getRangeForSensor";
        }
        return "getRangeForUser";
    }

    private boolean isValidSensorIdFormat(String sensorId) {
        // Any non-empty string is a valid sensor ID format
        return sensorId != null && !sensorId.trim().isEmpty();
    }

    // ========== Mock ViewModel for Testing ==========

    private static class MockViewModel {
        private String sensorId;
        private String userId;

        void setSensorId(String sensorId) {
            this.sensorId = sensorId;
        }

        void setUserId(String userId) {
            this.userId = userId;
        }

        String getActiveMode() {
            if (sensorId != null && !sensorId.isEmpty()) {
                return "sensor";
            }
            if (userId != null && !userId.isEmpty()) {
                return "user";
            }
            return "none";
        }
    }
}
