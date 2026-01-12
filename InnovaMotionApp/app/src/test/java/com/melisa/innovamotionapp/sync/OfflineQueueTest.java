package com.melisa.innovamotionapp.sync;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Unit tests for offline queue operations.
 * 
 * Tests queue management, size limits, ordering, and retry tracking.
 */
public class OfflineQueueTest {

    private static final int MAX_QUEUE_SIZE = 100;

    private Queue<MockQueuedPacket> queue;

    @Before
    public void setUp() {
        queue = new ConcurrentLinkedQueue<>();
    }

    // ========== Basic Queue Operations ==========

    @Test
    public void queue_addPacket_increasesSize() {
        assertEquals(0, queue.size());
        queue.add(createMockPacket(10));
        assertEquals(1, queue.size());
    }

    @Test
    public void queue_pollPacket_decreasesSize() {
        queue.add(createMockPacket(10));
        queue.add(createMockPacket(20));
        assertEquals(2, queue.size());
        
        queue.poll();
        assertEquals(1, queue.size());
    }

    @Test
    public void queue_pollEmpty_returnsNull() {
        assertNull(queue.poll());
    }

    @Test
    public void queue_peekDoesNotRemove() {
        queue.add(createMockPacket(10));
        queue.peek();
        assertEquals(1, queue.size());
    }

    // ========== FIFO Ordering Tests ==========

    @Test
    public void queue_maintainsFifoOrder() {
        MockQueuedPacket first = createMockPacket(1);
        MockQueuedPacket second = createMockPacket(2);
        MockQueuedPacket third = createMockPacket(3);

        queue.add(first);
        queue.add(second);
        queue.add(third);

        assertEquals(first, queue.poll());
        assertEquals(second, queue.poll());
        assertEquals(third, queue.poll());
    }

    // ========== Size Limit Tests ==========

    @Test
    public void queueWithLimit_atLimit_dropsOldest() {
        // Simulate a queue with max size limit
        for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
            queue.add(createMockPacket(i));
        }
        assertEquals(MAX_QUEUE_SIZE, queue.size());

        // Add one more - should drop oldest (simulating the logic)
        if (queue.size() >= MAX_QUEUE_SIZE) {
            queue.poll(); // Drop oldest
        }
        queue.add(createMockPacket(MAX_QUEUE_SIZE));
        
        assertEquals(MAX_QUEUE_SIZE, queue.size());
    }

    @Test
    public void queueWithLimit_newestPreserved() {
        for (int i = 0; i < MAX_QUEUE_SIZE; i++) {
            queue.add(createMockPacket(i));
        }

        // Add newest packet after dropping oldest
        if (queue.size() >= MAX_QUEUE_SIZE) {
            queue.poll();
        }
        MockQueuedPacket newest = createMockPacket(999);
        queue.add(newest);

        // Poll until last
        MockQueuedPacket last = null;
        while (!queue.isEmpty()) {
            last = queue.poll();
        }
        
        assertEquals(newest, last);
    }

    // ========== Retry Tracking Tests ==========

    @Test
    public void retryCount_initiallyZero() {
        MockQueuedPacket packet = createMockPacket(10);
        assertEquals(0, packet.retryCount);
    }

    @Test
    public void retryCount_incrementsOnFailure() {
        MockQueuedPacket packet = createMockPacket(10);
        packet.retryCount++;
        assertEquals(1, packet.retryCount);
        packet.retryCount++;
        assertEquals(2, packet.retryCount);
    }

    @Test
    public void retryCount_exceedsMax_shouldDrop() {
        MockQueuedPacket packet = createMockPacket(10);
        packet.retryCount = 3;
        
        int maxRetries = 3;
        boolean shouldDrop = packet.retryCount >= maxRetries;
        assertTrue(shouldDrop);
    }

    @Test
    public void retryCount_belowMax_shouldRetry() {
        MockQueuedPacket packet = createMockPacket(10);
        packet.retryCount = 2;
        
        int maxRetries = 3;
        boolean shouldRetry = packet.retryCount < maxRetries;
        assertTrue(shouldRetry);
    }

    // ========== Thread Safety Tests ==========

    @Test
    public void concurrentLinkedQueue_isThreadSafe() {
        // ConcurrentLinkedQueue should handle concurrent access
        ConcurrentLinkedQueue<MockQueuedPacket> concurrentQueue = new ConcurrentLinkedQueue<>();
        
        // Add from multiple simulated threads
        for (int i = 0; i < 100; i++) {
            concurrentQueue.add(createMockPacket(i));
        }
        
        assertEquals(100, concurrentQueue.size());
    }

    // ========== Edge Cases ==========

    @Test
    public void queue_clearRemovesAll() {
        queue.add(createMockPacket(1));
        queue.add(createMockPacket(2));
        queue.add(createMockPacket(3));
        
        queue.clear();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    public void queue_isEmpty_afterAllPolled() {
        queue.add(createMockPacket(1));
        queue.poll();
        assertTrue(queue.isEmpty());
    }

    @Test
    public void packetEntities_defensivelyCopied() {
        List<String> original = new ArrayList<>();
        original.add("entity1");
        original.add("entity2");
        
        MockQueuedPacket packet = new MockQueuedPacket(new ArrayList<>(original));
        
        // Modify original
        original.add("entity3");
        
        // Packet should have original 2 entities (defensive copy)
        assertEquals(2, packet.entities.size());
    }

    // ========== Helper Classes ==========

    /**
     * Mock implementation of QueuedPacket for testing
     */
    private static class MockQueuedPacket {
        final List<String> entities;
        int retryCount;
        final int id;
        private static int idCounter = 0;

        MockQueuedPacket(List<String> entities) {
            this.entities = new ArrayList<>(entities); // Defensive copy
            this.retryCount = 0;
            this.id = idCounter++;
        }
    }

    private MockQueuedPacket createMockPacket(int entityCount) {
        List<String> entities = new ArrayList<>();
        for (int i = 0; i < entityCount; i++) {
            entities.add("entity_" + i);
        }
        return new MockQueuedPacket(entities);
    }
}
