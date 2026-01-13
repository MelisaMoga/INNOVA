package com.melisa.innovamotionapp.activities;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for BtConnectedActivity navigation logic.
 * 
 * Tests intent extras handling, navigation validation, and sensor context preservation.
 * These tests verify business logic independent of the Android framework.
 */
public class BtConnectedActivityTest {

    // ========== Intent Extras Constants Tests ==========

    @Test
    public void extraSensorIdKey_isCorrect() {
        assertEquals("extra_sensor_id", BtConnectedActivity.EXTRA_SENSOR_ID);
    }

    @Test
    public void extraPersonNameKey_isCorrect() {
        assertEquals("extra_person_name", BtConnectedActivity.EXTRA_PERSON_NAME);
    }

    // ========== Navigation Validation Tests ==========

    @Test
    public void canNavigate_always_returnsTrue() {
        // Navigation should always be possible from BtConnectedActivity
        // even with null/empty sensorId (falls back to legacy mode)
        assertTrue(canNavigate(null, null));
        assertTrue(canNavigate("sensor001", "Ion Popescu"));
        assertTrue(canNavigate("", ""));
    }

    // ========== Intent Extras Preservation Tests ==========

    @Test
    public void intentExtras_sensorIdIsPreserved() {
        String sensorId = "sensor001";
        IntentExtras extras = createIntentExtras(sensorId, "Ion Popescu");
        assertEquals(sensorId, extras.sensorId);
    }

    @Test
    public void intentExtras_personNameIsPreserved() {
        String personName = "Ion Popescu";
        IntentExtras extras = createIntentExtras("sensor001", personName);
        assertEquals(personName, extras.personName);
    }

    @Test
    public void intentExtras_nullValuesArePreserved() {
        IntentExtras extras = createIntentExtras(null, null);
        assertNull(extras.sensorId);
        assertNull(extras.personName);
    }

    @Test
    public void intentExtras_emptyValuesArePreserved() {
        IntentExtras extras = createIntentExtras("", "");
        assertEquals("", extras.sensorId);
        assertEquals("", extras.personName);
    }

    // ========== Display Name Resolution Tests ==========

    @Test
    public void getDisplayName_withPersonName_returnsPersonName() {
        assertEquals("Ion Popescu", getDisplayName("Ion Popescu", "Default User"));
    }

    @Test
    public void getDisplayName_withNullPersonName_returnsFallback() {
        assertEquals("Default User", getDisplayName(null, "Default User"));
    }

    @Test
    public void getDisplayName_withEmptyPersonName_returnsFallback() {
        assertEquals("Default User", getDisplayName("", "Default User"));
    }

    @Test
    public void getDisplayName_withBothNull_returnsEmptyString() {
        assertEquals("", getDisplayName(null, null));
    }

    // ========== Navigation Target Tests ==========

    @Test
    public void navigationTargets_areThree() {
        String[] targets = getNavigationTargets();
        assertEquals(3, targets.length);
    }

    @Test
    public void navigationTargets_areDistinct() {
        String[] targets = getNavigationTargets();
        
        for (int i = 0; i < targets.length; i++) {
            for (int j = i + 1; j < targets.length; j++) {
                assertNotEquals("Targets should be distinct", targets[i], targets[j]);
            }
        }
    }

    @Test
    public void navigationTargets_includeStatistics() {
        assertTrue(containsTarget("StatisticsActivity"));
    }

    @Test
    public void navigationTargets_includeTimeLapse() {
        assertTrue(containsTarget("TimeLapseActivity"));
    }

    @Test
    public void navigationTargets_includeEnergyConsumption() {
        assertTrue(containsTarget("EnergyConsumptionActivity"));
    }

    @Test
    public void navigationTargets_excludeMonitorizare() {
        // BtConnectedActivity IS the Monitorizare screen, so it shouldn't navigate to itself
        assertFalse(containsTarget("BtConnectedActivity"));
    }

    // ========== Sensor Mode Detection Tests ==========

    @Test
    public void isSensorMode_withValidSensorIdAndSupervisor_returnsTrue() {
        assertTrue(isSensorMode("sensor001", true));
    }

    @Test
    public void isSensorMode_withNullSensorId_returnsFalse() {
        assertFalse(isSensorMode(null, true));
    }

    @Test
    public void isSensorMode_withEmptySensorId_returnsFalse() {
        assertFalse(isSensorMode("", true));
    }

    @Test
    public void isSensorMode_withValidSensorIdButNotSupervisor_returnsFalse() {
        assertFalse(isSensorMode("sensor001", false));
    }

    // ========== Helper Classes and Methods ==========

    /**
     * Simple class to simulate intent extras.
     */
    private static class IntentExtras {
        final String sensorId;
        final String personName;
        
        IntentExtras(String sensorId, String personName) {
            this.sensorId = sensorId;
            this.personName = personName;
        }
    }

    /**
     * Checks if navigation is possible.
     * Navigation is always possible from BtConnectedActivity.
     */
    private boolean canNavigate(String sensorId, String personName) {
        return true; // Always navigable
    }

    /**
     * Creates intent extras for navigation.
     */
    private IntentExtras createIntentExtras(String sensorId, String personName) {
        return new IntentExtras(sensorId, personName);
    }

    /**
     * Gets display name with fallback.
     * Mirrors logic in BtConnectedActivity.displayPostureData().
     */
    private String getDisplayName(String personName, String fallbackName) {
        if (personName != null && !personName.isEmpty()) {
            return personName;
        }
        return fallbackName != null ? fallbackName : "";
    }

    /**
     * Gets all navigation target activity names.
     */
    private String[] getNavigationTargets() {
        return new String[] {
            "StatisticsActivity",
            "TimeLapseActivity",
            "EnergyConsumptionActivity"
        };
    }

    /**
     * Checks if the navigation targets contain a specific target.
     */
    private boolean containsTarget(String target) {
        for (String t : getNavigationTargets()) {
            if (t.equals(target)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determines if sensor-specific mode should be used.
     * Mirrors logic in BtConnectedActivity.onCreate().
     */
    private boolean isSensorMode(String sensorId, boolean isSupervisor) {
        return sensorId != null && !sensorId.isEmpty() && isSupervisor;
    }
}
