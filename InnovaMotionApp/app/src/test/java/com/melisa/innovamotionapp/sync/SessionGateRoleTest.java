package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import com.melisa.innovamotionapp.utils.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Unit tests for SessionGate role handling logic.
 * 
 * These tests verify that:
 * 1. Explicitly selected roles (via RoleSelectionActivity) are preserved during bootstrap
 * 2. Default role fallback works correctly for cold starts
 * 3. Role order in the profile doesn't affect the user's explicit selection
 * 
 * Since SessionGate depends on Android Context and Firebase, these tests use a 
 * mock implementation to test the logic in isolation.
 */
public class SessionGateRoleTest {

    // ========== Role Preservation Tests ==========

    /**
     * When user explicitly selects "supervisor" in RoleSelectionActivity,
     * the role should be preserved even if their profile lists "aggregator" first.
     */
    @Test
    public void testUpdateSessionCache_PreservesExistingRole() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // User explicitly selected supervisor in RoleSelectionActivity
        globalData.setCurrentUserRole(Constants.ROLE_SUPERVISOR);
        
        // Session reloads with roles from Firestore (aggregator is first)
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Role should remain "supervisor" (user's selection preserved)
        assertEquals("Explicit role selection should be preserved",
                Constants.ROLE_SUPERVISOR, globalData.getCurrentUserRole());
    }

    /**
     * When user explicitly selects "aggregator" in RoleSelectionActivity,
     * the role should be preserved even if their profile lists "supervisor" first.
     */
    @Test
    public void testUpdateSessionCache_PreservesAggregatorSelection() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // User explicitly selected aggregator
        globalData.setCurrentUserRole(Constants.ROLE_AGGREGATOR);
        
