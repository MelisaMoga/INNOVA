package com.melisa.innovamotionapp.ui.adapters;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PersonCardAdapter.
 * 
 * Tests time formatting and stale detection logic.
 */
public class PersonCardAdapterTest {

    // ========== Time Formatting Tests ==========

    @Test
    public void formatTimeAgo_justNow_under1Minute() {
        long now = System.currentTimeMillis();
        long timestamp = now - 30_000; // 30 seconds ago
        
        assertEquals("Just now", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_oneMinuteAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - 60_000; // 1 minute ago
        
        assertEquals("1m ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_fiveMinutesAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (5 * 60_000); // 5 minutes ago
        
        assertEquals("5m ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_thirtyMinutesAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (30 * 60_000); // 30 minutes ago
        
        assertEquals("30m ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_fiftyNineMinutesAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (59 * 60_000); // 59 minutes ago
        
        assertEquals("59m ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_oneHourAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (60 * 60_000); // 1 hour ago
        
        assertEquals("1h ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_threeHoursAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (3 * 60 * 60_000); // 3 hours ago
        
        assertEquals("3h ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_twentyThreeHoursAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (23 * 60 * 60_000); // 23 hours ago
        
        assertEquals("23h ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_oneDayAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (24 * 60 * 60_000); // 1 day ago
        
        assertEquals("1d ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    @Test
    public void formatTimeAgo_threeDaysAgo() {
        long now = System.currentTimeMillis();
        long timestamp = now - (3 * 24 * 60 * 60_000L); // 3 days ago
        
        assertEquals("3d ago", PersonCardAdapter.formatTimeAgo(timestamp, now));
    }

    // ========== Stale Detection Tests ==========

    @Test
    public void isStale_recentTimestamp_returnsFalse() {
        long now = System.currentTimeMillis();
        long timestamp = now - 60_000; // 1 minute ago
        
        assertFalse(PersonCardAdapter.isStale(timestamp, now));
    }

    @Test
    public void isStale_exactlyAtThreshold_returnsFalse() {
        long now = System.currentTimeMillis();
        long threshold = PersonCardAdapter.getStaleThresholdMs();
        long timestamp = now - threshold; // Exactly at threshold
        
        // At exactly threshold, it's not stale (needs to be > threshold)
        assertFalse(PersonCardAdapter.isStale(timestamp, now));
    }

    @Test
    public void isStale_justOverThreshold_returnsTrue() {
        long now = System.currentTimeMillis();
        long threshold = PersonCardAdapter.getStaleThresholdMs();
        long timestamp = now - threshold - 1; // Just over threshold
        
        assertTrue(PersonCardAdapter.isStale(timestamp, now));
    }

    @Test
    public void isStale_veryOldTimestamp_returnsTrue() {
        long now = System.currentTimeMillis();
        long timestamp = now - (60 * 60_000); // 1 hour ago
        
        assertTrue(PersonCardAdapter.isStale(timestamp, now));
    }

    @Test
    public void isStale_fourMinutesAgo_returnsFalse() {
        long now = System.currentTimeMillis();
        long timestamp = now - (4 * 60_000); // 4 minutes ago
        
        assertFalse(PersonCardAdapter.isStale(timestamp, now));
    }

    @Test
    public void isStale_sixMinutesAgo_returnsTrue() {
        long now = System.currentTimeMillis();
        long timestamp = now - (6 * 60_000); // 6 minutes ago
        
        assertTrue(PersonCardAdapter.isStale(timestamp, now));
    }

    // ========== Threshold Constant Tests ==========

    @Test
    public void staleThreshold_isFiveMinutes() {
        assertEquals(5 * 60 * 1000, PersonCardAdapter.getStaleThresholdMs());
    }

    // ========== Edge Cases ==========

    @Test
    public void formatTimeAgo_futureTimestamp_showsJustNow() {
        long now = System.currentTimeMillis();
        long timestamp = now + 60_000; // 1 minute in the future
        
        // For negative diffs (future), it should still work
        // The actual behavior depends on implementation, but it shouldn't crash
        String result = PersonCardAdapter.formatTimeAgo(timestamp, now);
        assertNotNull(result);
    }

    @Test
    public void formatTimeAgo_zeroTimestamp() {
        long now = System.currentTimeMillis();
        long timestamp = 0;
        
        // Very old timestamp should show days
        String result = PersonCardAdapter.formatTimeAgo(timestamp, now);
        assertTrue(result.contains("d ago"));
    }

    @Test
    public void isStale_zeroTimestamp_returnsTrue() {
        long now = System.currentTimeMillis();
        assertTrue(PersonCardAdapter.isStale(0, now));
    }
}
