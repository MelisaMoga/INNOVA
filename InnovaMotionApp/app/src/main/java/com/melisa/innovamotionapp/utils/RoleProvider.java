package com.melisa.innovamotionapp.utils;

/**
 * Utility class for accessing the current user's role.
 * Provides a simple interface to get the cached role from GlobalData.
 * 
 * Role terminology:
 * - AGGREGATOR: Data collection phone (formerly "supervised")
 * - SUPERVISOR: Monitoring person
 */
public final class RoleProvider {
    public enum Role { 
        AGGREGATOR,  // Data collection phone (multi-user collector)
        SUPERVISOR,  // Monitoring person
        UNKNOWN 
    }

    private RoleProvider() {}

    /**
     * Get the current user's role from cached data.
     * This assumes the role is already cached in GlobalData after login.
     * 
     * @return The current user's role, or UNKNOWN if not available
     */
    public static Role getCurrentRole() {
        // Get role from GlobalData (should be cached after login)
        String role = GlobalData.getInstance().currentUserRole;
        
        // Support both "aggregator" (new) and "supervised" (legacy) for transition
        if ("aggregator".equalsIgnoreCase(role) || "supervised".equalsIgnoreCase(role)) {
            return Role.AGGREGATOR;
        }
        if ("supervisor".equalsIgnoreCase(role)) {
            return Role.SUPERVISOR;
        }
        return Role.UNKNOWN;
    }
    
    /**
     * Check if the current user is a supervisor
     * @return true if user is a supervisor, false otherwise
     */
    public static boolean isSupervisor() {
        return getCurrentRole() == Role.SUPERVISOR;
    }
    
    /**
     * Check if the current user is an aggregator (data collector)
     * @return true if user is an aggregator, false otherwise
     */
    public static boolean isAggregator() {
        return getCurrentRole() == Role.AGGREGATOR;
    }
}
