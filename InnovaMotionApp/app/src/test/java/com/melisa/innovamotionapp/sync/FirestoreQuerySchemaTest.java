package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.Map;

/**
 * Unit tests to verify Firestore schema consistency.
 * 
 * These tests ensure that:
 * 1. The field names used in queries match the field names in documents
 * 2. The 'uploadedBy' field is used consistently (not the deprecated 'userId')
 * 3. Schema constants are defined correctly
 * 
 * This test class was added to prevent regressions where query field names
 * don't match document field names (e.g., querying "userId" when docs have "uploadedBy").
 */
public class FirestoreQuerySchemaTest {

    // ========== Schema Field Name Constants ==========
    
    /**
     * The correct field name for the aggregator/uploader ID.
     * IMPORTANT: All Firestore queries must use this field name, not "userId".
     */
    public static final String FIELD_UPLOADED_BY = "uploadedBy";
    
    /**
     * The legacy field name (deprecated, kept for reading old documents).
     */
    public static final String LEGACY_FIELD_USER_ID = "userId";

    // ========== Field Consistency Tests ==========

    @Test
    public void testFirestoreDataModelUsesCorrectFieldName() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "aggregator123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // CRITICAL: Document must use 'uploadedBy', not 'userId'
        assertTrue("Document should contain '" + FIELD_UPLOADED_BY + "' field", 
                doc.containsKey(FIELD_UPLOADED_BY));
        assertFalse("Document should NOT contain '" + LEGACY_FIELD_USER_ID + "' field", 
                doc.containsKey(LEGACY_FIELD_USER_ID));
    }

    @Test
    public void testUploadedByFieldValue() {
        String expectedUploader = "test_aggregator_uid";
        
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", expectedUploader, "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        assertEquals("uploadedBy field should contain the aggregator UID",
                expectedUploader, doc.get(FIELD_UPLOADED_BY));
    }

    @Test
    public void testQueryFieldMatchesDocumentField() {
        // This test documents the expected behavior:
        // Queries should use 'uploadedBy' because documents have 'uploadedBy'
        
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // The field name that queries should use
        String queryFieldName = FIELD_UPLOADED_BY;
        
        // Verify this field exists in the document
        assertTrue("Query field '" + queryFieldName + "' must exist in document",
                doc.containsKey(queryFieldName));
        
        // Verify the value is accessible with this key
        assertNotNull("Value at '" + queryFieldName + "' should not be null",
                doc.get(queryFieldName));
    }

    // ========== Legacy Fallback Tests ==========

    @Test
    public void testLegacyDocumentsStillReadable() {
        // Legacy documents have 'userId' instead of 'uploadedBy'
        java.util.Map<String, Object> legacyDoc = new java.util.HashMap<>();
        legacyDoc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        legacyDoc.put("timestamp", 1000L);
        legacyDoc.put("receivedMsg", "0xAB3311");
        legacyDoc.put("userId", "legacy_user"); // Old field name
        legacyDoc.put("sensorId", "sensor001");

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(legacyDoc);

        // Should successfully read via fallback
        assertEquals("legacy_user", model.getUploadedBy());
    }

    @Test
    public void testNewFieldTakesPrecedenceOverLegacy() {
        // If document has both fields, 'uploadedBy' takes precedence
        java.util.Map<String, Object> doc = new java.util.HashMap<>();
        doc.put("deviceAddress", "AA:BB:CC:DD:EE:FF");
        doc.put("timestamp", 1000L);
        doc.put("uploadedBy", "new_user");
        doc.put("userId", "old_user"); // Should be ignored

        FirestoreDataModel model = FirestoreDataModel.fromFirestoreDocument(doc);

        assertEquals("new_user", model.getUploadedBy());
    }

    // ========== Document Structure Tests ==========

    @Test
    public void testRequiredFieldsPresent() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // All required fields for Firestore queries
        String[] requiredFields = {
                "deviceAddress",
                "timestamp",
                "receivedMsg",
                "uploadedBy",  // NOT "userId"
                "sensorId",
                "syncTimestamp",
                "documentId"
        };

        for (String field : requiredFields) {
            assertTrue("Missing required field: " + field, doc.containsKey(field));
        }
    }

    @Test
    public void testNoUnexpectedFields() {
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // Should have exactly 7 fields
        assertEquals("Document should have exactly 7 fields", 7, doc.size());

        // Verify no legacy fields are written
        assertFalse("Should not write 'userId' field", doc.containsKey("userId"));
        assertFalse("Should not write 'user' field", doc.containsKey("user"));
    }

    // ========== Index/Query Compatibility Tests ==========

    @Test
    public void testSensorIdFieldForQueries() {
        // Supervisor queries use sensorId to find relevant documents
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        assertTrue("Document must have 'sensorId' for supervisor queries",
                doc.containsKey("sensorId"));
        assertEquals("sensor001", doc.get("sensorId"));
    }

    @Test
    public void testTimestampFieldForOrdering() {
        // Queries order by timestamp
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        assertTrue("Document must have 'timestamp' for ordering",
                doc.containsKey("timestamp"));
        assertEquals(1000L, doc.get("timestamp"));
    }

    // ========== Aggregator Query Pattern Test ==========

    @Test
    public void testAggregatorQueryPattern() {
        // Aggregator queries: whereEqualTo("uploadedBy", userId)
        String aggregatorUid = "aggregator_abc123";
        
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", aggregatorUid, "sensor001");

        Map<String, Object> doc = model.toFirestoreDocument();

        // Verify the pattern works
        String queryValue = (String) doc.get(FIELD_UPLOADED_BY);
        assertEquals("Query should find this document with uploadedBy=" + aggregatorUid,
                aggregatorUid, queryValue);
    }

    // ========== Document ID Format Test ==========

    @Test
    public void testDocumentIdFormat() {
        // Document ID format: deviceAddress_sensorId_timestamp (no userId)
        FirestoreDataModel model = new FirestoreDataModel(
                "AA:BB:CC:DD:EE:FF", 1000L, "0xAB3311", "user123", "sensor001");

        String docId = model.getDocumentId();

        // Should NOT contain userId/uploadedBy (aggregator-agnostic)
        assertFalse("Document ID should not contain uploadedBy",
                docId.contains("user123"));
        
        // Should contain deviceAddress, sensorId, timestamp
        assertTrue("Document ID should contain device address",
                docId.contains("AABBCCDDEEFF"));
        assertTrue("Document ID should contain sensorId",
                docId.contains("sensor001"));
        assertTrue("Document ID should contain timestamp",
                docId.contains("1000"));
    }
}
