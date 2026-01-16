package com.melisa.innovamotionapp.activities;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for Person Detail View functionality.
 * 
 * Tests intent extras handling and sensor vs user mode selection.
 */
public class PersonDetailViewTest {

    // ========== Intent Extras Validation Tests ==========

    @Test
    public void isValidSensorId_null_returnsFalse() {
        assertFalse(isValidSensorId(null));
    }

    @Test
    public void isValidSensorId_empty_returnsFalse() {
        assertFalse(isValidSensorId(""));
    }

    @Test
    public void isValidSensorId_whitespaceOnly_returnsFalse() {
        assertFalse(isValidSensorId("   "));
    }

    @Test
    public void isValidSensorId_validId_returnsTrue() {
        assertTrue(isValidSensorId("sensor001"));
    }

    @Test
    public void isValidSensorId_uuid_returnsTrue() {
        assertTrue(isValidSensorId("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
    }

    // ========== Person Name Validation Tests ==========

    @Test
    public void isValidPersonName_null_returnsFalse() {
        assertFalse(isValidPersonName(null));
    }

    @Test
    public void isValidPersonName_empty_returnsFalse() {
        assertFalse(isValidPersonName(""));
    }

    @Test
    public void isValidPersonName_validName_returnsTrue() {
        assertTrue(isValidPersonName("Ion Popescu"));
    }

    @Test
    public void isValidPersonName_unicode_returnsTrue() {
        assertTrue(isValidPersonName("Ștefan Românescu"));
    }

    // ========== Mode Selection Tests ==========

    @Test
    public void shouldUseSensorMode_withSensorIdAndSupervisor_returnsTrue() {
        assertTrue(shouldUseSensorMode("sensor001", true));
    }

    @Test
    public void shouldUseSensorMode_withSensorIdAndAggregator_returnsTrue() {
        // FIX: Aggregators now ALSO support sensor mode (role-agnostic)
        assertTrue(shouldUseSensorMode("sensor001", false));
    }

    @Test
    public void shouldUseSensorMode_withoutSensorId_returnsFalse() {
        assertFalse(shouldUseSensorMode(null, true));
        assertFalse(shouldUseSensorMode("", true));
        assertFalse(shouldUseSensorMode(null, false));
        assertFalse(shouldUseSensorMode("", false));
    }

    // ========== Display Name Selection Tests ==========

    @Test
    public void getDisplayName_personNameProvided_usesPersonName() {
        String result = getDisplayName("Ion Popescu", "Default User");
        assertEquals("Ion Popescu", result);
    }

    @Test
    public void getDisplayName_personNameNull_usesFallback() {
        String result = getDisplayName(null, "Default User");
        assertEquals("Default User", result);
    }

    @Test
    public void getDisplayName_personNameEmpty_usesFallback() {
        String result = getDisplayName("", "Default User");
        assertEquals("Default User", result);
    }

    @Test
    public void getDisplayName_bothNull_returnsEmptyString() {
        String result = getDisplayName(null, null);
        assertEquals("", result);
    }

    // ========== Extra Key Consistency Tests ==========

    @Test
    public void extraKeys_areConsistentAcrossActivities() {
        // All activities should use the same extra keys
        String personDetailSensorKey = PersonDetailActivity.EXTRA_SENSOR_ID;
        String personDetailNameKey = PersonDetailActivity.EXTRA_PERSON_NAME;
        
        String statisticsSensorKey = StatisticsActivity.EXTRA_SENSOR_ID;
        String statisticsNameKey = StatisticsActivity.EXTRA_PERSON_NAME;
        
        String energySensorKey = EnergyConsumptionActivity.EXTRA_SENSOR_ID;
        String energyNameKey = EnergyConsumptionActivity.EXTRA_PERSON_NAME;
        
        String timelapseSensorKey = TimeLapseActivity.EXTRA_SENSOR_ID;
        String timelapseNameKey = TimeLapseActivity.EXTRA_PERSON_NAME;
        
        // All sensor ID keys should match
        assertEquals(personDetailSensorKey, statisticsSensorKey);
        assertEquals(statisticsSensorKey, energySensorKey);
        assertEquals(energySensorKey, timelapseSensorKey);
        
        // All person name keys should match
        assertEquals(personDetailNameKey, statisticsNameKey);
        assertEquals(statisticsNameKey, energyNameKey);
        assertEquals(energyNameKey, timelapseNameKey);
    }

    // ========== Helper Methods (simulating Activity logic) ==========

    private boolean isValidSensorId(String sensorId) {
        return sensorId != null && !sensorId.trim().isEmpty();
    }

    private boolean isValidPersonName(String personName) {
        return personName != null && !personName.isEmpty();
    }

    private boolean shouldUseSensorMode(String sensorId, boolean isSupervisor) {
        // FIX: Role-agnostic - both Aggregator and Supervisor can use sensor mode
        return isValidSensorId(sensorId);
    }

    private String getDisplayName(String personName, String fallbackName) {
        if (personName != null && !personName.isEmpty()) {
            return personName;
        }
        return fallbackName != null ? fallbackName : "";
    }
}
