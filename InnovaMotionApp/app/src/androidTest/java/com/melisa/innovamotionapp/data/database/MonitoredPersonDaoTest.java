package com.melisa.innovamotionapp.data.database;

import static org.junit.Assert.*;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Instrumented tests for MonitoredPersonDao.
 * 
 * Tests cover:
 * - Insert, update, delete operations
 * - Display name lookup
 * - Upsert functionality
 * - Sensor existence check
 * - Query ordering
 */
@RunWith(AndroidJUnit4.class)
public class MonitoredPersonDaoTest {

    private InnovaDatabase database;
    private MonitoredPersonDao dao;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, InnovaDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = database.monitoredPersonDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    // ========== Helper Methods ==========

    private MonitoredPerson createPerson(String sensorId, String displayName) {
        long now = System.currentTimeMillis();
        return new MonitoredPerson(sensorId, displayName, now, now);
    }

    private <T> T getLiveDataValue(LiveData<T> liveData) throws InterruptedException {
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        liveData.observeForever(value -> {
            result.set(value);
            latch.countDown();
        });
        latch.await(2, TimeUnit.SECONDS);
        return result.get();
    }

    // ========== Insert Tests ==========

    @Test
    public void testInsert() {
        MonitoredPerson person = createPerson("sensor001", "Ion Popescu");
        long id = dao.insert(person);
        assertTrue(id > 0);
    }

    @Test
    public void testInsertReplacesOnConflict() {
        dao.insert(createPerson("sensor001", "Ion Popescu"));
        dao.insert(createPerson("sensor001", "Maria Ionescu")); // Same sensorId
        
        String name = dao.getDisplayNameForSensor("sensor001");
        assertEquals("Maria Ionescu", name);
    }

    @Test
    public void testInsertMultiple() {
        dao.insert(createPerson("sensor001", "Ion"));
        dao.insert(createPerson("sensor002", "Maria"));
        dao.insert(createPerson("sensor003", "Gheorghe"));
        
        assertEquals(3, dao.countAll());
    }

    // ========== Query Tests ==========

    @Test
    public void testGetDisplayNameForSensor() {
        dao.insert(createPerson("sensor001", "Ion Popescu"));
        
        String name = dao.getDisplayNameForSensor("sensor001");
        assertEquals("Ion Popescu", name);
    }

    @Test
    public void testGetDisplayNameForSensorReturnsNullIfNotFound() {
        String name = dao.getDisplayNameForSensor("unknown");
        assertNull(name);
    }

    @Test
    public void testGetPersonBySensorId() {
        dao.insert(createPerson("sensor001", "Ion Popescu"));
        
        MonitoredPerson person = dao.getPersonBySensorId("sensor001");
        assertNotNull(person);
        assertEquals("sensor001", person.getSensorId());
        assertEquals("Ion Popescu", person.getDisplayName());
    }

    @Test
    public void testGetPersonBySensorIdReturnsNullIfNotFound() {
        MonitoredPerson person = dao.getPersonBySensorId("unknown");
        assertNull(person);
    }

    @Test
    public void testSensorExists() {
        dao.insert(createPerson("sensor001", "Ion"));
        
        assertEquals(1, dao.sensorExists("sensor001"));
        assertEquals(0, dao.sensorExists("unknown"));
    }

    @Test
    public void testGetAllMonitoredPersonsSync() {
        dao.insert(createPerson("sensor003", "Gheorghe"));
        dao.insert(createPerson("sensor001", "Ion"));
        dao.insert(createPerson("sensor002", "Maria"));
        
        List<MonitoredPerson> persons = dao.getAllMonitoredPersonsSync();
        
        assertEquals(3, persons.size());
        // Should be ordered by display name ASC
        assertEquals("Gheorghe", persons.get(0).getDisplayName());
        assertEquals("Ion", persons.get(1).getDisplayName());
        assertEquals("Maria", persons.get(2).getDisplayName());
    }

    @Test
    public void testGetAllMonitoredPersonsLive() throws InterruptedException {
        dao.insert(createPerson("sensor001", "Ion"));
        dao.insert(createPerson("sensor002", "Maria"));
        
        List<MonitoredPerson> persons = getLiveDataValue(dao.getAllMonitoredPersons());
        
        assertNotNull(persons);
        assertEquals(2, persons.size());
    }

    @Test
    public void testGetAllSensorIds() {
        dao.insert(createPerson("sensor003", "Gheorghe"));
        dao.insert(createPerson("sensor001", "Ion"));
        dao.insert(createPerson("sensor002", "Maria"));
        
        List<String> sensorIds = dao.getAllSensorIds();
        
        assertEquals(3, sensorIds.size());
        // Ordered by display name, so order is: Gheorghe (003), Ion (001), Maria (002)
        assertEquals("sensor003", sensorIds.get(0));
        assertEquals("sensor001", sensorIds.get(1));
        assertEquals("sensor002", sensorIds.get(2));
    }

