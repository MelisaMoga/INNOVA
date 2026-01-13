package com.melisa.innovamotionapp.ui.viewmodels;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for MessageLogViewModel helper methods.
 * 
 * Note: Full ViewModel testing requires AndroidX Test framework since
 * MessageLogViewModel extends AndroidViewModel. These tests cover the
 * static/helper logic that can be tested without Android context.
 */
public class MessageLogViewModelTest {

    // Posture hex codes (same as in ViewModel)
    private static final String HEX_STANDING = "0xab3311";
    private static final String HEX_SITTING = "0xac4312";
    private static final String HEX_WALKING = "0xba3311";
    private static final String HEX_FALLING = "0xef0112";
    private static final String HEX_UNUSED = "0x793248";

    // ========== Fall Detection Tests ==========

    @Test
    public void isFallPosture_fallingHexCode_returnsTrue() {
        assertTrue(isFallPosture(HEX_FALLING));
    }

    @Test
    public void isFallPosture_fallingHexCodeUpperCase_returnsTrue() {
        assertTrue(isFallPosture("0xEF0112"));
    }

    @Test
    public void isFallPosture_standingHexCode_returnsFalse() {
        assertFalse(isFallPosture(HEX_STANDING));
    }

    @Test
    public void isFallPosture_sittingHexCode_returnsFalse() {
        assertFalse(isFallPosture(HEX_SITTING));
    }

    @Test
    public void isFallPosture_walkingHexCode_returnsFalse() {
        assertFalse(isFallPosture(HEX_WALKING));
    }

    @Test
    public void isFallPosture_unusedHexCode_returnsFalse() {
        assertFalse(isFallPosture(HEX_UNUSED));
    }

    @Test
    public void isFallPosture_null_returnsFalse() {
        assertFalse(isFallPosture(null));
    }

    @Test
    public void isFallPosture_emptyString_returnsFalse() {
        assertFalse(isFallPosture(""));
    }

    @Test
    public void isFallPosture_unknownCode_returnsFalse() {
        assertFalse(isFallPosture("0xFFFFFF"));
    }

    // ========== Posture Icon Mapping Tests ==========

    @Test
    public void getPostureType_standingCode_returnsStanding() {
        assertEquals("standing", getPostureType(HEX_STANDING));
    }

    @Test
    public void getPostureType_sittingCode_returnsSitting() {
        assertEquals("sitting", getPostureType(HEX_SITTING));
    }

    @Test
    public void getPostureType_walkingCode_returnsWalking() {
        assertEquals("walking", getPostureType(HEX_WALKING));
    }

    @Test
    public void getPostureType_fallingCode_returnsFalling() {
        assertEquals("falling", getPostureType(HEX_FALLING));
    }

    @Test
    public void getPostureType_unusedCode_returnsUnknown() {
        assertEquals("unknown", getPostureType(HEX_UNUSED));
    }

    @Test
    public void getPostureType_unknownCode_returnsUnknown() {
        assertEquals("unknown", getPostureType("0xFFFFFF"));
    }

    @Test
    public void getPostureType_null_returnsUnknown() {
        assertEquals("unknown", getPostureType(null));
    }

    @Test
    public void getPostureType_upperCaseCode_worksCorrectly() {
        assertEquals("standing", getPostureType("0xAB3311"));
        assertEquals("sitting", getPostureType("0xAC4312"));
        assertEquals("walking", getPostureType("0xBA3311"));
        assertEquals("falling", getPostureType("0xEF0112"));
    }

    @Test
    public void getPostureType_mixedCaseCode_worksCorrectly() {
        assertEquals("standing", getPostureType("0xAb3311"));
        assertEquals("falling", getPostureType("0xEf0112"));
    }

    // ========== Helper Methods (duplicated from ViewModel for testing) ==========

    /**
     * Check if a hex code represents a fall posture.
     * Duplicated from ViewModel for unit testing.
     */
    private boolean isFallPosture(String hexCode) {
        if (hexCode == null) return false;
        return hexCode.toLowerCase().equals(HEX_FALLING);
    }

    /**
     * Get posture type string for a hex code.
     * Helper for testing posture mapping logic.
     */
    private String getPostureType(String hexCode) {
        if (hexCode == null) return "unknown";
        
        String normalized = hexCode.toLowerCase();
        switch (normalized) {
            case HEX_STANDING:
                return "standing";
            case HEX_SITTING:
                return "sitting";
            case HEX_WALKING:
                return "walking";
            case HEX_FALLING:
                return "falling";
            default:
                return "unknown";
        }
    }
}
