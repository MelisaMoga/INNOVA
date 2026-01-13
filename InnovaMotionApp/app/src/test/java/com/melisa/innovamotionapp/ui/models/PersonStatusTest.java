package com.melisa.innovamotionapp.ui.models;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for PersonStatus model.
 * 
 * Tests equality, hashCode, and getPostureIconRes methods.
 */
public class PersonStatusTest {

    // ========== Equality Tests ==========

    @Test
    public void equals_sameValues_returnsTrue() {
        MockPosture posture1 = new MockPosture(100);
        MockPosture posture2 = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture1, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Ion", posture2, 1000L, false);
        
        assertEquals(status1, status2);
    }

    @Test
    public void equals_differentSensorId_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor2", "Ion", posture, 1000L, false);
        
        assertNotEquals(status1, status2);
    }

    @Test
    public void equals_differentDisplayName_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Maria", posture, 1000L, false);
        
        assertNotEquals(status1, status2);
    }

    @Test
    public void equals_differentTimestamp_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Ion", posture, 2000L, false);
        
        assertNotEquals(status1, status2);
    }

    @Test
    public void equals_differentAlertStatus_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Ion", posture, 1000L, true);
        
        assertNotEquals(status1, status2);
    }

    @Test
    public void equals_differentPostureClass_returnsFalse() {
        MockPosture posture1 = new MockPosture(100);
        MockPosture2 posture2 = new MockPosture2(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture1, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Ion", posture2, 1000L, false);
        
        assertNotEquals(status1, status2);
    }

    @Test
    public void equals_null_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        
        assertNotEquals(null, status);
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        
        assertNotEquals("not a PersonStatus", status);
    }

    // ========== HashCode Tests ==========

    @Test
    public void hashCode_equalObjects_sameHashCode() {
        MockPosture posture1 = new MockPosture(100);
        MockPosture posture2 = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture1, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor1", "Ion", posture2, 1000L, false);
        
        assertEquals(status1.hashCode(), status2.hashCode());
    }

    @Test
    public void hashCode_differentObjects_differentHashCode() {
        MockPosture posture = new MockPosture(100);
        
        PersonStatus status1 = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        PersonStatus status2 = new PersonStatus("sensor2", "Maria", posture, 2000L, true);
        
        assertNotEquals(status1.hashCode(), status2.hashCode());
    }

    // ========== Getter Tests ==========

    @Test
    public void getSensorId_returnsCorrectValue() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor123", "Ion", posture, 1000L, false);
        
        assertEquals("sensor123", status.getSensorId());
    }

    @Test
    public void getDisplayName_returnsCorrectValue() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion Popescu", posture, 1000L, false);
        
        assertEquals("Ion Popescu", status.getDisplayName());
    }

    @Test
    public void getLastUpdateTime_returnsCorrectValue() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1234567890L, false);
        
        assertEquals(1234567890L, status.getLastUpdateTime());
    }

    @Test
    public void isAlert_false_returnsFalse() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        
        assertFalse(status.isAlert());
    }

    @Test
    public void isAlert_true_returnsTrue() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1000L, true);
        
        assertTrue(status.isAlert());
    }

    @Test
    public void getPostureIconRes_delegatesToPosture() {
        MockPosture posture = new MockPosture(12345);
        PersonStatus status = new PersonStatus("sensor1", "Ion", posture, 1000L, false);
        
        assertEquals(12345, status.getPostureIconRes());
    }

    // ========== ToString Tests ==========

    @Test
    public void toString_containsSensorId() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor123", "Ion", posture, 1000L, false);
        
        assertTrue(status.toString().contains("sensor123"));
    }

    @Test
    public void toString_containsDisplayName() {
        MockPosture posture = new MockPosture(100);
        PersonStatus status = new PersonStatus("sensor1", "Ion Popescu", posture, 1000L, false);
        
        assertTrue(status.toString().contains("Ion Popescu"));
    }

    // ========== Mock Posture Classes for Testing ==========

    private static class MockPosture extends com.melisa.innovamotionapp.data.posture.Posture {
        private final int pictureCode;
        
        MockPosture(int pictureCode) {
            this.pictureCode = pictureCode;
        }
        
        @Override public int getRisc() { return 0; }
        @Override public int getTextCode() { return 0; }
        @Override public int getVideoCode() { return 0; }
        @Override public int getPictureCode() { return pictureCode; }
        @Override public int getCalories() { return 0; }
    }

    private static class MockPosture2 extends com.melisa.innovamotionapp.data.posture.Posture {
        private final int pictureCode;
        
        MockPosture2(int pictureCode) {
            this.pictureCode = pictureCode;
        }
        
        @Override public int getRisc() { return 0; }
        @Override public int getTextCode() { return 0; }
        @Override public int getVideoCode() { return 0; }
        @Override public int getPictureCode() { return pictureCode; }
        @Override public int getCalories() { return 0; }
    }
}