    // ========== Upsert Tests ==========

    @Test
    public void testUpsertByNameCreatesNew() {
        long now = System.currentTimeMillis();
        dao.upsertByName("sensor001", "Ion Popescu", now);
        
        MonitoredPerson person = dao.getPersonBySensorId("sensor001");
        assertNotNull(person);
        assertEquals("Ion Popescu", person.getDisplayName());
    }

    @Test
    public void testUpsertByNameUpdatesExisting() {
        long now1 = System.currentTimeMillis();
        dao.upsertByName("sensor001", "Ion Popescu", now1);
        
        long now2 = now1 + 1000;
        dao.upsertByName("sensor001", "Ion Alexandru", now2);
        
        MonitoredPerson person = dao.getPersonBySensorId("sensor001");
        assertNotNull(person);
        assertEquals("Ion Alexandru", person.getDisplayName());
        assertEquals(now2, person.getUpdatedAt());
        // Created at should remain the original time
        assertEquals(now1, person.getCreatedAt());
    }

    @Test
    public void testUpsertPreservesCreatedAt() {
        long originalTime = 1000L;
        dao.upsertByName("sensor001", "Original Name", originalTime);
        
        MonitoredPerson original = dao.getPersonBySensorId("sensor001");
        long originalCreatedAt = original.getCreatedAt();
        
        long updateTime = 2000L;
        dao.upsertByName("sensor001", "Updated Name", updateTime);
        
        MonitoredPerson updated = dao.getPersonBySensorId("sensor001");
        assertEquals(originalCreatedAt, updated.getCreatedAt());
        assertEquals(updateTime, updated.getUpdatedAt());
    }

    // ========== Update Tests ==========

    @Test
    public void testUpdateDisplayName() {
        dao.insert(createPerson("sensor001", "Old Name"));
        
        long now = System.currentTimeMillis();
        int affected = dao.updateDisplayName("sensor001", "New Name", now);
        
        assertEquals(1, affected);
        assertEquals("New Name", dao.getDisplayNameForSensor("sensor001"));
    }

    @Test
    public void testUpdateDisplayNameReturnsZeroIfNotFound() {
        int affected = dao.updateDisplayName("unknown", "Name", System.currentTimeMillis());
        assertEquals(0, affected);
    }

    // ========== Delete Tests ==========

    @Test
    public void testDelete() {
        MonitoredPerson person = createPerson("sensor001", "Ion");
        dao.insert(person);
        
        // Need to get the person with ID set
        MonitoredPerson inserted = dao.getPersonBySensorId("sensor001");
        dao.delete(inserted);
        
        assertNull(dao.getPersonBySensorId("sensor001"));
    }

    @Test
    public void testDeleteBySensorId() {
        dao.insert(createPerson("sensor001", "Ion"));
        
        int affected = dao.deleteBySensorId("sensor001");
        
        assertEquals(1, affected);
        assertNull(dao.getPersonBySensorId("sensor001"));
    }

    @Test
    public void testDeleteBySensorIdReturnsZeroIfNotFound() {
        int affected = dao.deleteBySensorId("unknown");
        assertEquals(0, affected);
    }

    @Test
    public void testClearAll() {
        dao.insert(createPerson("sensor001", "Ion"));
        dao.insert(createPerson("sensor002", "Maria"));
        dao.insert(createPerson("sensor003", "Gheorghe"));
        
        int affected = dao.clearAll();
        
        assertEquals(3, affected);
        assertEquals(0, dao.countAll());
    }

    // ========== Count Tests ==========

    @Test
    public void testCountAll() {
        assertEquals(0, dao.countAll());
        
        dao.insert(createPerson("sensor001", "Ion"));
        assertEquals(1, dao.countAll());
        
        dao.insert(createPerson("sensor002", "Maria"));
        assertEquals(2, dao.countAll());
    }

    // ========== UUID SensorId Format Tests ==========

    @Test
    public void testUuidSensorIdFormat() {
        String uuid = "5d6d75ee-b6c8-42d4-a233-b13d137fea38";
        dao.insert(createPerson(uuid, "Copil 1"));
        
        MonitoredPerson person = dao.getPersonBySensorId(uuid);
        assertNotNull(person);
        assertEquals(uuid, person.getSensorId());
    }

    @Test
    public void testMixedSensorIdFormats() {
        dao.insert(createPerson("sensor001", "Simple ID"));
        dao.insert(createPerson("5d6d75ee-b6c8-42d4-a233-b13d137fea38", "UUID"));
        dao.insert(createPerson("12345", "Numeric"));
        
        assertEquals(3, dao.countAll());
        assertEquals("Simple ID", dao.getDisplayNameForSensor("sensor001"));
        assertEquals("UUID", dao.getDisplayNameForSensor("5d6d75ee-b6c8-42d4-a233-b13d137fea38"));
        assertEquals("Numeric", dao.getDisplayNameForSensor("12345"));
    }
}
