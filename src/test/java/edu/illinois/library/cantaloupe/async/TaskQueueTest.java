package edu.illinois.library.cantaloupe.async;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.FutureTask;

import static org.junit.Assert.*;

public class TaskQueueTest extends BaseTest {

    private TaskQueue instance;

    @Before
    public void setUp() {
        TaskQueue.clearInstance();
        instance = TaskQueue.getInstance();
    }

    /* queuedTasks() */

    @Test
    public void testQueuedTasks() throws Exception {
        MockCallable<?> task1 = new MockCallable();
        MockCallable<?> task2 = new MockCallable();
        MockCallable<?> task3 = new MockCallable();
        FutureTask<?> future1 = new FutureTask<>(task1);
        FutureTask<?> future2 = new FutureTask<>(task2);
        FutureTask<?> future3 = new FutureTask<>(task3);
        instance.submit(future1);
        instance.submit(future2);
        instance.submit(future3);

        Thread.sleep(20);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());
    }

    /* submit(Callable<?>) */

    @Test
    public void testSubmitCallable() throws Exception {
        MockCallable<?> callable1 = new MockCallable();
        MockCallable<?> callable2 = new MockCallable();
        MockCallable<?> callable3 = new MockCallable();

        instance.submit(callable1);
        instance.submit(callable2);
        instance.submit(callable3);

        Thread.sleep(20);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());

        Thread.sleep(400); // wait for them all to complete
        assertTrue(callable1.ran());
        assertTrue(callable2.ran());
        assertTrue(callable3.ran());
    }

    /* submit(Runnable) */

    @Test
    public void testSubmitRunnable() throws Exception {
        MockRunnable runnable1 = new MockRunnable();
        MockRunnable runnable2 = new MockRunnable();
        MockRunnable runnable3 = new MockRunnable();

        instance.submit(runnable1);
        instance.submit(runnable2);
        instance.submit(runnable3);

        Thread.sleep(20);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());

        Thread.sleep(400); // wait for them all to complete
        assertTrue(runnable1.ran());
        assertTrue(runnable2.ran());
        assertTrue(runnable3.ran());
    }

    /* submit(Runnable) with AuditableFutureTask */

    @Test
    public void testSubmitAuditableFutureTaskSetsQueuedTaskStatus()
            throws Exception {
        AuditableFutureTask<?> future1 =
                new AuditableFutureTask<>(new MockCallable<>());
        assertEquals(TaskStatus.NEW, future1.getStatus());
        AuditableFutureTask<?> future2 =
                new AuditableFutureTask<>(new MockCallable<>());
        assertEquals(TaskStatus.NEW, future2.getStatus());

        instance.submit(future1);
        instance.submit(future2);

        Thread.sleep(50);
        assertEquals(TaskStatus.RUNNING, future1.getStatus());
        assertEquals(TaskStatus.QUEUED, future2.getStatus());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsRunningTaskStatus()
            throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);
        Thread.sleep(50);
        assertEquals(TaskStatus.RUNNING, future.getStatus());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsInstantQueued()
            throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        assertNotNull(future.getInstantQueued());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsInstantStarted()
            throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(50);
        assertNotNull(future.getInstantStarted());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsInstantStopped()
            throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(150);
        assertNotNull(future.getInstantStopped());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsSuccessfulTaskStatus()
            throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);
        Thread.sleep(150);
        assertEquals(TaskStatus.SUCCEEDED, future.getStatus());
    }

    @Test
    public void testSubmitAuditableFutureTaskSetsFailedTaskStatus()
            throws Exception {
        MockFailingCallable<?> task = new MockFailingCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);
        Thread.sleep(150);
        assertEquals(TaskStatus.FAILED, future.getStatus());
        assertEquals("Failed, as requested",
                future.getException().getMessage());
    }

}
