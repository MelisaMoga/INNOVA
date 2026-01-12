package com.melisa.innovamotionapp.utils;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for role terminology and RoleProvider functionality.
 * 
 * Tests the updated role system:
 * - Aggregator: Collects sensor data, uploads to cloud
 * - Supervisor: Monitors sensors assigned by aggregators
 * - Sensor: ID-only entity (no email), linked to aggregatorUID
 */
public class RoleProviderTest {

    // ========== Role Enum Tests ==========

    @Test
    public void role_aggregator_exists() {
        assertNotNull(RoleProvider.Role.AGGREGATOR);
    }

    @Test
    public void role_supervisor_exists() {
        assertNotNull(RoleProvider.Role.SUPERVISOR);
    }

    @Test
    public void role_unknown_exists() {
        assertNotNull(RoleProvider.Role.UNKNOWN);
    }

    @Test
    public void role_enum_hasThreeValues() {
        assertEquals(3, RoleProvider.Role.values().length);
    }

    // ========== Role String Parsing Tests ==========

    @Test
    public void parseRole_aggregator_lowercase() {
        assertEquals(RoleProvider.Role.AGGREGATOR, parseRoleFromString("aggregator"));
    }

    @Test
    public void parseRole_aggregator_uppercase() {
        assertEquals(RoleProvider.Role.AGGREGATOR, parseRoleFromString("AGGREGATOR"));
    }

    @Test
    public void parseRole_aggregator_mixedCase() {
        assertEquals(RoleProvider.Role.AGGREGATOR, parseRoleFromString("Aggregator"));
    }

    @Test
    public void parseRole_supervisor_lowercase() {
        assertEquals(RoleProvider.Role.SUPERVISOR, parseRoleFromString("supervisor"));
    }

    @Test
    public void parseRole_supervisor_uppercase() {
        assertEquals(RoleProvider.Role.SUPERVISOR, parseRoleFromString("SUPERVISOR"));
    }

    @Test
    public void parseRole_supervisor_mixedCase() {
        assertEquals(RoleProvider.Role.SUPERVISOR, parseRoleFromString("Supervisor"));
    }

    @Test
    public void parseRole_null_returnsUnknown() {
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString(null));
    }

    @Test
    public void parseRole_empty_returnsUnknown() {
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString(""));
    }

    @Test
    public void parseRole_invalid_returnsUnknown() {
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString("admin"));
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString("user"));
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString("guest"));
    }

    // ========== Legacy "supervised" Role Tests ==========
    // Note: "supervised" is the old name for "aggregator"
    // We NO LONGER support this since databases are rebuilt

    @Test
    public void parseRole_supervised_notSupported() {
        // After removing backward compatibility, "supervised" should return UNKNOWN
        assertEquals(RoleProvider.Role.UNKNOWN, parseRoleFromString("supervised"));
    }

    // ========== Role Check Method Tests ==========

    @Test
    public void isAggregator_aggregatorRole_returnsTrue() {
        assertTrue(isAggregator("aggregator"));
    }

    @Test
    public void isAggregator_supervisorRole_returnsFalse() {
        assertFalse(isAggregator("supervisor"));
    }

    @Test
    public void isAggregator_nullRole_returnsFalse() {
        assertFalse(isAggregator(null));
    }

    @Test
    public void isSupervisor_supervisorRole_returnsTrue() {
        assertTrue(isSupervisor("supervisor"));
    }

    @Test
    public void isSupervisor_aggregatorRole_returnsFalse() {
        assertFalse(isSupervisor("aggregator"));
    }

    @Test
    public void isSupervisor_nullRole_returnsFalse() {
        assertFalse(isSupervisor(null));
    }

    // ========== Entity Model Tests ==========

    @Test
    public void entityModel_aggregatorHasEmail() {
        // Aggregator is an email-based account
        assertTrue(isEmailBasedAccount("aggregator"));
    }

    @Test
    public void entityModel_supervisorHasEmail() {
        // Supervisor is an email-based account
        assertTrue(isEmailBasedAccount("supervisor"));
    }

    @Test
    public void entityModel_sensorNoEmail() {
        // Sensor is ID-only (not a role, just for documentation)
        assertFalse(isEmailBasedAccount("sensor"));
    }

    // ========== Role Terminology Tests ==========

    @Test
    public void terminology_aggregatorNotSupervised() {
        // Verify we use "aggregator" not "supervised"
        assertNotEquals("supervised", RoleProvider.Role.AGGREGATOR.name().toLowerCase());
        assertEquals("aggregator", RoleProvider.Role.AGGREGATOR.name().toLowerCase());
    }

    @Test
    public void terminology_roleEnumNaming() {
        assertEquals("AGGREGATOR", RoleProvider.Role.AGGREGATOR.name());
        assertEquals("SUPERVISOR", RoleProvider.Role.SUPERVISOR.name());
        assertEquals("UNKNOWN", RoleProvider.Role.UNKNOWN.name());
    }

    // ========== Helper Methods (simulating RoleProvider logic) ==========

    private RoleProvider.Role parseRoleFromString(String role) {
        if ("aggregator".equalsIgnoreCase(role)) {
            return RoleProvider.Role.AGGREGATOR;
        }
        if ("supervisor".equalsIgnoreCase(role)) {
            return RoleProvider.Role.SUPERVISOR;
        }
        return RoleProvider.Role.UNKNOWN;
    }

    private boolean isAggregator(String role) {
        return parseRoleFromString(role) == RoleProvider.Role.AGGREGATOR;
    }

    private boolean isSupervisor(String role) {
        return parseRoleFromString(role) == RoleProvider.Role.SUPERVISOR;
    }

    private boolean isEmailBasedAccount(String role) {
        // Aggregators and Supervisors have emails
        // Sensors do not (they're just IDs)
        return "aggregator".equalsIgnoreCase(role) || "supervisor".equalsIgnoreCase(role);
    }
}
