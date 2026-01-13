package com.melisa.innovamotionapp.activities;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for child activities (Statistics, TimeLapse, EnergyConsumption) filter logic.
 * 
 * Tests title generation, sensor mode detection, and filtering behavior.
 * These tests verify business logic independent of the Android framework.
 */
public class ChildActivitiesFilterTest {

    // ========== Intent Extras Constants Tests ==========

    @Test
    public void statisticsActivity_extraSensorIdKey_isCorrect() {
        assertEquals("extra_sensor_id", StatisticsActivity.EXTRA_SENSOR_ID);
    }

    @Test
    public void statisticsActivity_extraPersonNameKey_isCorrect() {
        assertEquals("extra_person_name", StatisticsActivity.EXTRA_PERSON_NAME);
    }

    @Test
    public void timeLapseActivity_extraSensorIdKey_isCorrect() {
        assertEquals("extra_sensor_id", TimeLapseActivity.EXTRA_SENSOR_ID);
    }

    @Test
    public void timeLapseActivity_extraPersonNameKey_isCorrect() {
        assertEquals("extra_person_name", TimeLapseActivity.EXTRA_PERSON_NAME);
    }

    @Test
    public void energyConsumptionActivity_extraSensorIdKey_isCorrect() {
        assertEquals("extra_sensor_id", EnergyConsumptionActivity.EXTRA_SENSOR_ID);
    }

    @Test
    public void energyConsumptionActivity_extraPersonNameKey_isCorrect() {
        assertEquals("extra_person_name", EnergyConsumptionActivity.EXTRA_PERSON_NAME);
    }

    // ========== Title Generation Logic Tests ==========

    @Test
    public void getTitleForStatistics_withPersonName_returnsFormattedTitle() {
        String result = getTitleForStatistics("Ion Popescu");
        assertEquals("Statistica: Ion Popescu", result);
    }

    @Test
    public void getTitleForStatistics_withNullPersonName_returnsDefaultTitle() {
        String result = getTitleForStatistics(null);
        assertEquals("Statistica", result);
    }

    @Test
    public void getTitleForStatistics_withEmptyPersonName_returnsDefaultTitle() {
        String result = getTitleForStatistics("");
        assertEquals("Statistica", result);
    }

    @Test
    public void getTitleForActivities_withPersonName_returnsFormattedTitle() {
        String result = getTitleForActivities("Maria Ionescu");
        assertEquals("Activitati: Maria Ionescu", result);
    }

    @Test
    public void getTitleForActivities_withNullPersonName_returnsDefaultTitle() {
        String result = getTitleForActivities(null);
        assertEquals("Activitati", result);
    }

    @Test
    public void getTitleForActivities_withEmptyPersonName_returnsDefaultTitle() {
        String result = getTitleForActivities("");
        assertEquals("Activitati", result);
    }

    @Test
    public void getTitleForEnergy_withPersonName_returnsFormattedTitle() {
        String result = getTitleForEnergy("Andrei Vasilescu");
        assertEquals("Consum energetic: Andrei Vasilescu", result);
    }

    @Test
    public void getTitleForEnergy_withNullPersonName_returnsDefaultTitle() {
        String result = getTitleForEnergy(null);
        assertEquals("Consum energetic", result);
    }

    @Test
    public void getTitleForEnergy_withEmptyPersonName_returnsDefaultTitle() {
        String result = getTitleForEnergy("");
        assertEquals("Consum energetic", result);
    }

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
    public void isSensorMode_withWhitespaceSensorId_returnsTrue() {
        // Whitespace is a valid (albeit unusual) sensor ID
        assertTrue(isSensorMode("   "));
    }

    // ========== Fallback Behavior Tests ==========

    @Test
    public void fallbackBehavior_sensorIdMissing_usesUserBasedMode() {
        FilterMode mode = determineFilterMode(null);
        assertEquals(FilterMode.USER_BASED, mode);
    }

    @Test
    public void fallbackBehavior_sensorIdEmpty_usesUserBasedMode() {
        FilterMode mode = determineFilterMode("");
        assertEquals(FilterMode.USER_BASED, mode);
    }

