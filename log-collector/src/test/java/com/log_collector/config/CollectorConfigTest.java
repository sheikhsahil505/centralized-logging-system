package com.log_collector.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class CollectorConfigTest {

    private CollectorConfig config;

    @BeforeEach
    void setUp() {
        config = new CollectorConfig();
    }

    @Test
    void testLogQueueCreation() {
        BlockingQueue<String> queue = config.logQueue();

        assertNotNull(queue);
        assertEquals(1000, queue.remainingCapacity());
        assertTrue(queue.isEmpty());
    }

    @Test
    void testLogQueueCapacity() {
        BlockingQueue<String> queue = config.logQueue();

        // Fill queue to test capacity
        for (int i = 0; i < 1000; i++) {
            assertTrue(queue.offer("message" + i));
        }

        // Queue should be full
        assertFalse(queue.offer("extra"));
        assertEquals(0, queue.remainingCapacity());
    }

    @Test
    void testWorkerPoolCreation() {
        ExecutorService pool = config.workerPool();

        assertNotNull(pool);
        assertFalse(pool.isShutdown());
        assertFalse(pool.isTerminated());
    }

    @Test
    void testWorkerPoolShutdown() {
        ExecutorService pool = config.workerPool();

        pool.shutdown();
        assertTrue(pool.isShutdown());
    }

    @Test
    void testWorkerPoolExecutesTasks() throws Exception {
        ExecutorService pool = config.workerPool();
        final boolean[] executed = {false};

        pool.submit(() -> executed[0] = true);
        Thread.sleep(100);

        assertTrue(executed[0]);
        pool.shutdown();
    }

    @Test
    void testMultipleQueueInstances() {
        BlockingQueue<String> queue1 = config.logQueue();
        BlockingQueue<String> queue2 = config.logQueue();

        // Each call should create a new instance
        assertNotSame(queue1, queue2);
    }

    @Test
    void testMultiplePoolInstances() {
        ExecutorService pool1 = config.workerPool();
        ExecutorService pool2 = config.workerPool();

        assertNotSame(pool1, pool2);

        pool1.shutdown();
        pool2.shutdown();
    }
}