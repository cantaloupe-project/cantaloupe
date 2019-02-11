package edu.illinois.library.cantaloupe.async;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class AuditableFutureTaskTest extends BaseTest {

    private AuditableFutureTask<?> instance;

    @Before
    public void setUp() {
        instance = new AuditableFutureTask<>(new MockCallable<>());
        assertNotNull(instance.getUUID());
        assertEquals(TaskStatus.NEW, instance.getStatus());
    }

    @Test
    public void testGetException() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> {
            throw new Exception("fail");
        });
        task.run();
        assertEquals("fail", task.getException().getMessage());
    }

    @Test
    public void testGetInstantQueued() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> "");
        task.setInstantQueued(Instant.now());
        assertNotNull(task.getInstantQueued());
    }

    @Test
    public void testGetStatus() {
        // This will be tested in other test methods.
    }

    @Test
    public void testRun() {
        // Assert that this gets incremented.
        final AtomicInteger integer = new AtomicInteger(0);
        AuditableFutureTask<?> task =
                new AuditableFutureTask<>(integer::incrementAndGet);
        task.run();
        assertEquals(1, integer.get());
    }

    @Test
    public void testRunSetsInstantStarted() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> "");
        task.run();

        final long delta = Instant.now().toEpochMilli() -
                task.getInstantStarted().toEpochMilli();
        assertTrue(delta < 10);
    }

    @Test
    public void testRunSetsInstantStoppedUponSuccess() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> "");
        task.run();

        final long delta = Instant.now().toEpochMilli() -
                task.getInstantStopped().toEpochMilli();
        assertTrue(delta < 10);
    }

    @Test
    public void testRunSetsInstantStoppedUponFailure() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> {
            throw new Exception("fail");
        });
        task.run();

        final long delta = Instant.now().toEpochMilli() -
                task.getInstantStopped().toEpochMilli();
        assertTrue(delta < 10);
    }

    @Test
    public void testRunSetsStatusWhenStarted() throws Exception {
        AuditableFutureTask<?> task = new AuditableFutureTask<Void>(() -> {
            Thread.sleep(50);
            return null;
        });
        ThreadPool.getInstance().submit(task);
        Thread.sleep(20);
        assertEquals(TaskStatus.RUNNING, task.getStatus());
    }

    @Test
    public void testRunSetsStatusUponSuccess() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> "");
        task.run();

        assertEquals(TaskStatus.SUCCEEDED, task.getStatus());
    }

    @Test
    public void testRunSetsStatusUponFailure() {
        AuditableFutureTask<?> task = new AuditableFutureTask<>(() -> {
            throw new Exception("fail");
        });
        task.run();
        assertEquals(TaskStatus.FAILED, task.getStatus());
    }

    @Test
    public void testToString() {
        assertEquals(instance.getUUID().toString(), instance.toString());
    }

}
