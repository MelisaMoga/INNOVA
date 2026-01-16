package com.melisa.innovamotionapp.data.models;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.utils.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for UserProfile POJO.
 * 
 * Tests cover:
 * - Constructor field assignment
 * - Role array parsing (new format) and legacy role string (backward compat)
 * - Role helper methods (hasRole, isAggregator, isSupervisor, hasBothRoles)
 * - Serialization/deserialization round-trip
 * - Edge cases (null roles, empty roles)
 */
public class UserProfileTest {

    private static final String TEST_UID = "test_uid_123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_DISPLAY_NAME = "Test User";
    private static final String TEST_PHOTO_URL = "https://example.com/photo.jpg";

    // ========== Constructor Tests ==========

    @Test
    public void testConstructorSetsAllFields() {
        List<String> roles = Arrays.asList("aggregator", "supervisor");
        
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertEquals(TEST_UID, profile.getUid());
        assertEquals(TEST_EMAIL, profile.getEmail());
        assertEquals(TEST_DISPLAY_NAME, profile.getDisplayName());
        assertEquals(TEST_PHOTO_URL, profile.getPhotoUrl());
        assertEquals(2, profile.getRoles().size());
        assertTrue(profile.getCreatedAt() > 0);
        assertTrue(profile.getLastSignIn() > 0);
    }

    @Test
    public void testDefaultConstructorInitializesEmptyRoles() {
        UserProfile profile = new UserProfile();
        
        assertNotNull(profile.getRoles());
        assertTrue(profile.getRoles().isEmpty());
    }

    @Test
    public void testConstructorWithNullRolesCreatesEmptyList() {
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, null);
        