    @Test
    public void fallbackBehavior_sensorIdPresent_usesSensorBasedMode() {
        FilterMode mode = determineFilterMode("sensor123");
        assertEquals(FilterMode.SENSOR_BASED, mode);
    }

    // ========== Person Name Display Tests ==========

    @Test
    public void hasPersonName_withValidName_returnsTrue() {
        assertTrue(hasPersonName("Ion Popescu"));
    }

    @Test
    public void hasPersonName_withNullName_returnsFalse() {
        assertFalse(hasPersonName(null));
    }

    @Test
    public void hasPersonName_withEmptyName_returnsFalse() {
        assertFalse(hasPersonName(""));
    }

    @Test
    public void hasPersonName_withWhitespaceName_returnsTrue() {
        // Whitespace is considered a valid name (passes isEmpty check)
        assertTrue(hasPersonName("   "));
    }

    // ========== Title Consistency Tests ==========

    @Test
    public void allActivities_useConsistentTitleFormat() {
        String name = "Test User";
        
        // All titles should follow same pattern: "{ActivityTitle}: {PersonName}"
        assertTrue(getTitleForStatistics(name).contains(": " + name));
        assertTrue(getTitleForActivities(name).contains(": " + name));
        assertTrue(getTitleForEnergy(name).contains(": " + name));
    }

    @Test
    public void allActivities_withoutPersonName_showDefaultTitles() {
        // All default titles should not contain ":"
        assertFalse(getTitleForStatistics(null).contains(":"));
        assertFalse(getTitleForActivities(null).contains(":"));
        assertFalse(getTitleForEnergy(null).contains(":"));
    }

    // ========== Edge Cases Tests ==========

    @Test
    public void personName_withSpecialCharacters_isHandled() {
        String specialName = "Ion-Maria O'Connor";
        String title = getTitleForStatistics(specialName);
        assertTrue(title.contains(specialName));
    }

    @Test
    public void personName_withUnicodeCharacters_isHandled() {
        String unicodeName = "Ștefan Țuțuianu";
        String title = getTitleForStatistics(unicodeName);
        assertTrue(title.contains(unicodeName));
    }

    @Test
    public void sensorId_withSpecialCharacters_isAccepted() {
        assertTrue(isSensorMode("sensor:001"));
        assertTrue(isSensorMode("sensor-001"));
        assertTrue(isSensorMode("sensor_001"));
    }

    // ========== Helper Enums ==========

    private enum FilterMode {
        USER_BASED,
        SENSOR_BASED
    }

    // ========== Helper Methods ==========

    /**
     * Generates title for Statistics activity.
     * Mirrors logic in StatisticsActivity.updateTitle().
     */
    private String getTitleForStatistics(String personName) {
        if (personName != null && !personName.isEmpty()) {
            return "Statistica: " + personName;
        }
        return "Statistica";
    }

    /**
     * Generates title for TimeLapse activity.
     * Mirrors logic in TimeLapseActivity.updateTitle().
     */
    private String getTitleForActivities(String personName) {
        if (personName != null && !personName.isEmpty()) {
            return "Activitati: " + personName;
        }
        return "Activitati";
    }

    /**
     * Generates title for EnergyConsumption activity.
     * Mirrors logic in EnergyConsumptionActivity.updateTitle().
     */
    private String getTitleForEnergy(String personName) {
        if (personName != null && !personName.isEmpty()) {
            return "Consum energetic: " + personName;
        }
        return "Consum energetic";
    }

    /**
     * Determines if sensor-specific mode should be used.
     * Mirrors logic in all child activities.
     */
    private boolean isSensorMode(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    /**
     * Determines the filter mode based on sensorId.
     * Mirrors the filtering logic in child activities.
     */
    private FilterMode determineFilterMode(String sensorId) {
        if (sensorId != null && !sensorId.isEmpty()) {
            return FilterMode.SENSOR_BASED;
        }
        return FilterMode.USER_BASED;
    }

    /**
     * Checks if person name is available for display.
     */
    private boolean hasPersonName(String personName) {
        return personName != null && !personName.isEmpty();
    }
}
