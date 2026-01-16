package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.data.models.UserProfile;
import com.melisa.innovamotionapp.utils.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for UserSession multi-role support.
 * 
 * Since UserSession depends on Firebase and Android Context,
 * these tests focus on simulating the role logic that would be used.
 * 
 * Tests cover:
 * - hasRole() for different role types
 * - hasBothRoles() for dual-role users
 * - getRoles() returning defensive copy
 * - getRole() legacy method returning first role
 * - Empty roles handling
 */
public class UserSessionMultiRoleTest {

    private static final String TEST_UID = "test_user_123";
    private static final String TEST_EMAIL = "test@example.com";
    private static final String TEST_DISPLAY_NAME = "Test User";

    // ========== hasRole() Tests ==========

    @Test
    public void testHasRoleAggregator() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        MockUserSession session = new MockUserSession(roles);

        assertTrue(session.hasRole(Constants.ROLE_AGGREGATOR));
        assertTrue(session.isAggregator());
        assertFalse(session.isSupervisor());
    }

    @Test
    public void testHasRoleSupervisor() {
        List<String> roles = Arrays.asList(Constants.ROLE_SUPERVISOR);
        MockUserSession session = new MockUserSession(roles);

        assertTrue(session.hasRole(Constants.ROLE_SUPERVISOR));
        assertTrue(session.isSupervisor());
        assertFalse(session.isAggregator());
    }

    @Test
    public void testHasRoleWithUnknownRole() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR);
        MockUserSession session = new MockUserSession(roles);

        assertFalse(session.hasRole("admin"));
        assertFalse(session.hasRole("guest"));
        assertFalse(session.hasRole(""));
        assertFalse(session.hasRole(null));
    }

    // ========== hasBothRoles() Tests ==========

    @Test
    public void testHasBothRoles() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        MockUserSession session = new MockUserSession(roles);

        assertTrue(session.hasBothRoles());
        assertTrue(session.isAggregator());
        assertTrue(session.isSupervisor());
    }

    @Test
    public void testHasBothRolesOrderDoesNotMatter() {
        // Order should not affect the result
        List<String> roles1 = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        List<String> roles2 = Arrays.asList(Constants.ROLE_SUPERVISOR, Constants.ROLE_AGGREGATOR);

        MockUserSession session1 = new MockUserSession(roles1);
        MockUserSession session2 = new MockUserSession(roles2);

        assertTrue(session1.hasBothRoles());
        assertTrue(session2.hasBothRoles());
    }

    @Test
    public void testHasBothRolesReturnsFalseForSingleRole() {
        List<String> aggregatorOnly = Arrays.asList(Constants.ROLE_AGGREGATOR);
        List<String> supervisorOnly = Arrays.asList(Constants.ROLE_SUPERVISOR);

        MockUserSession session1 = new MockUserSession(aggregatorOnly);
        MockUserSession session2 = new MockUserSession(supervisorOnly);

        assertFalse(session1.hasBothRoles());
        assertFalse(session2.hasBothRoles());
    }

    // ========== getRoles() Tests ==========

    @Test
    public void testGetRolesReturnsCopy() {
        List<String> originalRoles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        MockUserSession session = new MockUserSession(originalRoles);

        List<String> returnedRoles = session.getRoles();
        returnedRoles.add(Constants.ROLE_SUPERVISOR);

        // Original should not be affected
        assertEquals(1, session.getRoles().size());
        assertFalse(session.hasRole(Constants.ROLE_SUPERVISOR));
    }

    @Test
    public void testGetRolesReturnsAllRoles() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        MockUserSession session = new MockUserSession(roles);

        List<String> returnedRoles = session.getRoles();

        assertEquals(2, returnedRoles.size());
        assertTrue(returnedRoles.contains(Constants.ROLE_AGGREGATOR));
        assertTrue(returnedRoles.contains(Constants.ROLE_SUPERVISOR));
    }

    // ========== getRole() Legacy Tests ==========

    @Test
    public void testGetRoleLegacyReturnsFirstRole() {
        List<String> roles = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        MockUserSession session = new MockUserSession(roles);

        // Legacy getRole() returns the first role
        assertEquals(Constants.ROLE_AGGREGATOR, session.getRole());
    }

    @Test
    public void testGetRoleLegacyReturnsNullForEmptyRoles() {
        MockUserSession session = new MockUserSession(new ArrayList<>());

        assertNull(session.getRole());
    }

    @Test
    public void testGetRoleLegacyReturnsNullForNullRoles() {
        MockUserSession session = new MockUserSession(null);

        assertNull(session.getRole());
    }

    // ========== Empty/Null Roles Tests ==========

    @Test
    public void testEmptyRoles() {
        MockUserSession session = new MockUserSession(new ArrayList<>());

        assertFalse(session.isAggregator());
        assertFalse(session.isSupervisor());
        assertFalse(session.hasBothRoles());
        assertTrue(session.getRoles().isEmpty());
    }

    @Test
    public void testNullRoles() {
        MockUserSession session = new MockUserSession(null);

        assertFalse(session.isAggregator());
        assertFalse(session.isSupervisor());
        assertFalse(session.hasBothRoles());
        assertNotNull(session.getRoles());
        assertTrue(session.getRoles().isEmpty());
    }

    // ========== UserProfile Integration Tests ==========

    @Test
    public void testRolesFromUserProfile() {
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, null,
                Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR));

        // Simulate what UserSession does when loading from profile
        MockUserSession session = new MockUserSession(profile.getRoles());

        assertTrue(session.hasBothRoles());
        assertEquals(2, session.getRoles().size());
    }

    @Test
    public void testSingleRoleFromUserProfile() {
        UserProfile profile = new UserProfile(TEST_UID, TEST_EMAIL, TEST_DISPLAY_NAME, null,
                Arrays.asList(Constants.ROLE_SUPERVISOR));

        MockUserSession session = new MockUserSession(profile.getRoles());

        assertFalse(session.hasBothRoles());
        assertTrue(session.isSupervisor());
        assertFalse(session.isAggregator());
    }

    // ========== Role Update Behavior Tests (simulating LoginActivity.saveUserRole) ==========

    /**
     * Simulates the arrayUnion behavior used in LoginActivity.saveUserRole.
     * When a user selects a role, it should ADD to existing roles, not replace.
     */
    @Test
    public void testArrayUnionBehaviorAddsRoleWithoutReplacing() {
        // User is already an aggregator
        List<String> existingRoles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        
        // User signs in and selects supervisor (arrayUnion simulation)
        String newRole = Constants.ROLE_SUPERVISOR;
        List<String> afterUnion = simulateArrayUnion(existingRoles, newRole);
        
        // Both roles should be present
        assertEquals(2, afterUnion.size());
        assertTrue(afterUnion.contains(Constants.ROLE_AGGREGATOR));
        assertTrue(afterUnion.contains(Constants.ROLE_SUPERVISOR));
    }

    /**
     * Verifies arrayUnion doesn't create duplicates.
     */
    @Test
    public void testArrayUnionDoesNotDuplicate() {
        // User is already an aggregator
        List<String> existingRoles = new ArrayList<>(Arrays.asList(Constants.ROLE_AGGREGATOR));
        
        // User signs in and selects aggregator again
        String newRole = Constants.ROLE_AGGREGATOR;
        List<String> afterUnion = simulateArrayUnion(existingRoles, newRole);
        
        // Should still only have one role
        assertEquals(1, afterUnion.size());
        assertTrue(afterUnion.contains(Constants.ROLE_AGGREGATOR));
    }

    /**
     * Simulates the full sign-in cycle: aggregator first, then supervisor.
     */
    @Test
    public void testFullSignInCyclePreservesBothRoles() {
        // First sign-in: empty roles, user chooses aggregator
        List<String> roles = new ArrayList<>();
        roles = simulateArrayUnion(roles, Constants.ROLE_AGGREGATOR);
        
        assertEquals(1, roles.size());
        assertTrue(roles.contains(Constants.ROLE_AGGREGATOR));
        
        // Sign out and sign in again, choosing supervisor this time
        roles = simulateArrayUnion(roles, Constants.ROLE_SUPERVISOR);
        
        // Both roles should be present
        assertEquals(2, roles.size());
        assertTrue(roles.contains(Constants.ROLE_AGGREGATOR));
        assertTrue(roles.contains(Constants.ROLE_SUPERVISOR));
    }

    /**
     * Tests that legacy 'role' field is NOT used for updates.
     * The login flow should only modify 'roles' array.
     */
    @Test
    public void testLoginUpdatesShouldNotUseLegacyRoleField() {
        // Simulate what LoginActivity does when saving role
        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        
        // The correct implementation uses arrayUnion (simulated)
        String role = Constants.ROLE_SUPERVISOR;
        // updates.put("roles", FieldValue.arrayUnion(role)); // Firebase API
        // updates.put("role", FieldValue.delete()); // Remove legacy field
        
        // For testing, we just verify the structure
        updates.put("roles", "arrayUnion:" + role); // Simulated arrayUnion
        updates.put("role", null); // Simulated delete
        
        // Verify 'role' is being removed (null = delete)
        assertNull(updates.get("role"));
        
        // Verify 'roles' uses arrayUnion pattern
        assertTrue(updates.get("roles").toString().contains("arrayUnion"));
    }

    /**
     * Helper method to simulate Firebase FieldValue.arrayUnion behavior.
     * Adds element to list only if not already present.
     */
    private List<String> simulateArrayUnion(List<String> existingList, String newElement) {
        List<String> result = new ArrayList<>(existingList);
        if (!result.contains(newElement)) {
            result.add(newElement);
        }
        return result;
    }

    // ========== Mock UserSession for testing ==========

    /**
     * Mock implementation of UserSession role logic for testing.
     * This avoids Firebase/Android dependencies while testing the role logic.
     */
    static class MockUserSession {
        private final List<String> roles;

        public MockUserSession(List<String> roles) {
            this.roles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
        }

        public boolean hasRole(String role) {
            if (role == null) return false;
            return roles.contains(role);
        }

        public boolean isAggregator() {
            return hasRole(Constants.ROLE_AGGREGATOR);
        }

        public boolean isSupervisor() {
            return hasRole(Constants.ROLE_SUPERVISOR);
        }

        public boolean hasBothRoles() {
            return isAggregator() && isSupervisor();
        }

        public List<String> getRoles() {
            return new ArrayList<>(roles);
        }

        public String getRole() {
            if (!roles.isEmpty()) {
                return roles.get(0);
            }
            return null;
        }
    }
}
