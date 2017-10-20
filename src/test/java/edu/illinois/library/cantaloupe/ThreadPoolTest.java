package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ThreadPoolTest extends BaseTest {

    private ThreadPool instance;

    @Before
    public void setUp() {
        instance = ThreadPool.getInstance();
    }

    @After
    public void tearDown() {
        ThreadPool.clearInstance();
        instance = null;
    }

    @Test
    public void testIsShutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    public void testShutdown() {
        assertFalse(instance.isShutdown());
        instance.shutdown();
        assertTrue(instance.isShutdown());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        });
        Thread.sleep(10); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    public void testSubmitCallableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(() -> {
            atomicInt.incrementAndGet();
            return null;
        }, ThreadPool.Priority.HIGH);
        Thread.sleep(10); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet);
        Thread.sleep(10); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

    @Test
    public void testSubmitRunnableWithPriority() throws Exception {
        final AtomicInteger atomicInt = new AtomicInteger(0);
        instance.submit(atomicInt::incrementAndGet, ThreadPool.Priority.HIGH);
        Thread.sleep(10); // wait for it to happen
        assertEquals(1, atomicInt.get());
    }

}
