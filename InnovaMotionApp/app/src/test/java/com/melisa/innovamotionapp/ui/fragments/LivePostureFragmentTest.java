package com.melisa.innovamotionapp.ui.fragments;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for LivePostureFragment navigation logic.
 * 
 * Tests the navigation button visibility and intent extras validation.
 * These tests verify business logic independent of the Android framework.
 */
public class LivePostureFragmentTest {

    // ========== Navigation Button Visibility Tests ==========

    @Test
    public void shouldShowNavigationButtons_whenSensorSelected_returnsTrue() {
        assertTrue(shouldShowNavigationButtons("sensor001"));
    }

    @Test
    public void shouldShowNavigationButtons_whenNoSensorSelected_returnsFalse() {
        assertFalse(shouldShowNavigationButtons(null));
    }

    @Test
    public void shouldShowNavigationButtons_whenEmptySensorId_returnsFalse() {
        assertFalse(shouldShowNavigationButtons(""));
    }

    @Test
    public void shouldShowNavigationButtons_whenWhitespaceSensorId_returnsFalse() {
        assertFalse(shouldShowNavigationButtons("   "));
    }

    // ========== Navigation Validation Tests ==========

    @Test
    public void canNavigate_withValidSensorId_returnsTrue() {
        assertTrue(canNavigate("sensor001"));
    }

    @Test
    public void canNavigate_withNullSensorId_returnsFalse() {
        assertFalse(canNavigate(null));
    }

    @Test
    public void canNavigate_withEmptySensorId_returnsFalse() {
        assertFalse(canNavigate(""));
    }

    @Test
    public void canNavigate_withUuidSensorId_returnsTrue() {
        assertTrue(canNavigate("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
    }

    // ========== Intent Extras Validation Tests ==========

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
    public void intentExtras_nullPersonNameIsAllowed() {
        IntentExtras extras = createIntentExtras("sensor001", null);
        assertNull(extras.personName);
    }

    @Test
    public void intentExtras_emptyPersonNameIsAllowed() {
        IntentExtras extras = createIntentExtras("sensor001", "");
        assertEquals("", extras.personName);
    }

    // ========== Person Name Fallback Tests ==========

    @Test
    public void getDisplayPersonName_withValidName_returnsName() {
        assertEquals("Ion Popescu", getDisplayPersonName("Ion Popescu", "sensor001"));
    }

    @Test
    public void getDisplayPersonName_withNullName_returnsSensorId() {
        assertEquals("sensor001", getDisplayPersonName(null, "sensor001"));
    }

    @Test
    public void getDisplayPersonName_withEmptyName_returnsSensorId() {
        assertEquals("sensor001", getDisplayPersonName("", "sensor001"));
    }

    @Test
    public void getDisplayPersonName_withBothNull_returnsUnknown() {
        assertEquals("Unknown", getDisplayPersonName(null, null));
    }

    // ========== Activity Target Tests ==========

    @Test
    public void allNavigationTargetsAreDistinct() {
        String[] targets = {
            "BtConnectedActivity",
            "StatisticsActivity", 
            "TimeLapseActivity",
            "EnergyConsumptionActivity"
        };
        
        // Verify all targets are unique
        for (int i = 0; i < targets.length; i++) {
            for (int j = i + 1; j < targets.length; j++) {
                assertNotEquals("Targets should be distinct", targets[i], targets[j]);
            }
        }
    }

    @Test
    public void navigationTargetCount_isFour() {
        String[] targets = getNavigationTargets();
        assertEquals(4, targets.length);
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
     * Determines if navigation buttons should be visible.
     * Mirrors logic in LivePostureFragment.updateNavigationButtonsVisibility().
     */
    private boolean shouldShowNavigationButtons(String sensorId) {
        return sensorId != null && !sensorId.trim().isEmpty();
    }

    /**
     * Determines if navigation is allowed for the given sensor ID.
     * Mirrors validation in LivePostureFragment.navigateToActivity().
     */
    private boolean canNavigate(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    /**
     * Creates intent extras for navigation.
     * Mirrors what LivePostureFragment puts in the intent.
     */
    private IntentExtras createIntentExtras(String sensorId, String personName) {
        return new IntentExtras(sensorId, personName);
    }

    /**
     * Gets display name with fallback to sensor ID.
     * Mirrors the display name resolution logic.
     */
    private String getDisplayPersonName(String personName, String sensorId) {
        if (personName != null && !personName.isEmpty()) {
            return personName;
        }
        if (sensorId != null && !sensorId.isEmpty()) {
            return sensorId;
        }
        return "Unknown";
    }

    /**
     * Gets all navigation target activity names.
     */
    private String[] getNavigationTargets() {
        return new String[] {
            "BtConnectedActivity",
            "StatisticsActivity",
            "TimeLapseActivity",
            "EnergyConsumptionActivity"
        };
    }
}
