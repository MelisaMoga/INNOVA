package com.melisa.innovamotionapp.ui.models;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for MessageLogItem.
 */
public class MessageLogItemTest {

    private static final int ICON_RES = 123;

    // ========== Constructor Tests ==========

    @Test
    public void constructor_setsAllFieldsCorrectly() {
        MessageLogItem item = new MessageLogItem(
                1L, 1234567890L, "sensor001", "Ion Popescu", "0xAB3311", ICON_RES, false
        );

        assertEquals(1L, item.getId());
        assertEquals(1234567890L, item.getTimestamp());
        assertEquals("sensor001", item.getSensorId());
        assertEquals("Ion Popescu", item.getDisplayName());
        assertEquals("0xAB3311", item.getHexCode());
        assertEquals(ICON_RES, item.getPostureIconRes());
        assertFalse(item.isFall());
    }

    @Test
    public void constructor_withFallTrue() {
        MessageLogItem item = new MessageLogItem(
                2L, 1234567890L, "sensor002", "Maria Ionescu", "0xEF0112", ICON_RES, true
        );

        assertTrue(item.isFall());
    }

    // ========== Equals Tests ==========

    @Test
    public void equals_sameFields_returnsTrue() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertEquals(item1, item2);
    }

    @Test
    public void equals_differentId_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                2L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentTimestamp_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 2000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentSensorId_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s2", "Name1", "0x1", ICON_RES, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentDisplayName_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name2", "0x1", ICON_RES, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentHexCode_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x2", ICON_RES, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentIconRes_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", 100, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", 200, false
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_differentIsFall_returnsFalse() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, true
        );

        assertNotEquals(item1, item2);
    }

    @Test
    public void equals_null_returnsFalse() {
        MessageLogItem item = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertNotEquals(null, item);
    }

    @Test
    public void equals_differentClass_returnsFalse() {
        MessageLogItem item = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertNotEquals("string", item);
    }

    // ========== HashCode Tests ==========

    @Test
    public void hashCode_sameFields_sameHash() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );

        assertEquals(item1.hashCode(), item2.hashCode());
    }

    @Test
    public void hashCode_differentFields_differentHash() {
        MessageLogItem item1 = new MessageLogItem(
                1L, 1000L, "s1", "Name1", "0x1", ICON_RES, false
        );
        MessageLogItem item2 = new MessageLogItem(
                2L, 2000L, "s2", "Name2", "0x2", ICON_RES + 1, true
        );

        assertNotEquals(item1.hashCode(), item2.hashCode());
    }

    // ========== ToString Tests ==========

    @Test
    public void toString_containsAllFields() {
        MessageLogItem item = new MessageLogItem(
                1L, 1234567890L, "sensor001", "Ion Popescu", "0xAB3311", ICON_RES, true
        );

        String str = item.toString();

        assertTrue(str.contains("id=1"));
        assertTrue(str.contains("timestamp=1234567890"));
        assertTrue(str.contains("sensorId='sensor001'"));
        assertTrue(str.contains("displayName='Ion Popescu'"));
        assertTrue(str.contains("hexCode='0xAB3311'"));
        assertTrue(str.contains("isFall=true"));
    }

    // ========== Edge Cases ==========

    @Test
    public void constructor_emptyStrings() {
        MessageLogItem item = new MessageLogItem(
                0L, 0L, "", "", "", 0, false
        );

        assertEquals("", item.getSensorId());
        assertEquals("", item.getDisplayName());
        assertEquals("", item.getHexCode());
    }

    @Test
    public void constructor_unicodeDisplayName() {
        MessageLogItem item = new MessageLogItem(
                1L, 1000L, "sensor001", "Ștefan Românescu", "0xAB3311", ICON_RES, false
        );

        assertEquals("Ștefan Românescu", item.getDisplayName());
    }

    @Test
    public void constructor_uuidSensorId() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        MessageLogItem item = new MessageLogItem(
                1L, 1000L, uuid, "Test Name", "0xAB3311", ICON_RES, false
        );

        assertEquals(uuid, item.getSensorId());
    }

    @Test
    public void constructor_negativeId() {
        MessageLogItem item = new MessageLogItem(
                -1L, 1000L, "s1", "Name", "0x1", ICON_RES, false
        );

        assertEquals(-1L, item.getId());
    }

    @Test
    public void constructor_maxLongValues() {
        MessageLogItem item = new MessageLogItem(
                Long.MAX_VALUE, Long.MAX_VALUE, "s1", "Name", "0x1", Integer.MAX_VALUE, false
        );

        assertEquals(Long.MAX_VALUE, item.getId());
        assertEquals(Long.MAX_VALUE, item.getTimestamp());
        assertEquals(Integer.MAX_VALUE, item.getPostureIconRes());
    }
}
