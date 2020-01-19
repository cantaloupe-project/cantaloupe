package edu.illinois.library.cantaloupe.async;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ThreadPoolTest extends BaseTest {

    private ThreadPool instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = ThreadPool.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        ThreadPool.clearInstance();
        instance = null;
    }

    @Test
    void testIsShutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    void testShutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    void testSubmitCallable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        });
        Thread.sleep(20); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    void testSubmitCallableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        }, ThreadPool.Priority.HIGH);
        Thread.sleep(20); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    void testSubmitRunnable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet);
        Thread.sleep(10); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    void testSubmitRunnableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet, ThreadPool.Priority.HIGH);
        Thread.sleep(50); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

}
