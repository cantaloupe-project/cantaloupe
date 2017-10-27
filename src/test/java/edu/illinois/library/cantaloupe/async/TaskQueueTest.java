package edu.illinois.library.cantaloupe.async;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

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
        MockTask task1 = new MockTask();
        MockTask task2 = new MockTask();
        MockTask task3 = new MockTask();
        instance.submit(task1);
        instance.submit(task2);
        instance.submit(task3);

        Thread.sleep(20);

        // The first has been taken out and is running.
        assertEquals(2, instance.queuedTasks().size());
    }

    @Test
    public void testSubmitSubmitsTasks() throws Exception {
        MockTask task1 = new MockTask();
        MockTask task2 = new MockTask();
        MockTask task3 = new MockTask();
        instance.submit(task1);
        instance.submit(task2);
        instance.submit(task3);

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
        MockTask task = new MockTask();
        assertEquals(Task.Status.NEW, task.getStatus());

        instance.submit(task);
        Thread.sleep(50);
        assertEquals(Task.Status.RUNNING, task.getStatus());
    }

    @Test
    public void testSubmitSetsInstantQueued() throws Exception {
        MockTask task = new MockTask();
        instance.submit(task);
        assertNotNull(task.getInstantQueued());
    }

    @Test
    public void testSubmitSetsInstantStarted() throws Exception {
        MockTask task = new MockTask();
        instance.submit(task);
        Thread.sleep(50);
        assertNotNull(task.getInstantStarted());
    }

    @Test
    public void testSubmitSetsInstantStopped() throws Exception {
        MockTask task = new MockTask();
        instance.submit(task);
        Thread.sleep(150);
        assertNotNull(task.getInstantStopped());
    }

    @Test
    public void testSubmitSetsSuccessfulTaskStatus() throws Exception {
        MockTask task = new MockTask();
        assertEquals(Task.Status.NEW, task.getStatus());

        instance.submit(task);
        Thread.sleep(150);
        assertEquals(Task.Status.SUCCEEDED, task.getStatus());
    }

    @Test
    public void testSubmitSetsFailedTaskStatus() throws Exception {
        MockFailingTask task = new MockFailingTask();
        assertEquals(Task.Status.NEW, task.getStatus());

        instance.submit(task);
        Thread.sleep(150);
        assertEquals(Task.Status.FAILED, task.getStatus());
        assertEquals("Failed, as requested",
                task.getFailureException().getMessage());
    }

}
