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

    @Test
    public void testSubmitSubmitsTasks() throws Exception {
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

        Thread.sleep(400); // wait for them all to complete
        assertTrue(task1.ran());
        assertTrue(task2.ran());
        assertTrue(task3.ran());
    }

    @Test
    public void testSubmitSetsQueuedTaskStatus() throws Exception {
        // This is too hard to test so we'll skip it.
    }

    @Test
    public void testSubmitSetsRunningTaskStatus() throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);
        Thread.sleep(50);
        assertEquals(TaskStatus.RUNNING, future.getStatus());
    }

    @Test
    public void testSubmitSetsInstantQueued() throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        assertNotNull(future.getInstantQueued());
    }

    @Test
    public void testSubmitSetsInstantStarted() throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(50);
        assertNotNull(future.getInstantStarted());
    }

    @Test
    public void testSubmitSetsInstantStopped() throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        instance.submit(future);
        Thread.sleep(150);
        assertNotNull(future.getInstantStopped());
    }

    @Test
    public void testSubmitSetsSuccessfulTaskStatus() throws Exception {
        MockCallable<?> task = new MockCallable();
        AuditableFutureTask<?> future = new AuditableFutureTask<>(task);
        assertEquals(TaskStatus.NEW, future.getStatus());

        instance.submit(future);
        Thread.sleep(150);
        assertEquals(TaskStatus.SUCCEEDED, future.getStatus());
    }

    @Test
    public void testSubmitSetsFailedTaskStatus() throws Exception {
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
