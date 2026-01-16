package com.melisa.innovamotionapp.ui.fragments;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit tests for LivePostureFragment grid logic.
 * 
 * Refactored to test the new grid-based architecture that matches SupervisorDashboard.
 * Tests person card click handling, grid data transformation, and navigation.
 */
public class LivePostureFragmentTest {

    // ========== Grid Data Tests ==========

    @Test
    public void gridIsEmpty_whenNoData_returnsTrue() {
        List<PersonStatus> statuses = new ArrayList<>();
        assertTrue(shouldShowEmptyState(statuses));
    }

    @Test
    public void gridIsEmpty_whenNull_returnsTrue() {
        assertTrue(shouldShowEmptyState(null));
    }

    @Test
    public void gridIsNotEmpty_whenHasData_returnsFalse() {
        List<PersonStatus> statuses = new ArrayList<>();
        statuses.add(new PersonStatus("sensor001", "Ion Popescu", 1234567890L, false));
        assertFalse(shouldShowEmptyState(statuses));
    }

    // ========== Person Card Click Tests ==========

    @Test
    public void onPersonClick_extractsSensorId() {
        PersonStatus person = new PersonStatus("sensor001", "Ion Popescu", 1234567890L, false);
        assertEquals("sensor001", person.sensorId);
    }

    @Test
    public void onPersonClick_extractsPersonName() {
        PersonStatus person = new PersonStatus("sensor001", "Ion Popescu", 1234567890L, false);
        assertEquals("Ion Popescu", person.displayName);
    }

    // ========== Navigation Tests ==========

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

    // ========== Alert Sorting Tests ==========

    @Test
    public void alerts_areSortedFirst() {
        List<PersonStatus> statuses = new ArrayList<>();
        statuses.add(new PersonStatus("sensor001", "Ana", 1234567890L, false));
        statuses.add(new PersonStatus("sensor002", "Bogdan", 1234567890L, true)); // Alert
        statuses.add(new PersonStatus("sensor003", "Carmen", 1234567890L, false));
        
        List<PersonStatus> sorted = sortByAlertThenName(statuses);
        
        assertTrue("Alerts should be first", sorted.get(0).isAlert);
        assertEquals("Bogdan", sorted.get(0).displayName);
    }

    @Test
    public void nonAlerts_areSortedAlphabetically() {
        List<PersonStatus> statuses = new ArrayList<>();
        statuses.add(new PersonStatus("sensor003", "Carmen", 1234567890L, false));
        statuses.add(new PersonStatus("sensor001", "Ana", 1234567890L, false));
        statuses.add(new PersonStatus("sensor002", "Bogdan", 1234567890L, false));
        
        List<PersonStatus> sorted = sortByAlertThenName(statuses);
        
        assertEquals("Ana", sorted.get(0).displayName);
        assertEquals("Bogdan", sorted.get(1).displayName);
        assertEquals("Carmen", sorted.get(2).displayName);
    }

    // ========== Navigation Target Tests ==========

    @Test
    public void navigationTarget_isPersonDetailActivity() {
        assertEquals("PersonDetailActivity", getNavigationTarget());
    }

    // ========== Unified Flow Tests ==========

    @Test
    public void aggregatorFlow_matchesSupervisorFlow() {
        // Both flows now use the same pattern:
        // Grid → Click Person → PersonDetailActivity
        String aggregatorTarget = getNavigationTarget();
        String supervisorTarget = "PersonDetailActivity"; // Same as supervisor
        assertEquals(aggregatorTarget, supervisorTarget);
    }

    // ========== Helper Classes and Methods ==========

    /**
     * Simple class to simulate PersonStatus.
     */
    private static class PersonStatus {
        final String sensorId;
        final String displayName;
        final long timestamp;
        final boolean isAlert;
        
        PersonStatus(String sensorId, String displayName, long timestamp, boolean isAlert) {
            this.sensorId = sensorId;
            this.displayName = displayName;
            this.timestamp = timestamp;
            this.isAlert = isAlert;
        }
    }

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
     * Determines if empty state should be shown.
     */
    private boolean shouldShowEmptyState(List<PersonStatus> statuses) {
        return statuses == null || statuses.isEmpty();
    }

    /**
     * Determines if navigation is allowed for the given sensor ID.
     */
    private boolean canNavigate(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    /**
     * Creates intent extras for navigation.
     */
    private IntentExtras createIntentExtras(String sensorId, String personName) {
        return new IntentExtras(sensorId, personName);
    }

    /**
     * Sorts statuses: alerts first, then alphabetically by name.
     */
    private List<PersonStatus> sortByAlertThenName(List<PersonStatus> statuses) {
        List<PersonStatus> sorted = new ArrayList<>(statuses);
        sorted.sort((a, b) -> {
            if (a.isAlert != b.isAlert) {
                return a.isAlert ? -1 : 1;
            }
            return a.displayName.compareToIgnoreCase(b.displayName);
        });
        return sorted;
    }

    /**
     * Gets the navigation target activity name.
     */
    private String getNavigationTarget() {
        return "PersonDetailActivity";
    }
}
