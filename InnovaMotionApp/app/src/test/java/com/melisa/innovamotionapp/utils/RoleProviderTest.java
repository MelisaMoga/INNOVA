package com.melisa.innovamotionapp.utils;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for RoleProvider
 * Tests role detection and validation after supervised → aggregator rename
 */
public class RoleProviderTest {
    
    @Before
    public void setUp() {
        // Clear any existing role
        GlobalData.getInstance().setCurrentUserRole(null);
    }
    
    @After
    public void tearDown() {
        // Clean up after tests
        GlobalData.getInstance().setCurrentUserRole(null);
    }
    
    @Test
    public void testGetCurrentRole_Aggregator() {
        // Test: "aggregator" role returns AGGREGATOR
        GlobalData.getInstance().setCurrentUserRole("aggregator");
        
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        assertEquals(RoleProvider.Role.AGGREGATOR, role);
    }
    
    @Test
    public void testGetCurrentRole_SupervisedLegacy() {
        // Test: "supervised" (legacy) role returns AGGREGATOR
        GlobalData.getInstance().setCurrentUserRole("supervised");
        
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        assertEquals(RoleProvider.Role.AGGREGATOR, role);
    }
    
    @Test
    public void testGetCurrentRole_Supervisor() {
        // Test: "supervisor" role returns SUPERVISOR
        GlobalData.getInstance().setCurrentUserRole("supervisor");
        
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        assertEquals(RoleProvider.Role.SUPERVISOR, role);
    }
    
    @Test
    public void testGetCurrentRole_Unknown() {
        // Test: unknown role returns UNKNOWN
        GlobalData.getInstance().setCurrentUserRole("invalid_role");
        
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        assertEquals(RoleProvider.Role.UNKNOWN, role);
    }
    
    @Test
    public void testGetCurrentRole_Null() {
        // Test: null role returns UNKNOWN
        GlobalData.getInstance().setCurrentUserRole(null);
        
        RoleProvider.Role role = RoleProvider.getCurrentRole();
        
        assertEquals(RoleProvider.Role.UNKNOWN, role);
    }
    
    @Test
    public void testGetCurrentRole_CaseInsensitive() {
        // Test: role matching is case insensitive
        GlobalData.getInstance().setCurrentUserRole("AGGREGATOR");
        assertEquals(RoleProvider.Role.AGGREGATOR, RoleProvider.getCurrentRole());
        
        GlobalData.getInstance().setCurrentUserRole("Supervisor");
        assertEquals(RoleProvider.Role.SUPERVISOR, RoleProvider.getCurrentRole());
        
        GlobalData.getInstance().setCurrentUserRole("SUPERVISED");
        assertEquals(RoleProvider.Role.AGGREGATOR, RoleProvider.getCurrentRole());
    }
    
    @Test
    public void testIsAggregator_True() {
        // Test: isAggregator() returns true for "aggregator"
        GlobalData.getInstance().setCurrentUserRole("aggregator");
        assertTrue(RoleProvider.isAggregator());
        
        // Test: isAggregator() returns true for "supervised" (legacy)
        GlobalData.getInstance().setCurrentUserRole("supervised");
        assertTrue(RoleProvider.isAggregator());
    }
    
    @Test
    public void testIsAggregator_False() {
        // Test: isAggregator() returns false for non-aggregator roles
        GlobalData.getInstance().setCurrentUserRole("supervisor");
        assertFalse(RoleProvider.isAggregator());
        
        GlobalData.getInstance().setCurrentUserRole("unknown");
        assertFalse(RoleProvider.isAggregator());
        
        GlobalData.getInstance().setCurrentUserRole(null);
        assertFalse(RoleProvider.isAggregator());
    }
    
    @Test
    public void testIsSupervisor_True() {
        // Test: isSupervisor() returns true only for "supervisor"
        GlobalData.getInstance().setCurrentUserRole("supervisor");
        assertTrue(RoleProvider.isSupervisor());
    }
    
    @Test
    public void testIsSupervisor_False() {
        // Test: isSupervisor() returns false for non-supervisor roles
        GlobalData.getInstance().setCurrentUserRole("aggregator");
        assertFalse(RoleProvider.isSupervisor());
        
        GlobalData.getInstance().setCurrentUserRole("supervised");
        assertFalse(RoleProvider.isSupervisor());
        
        GlobalData.getInstance().setCurrentUserRole("unknown");
        assertFalse(RoleProvider.isSupervisor());
        
        GlobalData.getInstance().setCurrentUserRole(null);
        assertFalse(RoleProvider.isSupervisor());
    }
    
    @Test
    public void testRoleEnumValues() {
        // Test: Role enum has all expected values
        RoleProvider.Role[] values = RoleProvider.Role.values();
        
        assertTrue(values.length >= 3);
        boolean hasAggregator = false;
        boolean hasSupervisor = false;
        boolean hasUnknown = false;
        
        for (RoleProvider.Role role : values) {
            if (role == RoleProvider.Role.AGGREGATOR) hasAggregator = true;
            if (role == RoleProvider.Role.SUPERVISOR) hasSupervisor = true;
            if (role == RoleProvider.Role.UNKNOWN) hasUnknown = true;
        }
        
        assertTrue("AGGREGATOR enum missing", hasAggregator);
        assertTrue("SUPERVISOR enum missing", hasSupervisor);
        assertTrue("UNKNOWN enum missing", hasUnknown);
    }
}