        // Session reloads with supervisor first (unusual but possible)
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_SUPERVISOR, Constants.ROLE_AGGREGATOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Role should remain "aggregator"
        assertEquals("Explicit aggregator selection should be preserved",
                Constants.ROLE_AGGREGATOR, globalData.getCurrentUserRole());
    }

    // ========== Default Role Fallback Tests ==========

    /**
     * When no role is explicitly set (cold start), the first role in the list is used.
     */
    @Test
    public void testUpdateSessionCache_SetsDefaultRole_WhenNoExplicitSelection() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // No explicit role set (null)
        globalData.setCurrentUserRole(null);
        
        // Session loads with aggregator first
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Default to first role (aggregator)
        assertEquals("Should default to first role when no explicit selection",
                Constants.ROLE_AGGREGATOR, globalData.getCurrentUserRole());
    }

    /**
     * When role is empty string (edge case), treat as unset and use default.
     */
    @Test
    public void testUpdateSessionCache_SetsDefaultRole_WhenEmptyString() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // Empty string role (edge case)
        globalData.setCurrentUserRole("");
        
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_SUPERVISOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Default to first role (supervisor)
        assertEquals("Should default to first role when role is empty string",
                Constants.ROLE_SUPERVISOR, globalData.getCurrentUserRole());
    }

    /**
     * When roles list is empty, role should remain null (no default available).
     */
    @Test
    public void testUpdateSessionCache_NoRoles_StaysNull() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // No explicit role
        globalData.setCurrentUserRole(null);
        
        // Empty roles list (edge case)
        List<String> rolesFromFirestore = new ArrayList<>();
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Should remain null
        assertNull("Role should stay null when roles list is empty",
                globalData.getCurrentUserRole());
    }

    // ========== Bootstrap Flow Tests ==========

    /**
     * Simulates the full flow: RoleSelectionActivity sets role, then bootstrap is called.
     * The explicit selection must survive the bootstrap.
     */
    @Test
    public void testBootstrapFlow_PreservesRoleSelection() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // Step 1: User selects supervisor in RoleSelectionActivity
        globalData.setCurrentUserRole(Constants.ROLE_SUPERVISOR);
        
        // Step 2: RoleSelectionActivity calls reloadSessionAndBootstrap
        // This internally calls updateSessionCache with roles from Firestore
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_AGGREGATOR, Constants.ROLE_SUPERVISOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Step 3: Verify role is still supervisor
        assertEquals("Role should survive bootstrap", 
                Constants.ROLE_SUPERVISOR, globalData.getCurrentUserRole());
        
        // Step 4: RoleProvider would return supervisor
        boolean isSupervisor = Constants.ROLE_SUPERVISOR.equals(globalData.getCurrentUserRole());
        assertTrue("RoleProvider.isSupervisor() should return true", isSupervisor);
    }

    /**
     * Simulates first-time login where no role was explicitly selected.
     * Should use default (first role).
     */
    @Test
    public void testFirstTimeLogin_UsesDefaultRole() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // No explicit selection (first login, single role account)
        globalData.setCurrentUserRole(null);
        
        // User has only aggregator role
        List<String> rolesFromFirestore = Arrays.asList(Constants.ROLE_AGGREGATOR);
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Should default to aggregator
        assertEquals("First time login should use default role",
                Constants.ROLE_AGGREGATOR, globalData.getCurrentUserRole());
    }

    // ========== Edge Case Tests ==========

    /**
     * Null roles list should be handled gracefully.
     */
    @Test
    public void testUpdateSessionCache_NullRolesList() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        globalData.setCurrentUserRole(null);
        
        // Null roles list (edge case)
        sessionGate.updateSessionCache("user123", null);
        
        // Should remain null
        assertNull("Role should stay null when roles list is null",
                globalData.getCurrentUserRole());
    }

    /**
     * Role should be case-sensitive.
     */
    @Test
    public void testRoleCaseSensitivity() {
        MockGlobalData globalData = new MockGlobalData();
        MockSessionGate sessionGate = new MockSessionGate(globalData);
        
        // User set lowercase role
        globalData.setCurrentUserRole("supervisor");
        
        List<String> rolesFromFirestore = Arrays.asList("aggregator", "SUPERVISOR");
        sessionGate.updateSessionCache("user123", rolesFromFirestore);
        
        // Should preserve original case
        assertEquals("Role should preserve original case", 
                "supervisor", globalData.getCurrentUserRole());
    }

    // ========== Mock Classes ==========

    /**
     * Mock GlobalData for testing role logic without Android dependencies.
     */
    static class MockGlobalData {
        private String currentUserRole;
        private String currentUserUid;

        public String getCurrentUserRole() {
            return currentUserRole;
        }

        public void setCurrentUserRole(String role) {
            this.currentUserRole = role;
        }

        public void setCurrentUserUid(String uid) {
            this.currentUserUid = uid;
        }
    }

    /**
     * Mock SessionGate that tests updateSessionCache logic in isolation.
     */
    static class MockSessionGate {
        private final MockGlobalData globalData;
        private List<String> currentUserRoles;

        public MockSessionGate(MockGlobalData globalData) {
            this.globalData = globalData;
            this.currentUserRoles = new ArrayList<>();
        }

        /**
         * Mirrors the actual updateSessionCache logic from SessionGate.java.
         * This should match the production code.
         */
        public void updateSessionCache(String userId, List<String> roles) {
            // Cache session data locally
            this.currentUserRoles = roles != null ? new ArrayList<>(roles) : new ArrayList<>();
            
            // Update GlobalData singleton with session info
            globalData.setCurrentUserUid(userId);
            
            // Preserve existing role if explicitly set (e.g., by RoleSelectionActivity)
            String existingRole = globalData.getCurrentUserRole();
            if (existingRole == null || existingRole.isEmpty()) {
                // No role explicitly set - default to first role
                String primaryRole = !this.currentUserRoles.isEmpty() ? this.currentUserRoles.get(0) : null;
                globalData.setCurrentUserRole(primaryRole);
            }
            // else: Role was explicitly set - preserve it (do nothing)
        }
    }
}
