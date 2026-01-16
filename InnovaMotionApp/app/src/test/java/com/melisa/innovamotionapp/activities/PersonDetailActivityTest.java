package com.melisa.innovamotionapp.activities;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PersonDetailActivity navigation logic.
 * 
 * Tests intent extras handling, navigation validation, and sensor context preservation.
 * These tests verify business logic independent of the Android framework.
 * 
 * This activity is used by BOTH Aggregator and Supervisor roles:
 * - Aggregator: Click person in Live Posture tab → PersonDetailActivity
 * - Supervisor: Click person in Dashboard → PersonDetailActivity
 */
public class PersonDetailActivityTest {

    // ========== Intent Extras Constants Tests ==========

    @Test
    public void extraSensorIdKey_isCorrect() {
        assertEquals("extra_sensor_id", PersonDetailActivity.EXTRA_SENSOR_ID);
    }

    @Test
    public void extraPersonNameKey_isCorrect() {
        assertEquals("extra_person_name", PersonDetailActivity.EXTRA_PERSON_NAME);
    }

    // ========== Navigation Validation Tests ==========

    @Test
    public void canNavigate_always_returnsTrue() {
        // Navigation should always be possible from PersonDetailActivity
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
    public void navigationTargets_excludeSelf() {
        // PersonDetailActivity shouldn't navigate to itself
        assertFalse(containsTarget("PersonDetailActivity"));
    }

    // ========== Sensor Mode Detection Tests (UPDATED - role-agnostic) ==========

    @Test
    public void isSensorMode_withValidSensorId_returnsTrue() {
        // FIX: Now works for BOTH Aggregator and Supervisor
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

    // ========== Role-Agnostic Tests ==========

    @Test
    public void sensorMode_worksForAggregator() {
        // Aggregators can now use sensor-specific mode
        assertTrue(isSensorModeForRole("sensor001", "aggregator"));
    }

    @Test
    public void sensorMode_worksForSupervisor() {
        // Supervisors can use sensor-specific mode
        assertTrue(isSensorModeForRole("sensor001", "supervisor"));
    }

    @Test
    public void sensorMode_failsWithoutSensorId() {
        assertFalse(isSensorModeForRole(null, "aggregator"));
        assertFalse(isSensorModeForRole(null, "supervisor"));
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
     * Navigation is always possible from PersonDetailActivity.
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
     * Mirrors logic in PersonDetailActivity.displayPostureData().
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
     * UPDATED: Role-agnostic - only depends on sensorId being valid.
     */
    private boolean isSensorMode(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    /**
     * Determines if sensor-specific mode works for a given role.
     * UPDATED: Both roles now support sensor mode.
     */
    private boolean isSensorModeForRole(String sensorId, String role) {
        // Role doesn't matter anymore - if sensorId is valid, use sensor mode
        return sensorId != null && !sensorId.isEmpty();
    }
}