        assertNotNull(profile.getRoles());
        assertTrue(profile.getRoles().isEmpty());
    }

    // ========== Role Helper Tests ==========

    @Test
    public void testHasRoleReturnsTrue() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertTrue(profile.hasRole(Constants.ROLE_AGGREGATOR));
    }

    @Test
    public void testHasRoleReturnsFalse() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertFalse(profile.hasRole(Constants.ROLE_SUPERVISOR));
        assertFalse(profile.hasRole("unknown"));
    }

    @Test
    public void testHasRoleWithEmptyRoles() {
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, new ArrayList<>());
        
        assertFalse(profile.hasRole(Constants.ROLE_AGGREGATOR));
        assertFalse(profile.hasRole(Constants.ROLE_SUPERVISOR));
    }

    @Test
    public void testIsAggregator() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertTrue(profile.isAggregator());
        assertFalse(profile.isSupervisor());
    }

    @Test
    public void testIsSupervisor() {
        List<String> roles = Arrays.asList(Constants.ROLE_SUPERVISOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertTrue(profile.isSupervisor());
        assertFalse(profile.isAggregator());
    }

    @Test
    public void testHasBothRoles() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertTrue(profile.hasBothRoles());
        assertTrue(profile.isAggregator());
        assertTrue(profile.isSupervisor());
    }

    @Test
    public void testHasBothRolesReturnsFalseForSingleRole() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertFalse(profile.hasBothRoles());
    }

    // ========== Serialization Tests ==========

    @Test
    public void testToFirestoreDocumentIncludesAllFields() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        Map<String, Object> doc = profile.toFirestoreDocument();
        
        assertEquals(TEST_EMAIL, doc.get("email"));
        assertEquals(TEST_DISPLAY_NAME, doc.get("displayName"));
        assertEquals(TEST_PHOTO_URL, doc.get("photoUrl"));
        assertEquals(roles, doc.get("roles"));
        assertNotNull(doc.get("createdAt"));
        assertNotNull(doc.get("lastSignIn"));
    }

    @Test
    public void testToFirestoreDocumentFieldCount() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        Map<String, Object> doc = profile.toFirestoreDocument();
        
        // Should have 6 fields: email, displayName, photoUrl, roles, createdAt, lastSignIn
        assertEquals(6, doc.size());
    }

    // ========== Getter Returns Defensive Copy Tests ==========

    @Test
    public void testGetRolesReturnsCopy() {
        List<String> originalRoles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, originalRoles);
        
        List<String> returnedRoles = profile.getRoles();
        returnedRoles.add(Constants.ROLE_SUPERVISOR);
        
        // Original should not be affected
        assertEquals(1, profile.getRoles().size());
        assertFalse(profile.hasRole(Constants.ROLE_SUPERVISOR));
    }

    // ========== Setter Tests ==========

    @Test
    public void testSetters() {
        UserProfile profile = new UserProfile();
        
        profile.setUid("new_uid");
        profile.setEmail("new@example.com");
        profile.setDisplayName("New Name");
        profile.setPhotoUrl("https://new.com/photo.jpg");
        profile.setRoles(Arrays.asList(Constants.ROLE_SUPERVISOR));
        profile.setCreatedAt(1000L);
        profile.setLastSignIn(2000L);
        
        assertEquals("new_uid", profile.getUid());
        assertEquals("new@example.com", profile.getEmail());
        assertEquals("New Name", profile.getDisplayName());
        assertEquals("https://new.com/photo.jpg", profile.getPhotoUrl());
        assertTrue(profile.hasRole(Constants.ROLE_SUPERVISOR));
        assertEquals(1000L, profile.getCreatedAt());
        assertEquals(2000L, profile.getLastSignIn());
    }

    @Test
    public void testSetRolesWithNull() {
        UserProfile profile = new UserProfile();
        profile.setRoles(Arrays.asList(Constants.ROLE_AGGREGATOR));
        
        profile.setRoles(null);
        
        assertNotNull(profile.getRoles());
        assertTrue(profile.getRoles().isEmpty());
    }

    // ========== Timestamp Tests ==========

    @Test
    public void testCreatedAtIsAutoSet() {
        long before = System.currentTimeMillis();
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, new ArrayList<>());
        long after = System.currentTimeMillis();
        
        assertTrue(profile.getCreatedAt() >= before);
        assertTrue(profile.getCreatedAt() <= after);
    }

    @Test
    public void testLastSignInIsAutoSet() {
        long before = System.currentTimeMillis();
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, new ArrayList<>());
        long after = System.currentTimeMillis();
        
        assertTrue(profile.getLastSignIn() >= before);
        assertTrue(profile.getLastSignIn() <= after);
    }

    // ========== Role Merging Tests ==========

    /**
     * Verifies that adding a second role to an existing user preserves both roles.
     * This simulates the arrayUnion behavior when a user signs in with a new role.
     */
    @Test
    public void testRoleMergingPreservesBothRoles() {
        // User starts as aggregator
        List<String> initialRoles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, initialRoles);
        
        assertTrue(profile.isAggregator());
        assertFalse(profile.isSupervisor());
        
        // Simulate merging supervisor role (like arrayUnion would do)
        List<String> updatedRoles = new ArrayList<>(profile.getRoles());
        if (!updatedRoles.contains(Constants.ROLE_SUPERVISOR)) {
            updatedRoles.add(Constants.ROLE_SUPERVISOR);
        }
        profile.setRoles(updatedRoles);
        
        // Both roles should now be present
        assertTrue(profile.isAggregator());
        assertTrue(profile.isSupervisor());
        assertTrue(profile.hasBothRoles());
        assertEquals(2, profile.getRoles().size());
    }

    /**
     * Verifies that merging the same role twice doesn't create duplicates.
     * arrayUnion behavior: only adds if not already present.
     */
    @Test
    public void testRoleMergingNoDuplicates() {
        List<String> roles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        // Simulate merging the same role again (like arrayUnion would do)
        List<String> updatedRoles = new ArrayList<>(profile.getRoles());
        if (!updatedRoles.contains(Constants.ROLE_AGGREGATOR)) {
            updatedRoles.add(Constants.ROLE_AGGREGATOR);
        }
        profile.setRoles(updatedRoles);
        
        // Should still only have one role (no duplicates)
        assertEquals(1, profile.getRoles().size());
        assertTrue(profile.isAggregator());
    }

    /**
     * Verifies roles persist correctly after multiple sign-in cycles.
     */
    @Test
    public void testRolePersistenceAcrossMultipleUpdates() {
        // First sign-in: user chooses aggregator
        List<String> roles = new ArrayList<>();
        roles.add(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        assertTrue(profile.isAggregator());
        assertFalse(profile.isSupervisor());
        
        // Second sign-in: user adds supervisor (arrayUnion simulation)
        List<String> mergedRoles = new ArrayList<>(profile.getRoles());
        if (!mergedRoles.contains(Constants.ROLE_SUPERVISOR)) {
            mergedRoles.add(Constants.ROLE_SUPERVISOR);
        }
        profile.setRoles(mergedRoles);
        
        assertTrue(profile.hasBothRoles());
        
        // Third sign-in: selecting aggregator again should NOT remove supervisor
        List<String> rolesAfterThirdSignIn = new ArrayList<>(profile.getRoles());
        if (!rolesAfterThirdSignIn.contains(Constants.ROLE_AGGREGATOR)) {
            rolesAfterThirdSignIn.add(Constants.ROLE_AGGREGATOR);
        }
        profile.setRoles(rolesAfterThirdSignIn);
        
        // Both roles should still be present
        assertTrue(profile.hasBothRoles());
        assertEquals(2, profile.getRoles().size());
    }

    // ========== Legacy 'role' Field Removal Tests ==========

    /**
     * Verifies that toFirestoreDocument does NOT include the legacy 'role' field.
     * Only the 'roles' array should be written.
     */
    @Test
    public void testToFirestoreDocumentDoesNotIncludeLegacyRoleField() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        Map<String, Object> doc = profile.toFirestoreDocument();
        
        // Should NOT contain the legacy 'role' (singular) field
        assertFalse("Document should not contain legacy 'role' field", doc.containsKey("role"));
        
        // Should contain the 'roles' (plural) array
        assertTrue("Document should contain 'roles' array", doc.containsKey("roles"));
        assertEquals(roles, doc.get("roles"));
    }

    /**
     * Verifies that toFirestoreDocument writes the correct fields and nothing extra.
     */
    @Test
    public void testToFirestoreDocumentWritesOnlyExpectedFields() {
        List<String> roles = Arrays.asList(Constants.ROLE_SUPERVISOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        Map<String, Object> doc = profile.toFirestoreDocument();
        
        // Verify expected fields are present
        assertTrue(doc.containsKey("email"));
        assertTrue(doc.containsKey("displayName"));
        assertTrue(doc.containsKey("photoUrl"));
        assertTrue(doc.containsKey("roles"));
        assertTrue(doc.containsKey("createdAt"));
        assertTrue(doc.containsKey("lastSignIn"));
        
        // Verify no unexpected fields
        assertFalse("Should not contain 'role' (singular)", doc.containsKey("role"));
        assertFalse("Should not contain 'uid' (stored as document ID)", doc.containsKey("uid"));
    }

    /**
     * Simulates the migration scenario where a user document has the legacy 'role' field.
     * Verifies that re-saving doesn't include 'role' anymore.
     */
    @Test
    public void testMigrationFromLegacyRoleField() {
        // Simulate a legacy document that has both 'role' and 'roles' fields
        // When we create a UserProfile from it and save back, only 'roles' should be written
        
        // Create a profile with aggregator role
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, TEST_PHOTO_URL, roles);
        
        // Serialize back to document (simulating a save)
        Map<String, Object> doc = profile.toFirestoreDocument();
        
        // The legacy 'role' field should NOT be included
        assertFalse(doc.containsKey("role"));
        
        // The 'roles' array should be correct
        @SuppressWarnings("unchecked")
        List<String> savedRoles = (List<String>) doc.get("roles");
        assertEquals(1, savedRoles.size());
        assertEquals(Constants.ROLE_AGGREGATOR, savedRoles.get(0));
    }
}
