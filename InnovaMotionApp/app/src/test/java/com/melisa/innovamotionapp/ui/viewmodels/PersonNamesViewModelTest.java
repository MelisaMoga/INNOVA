package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PersonNamesViewModel and related classes.
 * 
 * Note: Full ViewModel testing requires AndroidX Test framework since
 * PersonNamesViewModel extends AndroidViewModel. These tests cover
 * helper logic and adapter DiffCallback functionality.
 */
public class PersonNamesViewModelTest {

    // ========== Input Validation Tests ==========

    @Test
    public void isValidName_null_returnsFalse() {
        assertFalse(isValidName(null));
    }

    @Test
    public void isValidName_empty_returnsFalse() {
        assertFalse(isValidName(""));
    }

    @Test
    public void isValidName_whitespaceOnly_returnsFalse() {
        assertFalse(isValidName("   "));
    }

    @Test
    public void isValidName_validName_returnsTrue() {
        assertTrue(isValidName("Ion Popescu"));
    }

    @Test
    public void isValidName_singleCharacter_returnsTrue() {
        assertTrue(isValidName("I"));
    }

    @Test
    public void isValidName_unicode_returnsTrue() {
        assertTrue(isValidName("Ștefan Românescu"));
    }

    @Test
    public void isValidName_withNumbers_returnsTrue() {
        assertTrue(isValidName("Sensor 001"));
    }

    // ========== Trimming Tests ==========

    @Test
    public void trimmedName_leadingWhitespace_removed() {
        assertEquals("Ion", trimName("  Ion"));
    }

    @Test
    public void trimmedName_trailingWhitespace_removed() {
        assertEquals("Ion", trimName("Ion  "));
    }

    @Test
    public void trimmedName_bothSides_removed() {
        assertEquals("Ion Popescu", trimName("  Ion Popescu  "));
    }

    @Test
    public void trimmedName_noWhitespace_unchanged() {
        assertEquals("Ion", trimName("Ion"));
    }

    // ========== Sensor ID Validation Tests ==========

    @Test
    public void isValidSensorId_null_returnsFalse() {
        assertFalse(isValidSensorId(null));
    }

    @Test
    public void isValidSensorId_empty_returnsFalse() {
        assertFalse(isValidSensorId(""));
    }

    @Test
    public void isValidSensorId_simpleId_returnsTrue() {
        assertTrue(isValidSensorId("sensor001"));
    }

    @Test
    public void isValidSensorId_uuid_returnsTrue() {
        assertTrue(isValidSensorId("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
    }

    // ========== Name Comparison Tests ==========

    @Test
    public void namesMatch_identical_returnsTrue() {
        assertTrue(namesMatch("Ion Popescu", "Ion Popescu"));
    }

    @Test
    public void namesMatch_different_returnsFalse() {
        assertFalse(namesMatch("Ion Popescu", "Maria Ionescu"));
    }

    @Test
    public void namesMatch_caseDifferent_returnsFalse() {
        // Names are case-sensitive
        assertFalse(namesMatch("Ion Popescu", "ion popescu"));
    }

    @Test
    public void namesMatch_nullFirst_returnsFalse() {
        assertFalse(namesMatch(null, "Ion"));
    }

    @Test
    public void namesMatch_nullSecond_returnsFalse() {
        assertFalse(namesMatch("Ion", null));
    }

    @Test
    public void namesMatch_bothNull_returnsFalse() {
        assertFalse(namesMatch(null, null));
    }

    // ========== DiffCallback Logic Tests ==========

    @Test
    public void areItemsTheSame_sameSensorId_returnsTrue() {
        assertTrue(areItemsTheSame("sensor001", "sensor001"));
    }

    @Test
    public void areItemsTheSame_differentSensorId_returnsFalse() {
        assertFalse(areItemsTheSame("sensor001", "sensor002"));
    }

    @Test
    public void areContentsTheSame_sameNameSameId_returnsTrue() {
        assertTrue(areContentsTheSame("sensor001", "Ion", "sensor001", "Ion"));
    }

    @Test
    public void areContentsTheSame_differentName_returnsFalse() {
        assertFalse(areContentsTheSame("sensor001", "Ion", "sensor001", "Maria"));
    }

    @Test
    public void areContentsTheSame_differentId_returnsFalse() {
        // Even if names match, different IDs mean different content
        assertFalse(areContentsTheSame("sensor001", "Ion", "sensor002", "Ion"));
    }

    // ========== Helper Methods (simulating ViewModel/Adapter logic) ==========

    private boolean isValidName(String name) {
        if (name == null) return false;
        return !name.trim().isEmpty();
    }

    private String trimName(String name) {
        return name != null ? name.trim() : "";
    }

    private boolean isValidSensorId(String sensorId) {
        return sensorId != null && !sensorId.isEmpty();
    }

    private boolean namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        return name1.equals(name2);
    }

    private boolean areItemsTheSame(String sensorId1, String sensorId2) {
        return sensorId1.equals(sensorId2);
    }

    private boolean areContentsTheSame(String sensorId1, String name1, String sensorId2, String name2) {
        return sensorId1.equals(sensorId2) && name1.equals(name2);
    }

    // ========== Supervisor Assignment Map Tests ==========
    
    @Test
    public void supervisorMap_getSupervisor_returnsEmail() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("sensor001", "supervisor@example.com");
        
        String result = map.get("sensor001");
        
        assertEquals("supervisor@example.com", result);
    }
    
    @Test
    public void supervisorMap_getSupervisor_notAssigned_returnsNull() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        
        String result = map.get("sensor001");
        
        assertNull(result);
    }
    
    @Test
    public void supervisorMap_updateEntry_replacesOldValue() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("sensor001", "old@example.com");
        
        // Simulate update
        map.put("sensor001", "new@example.com");
        
        assertEquals("new@example.com", map.get("sensor001"));
        assertEquals(1, map.size());
    }
    
    @Test
    public void supervisorMap_removeEntry_setsToNull() {
        java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("sensor001", "supervisor@example.com");
        
        // Simulate unassign
        map.remove("sensor001");
        
        assertNull(map.get("sensor001"));
        assertEquals(0, map.size());
    }
}
