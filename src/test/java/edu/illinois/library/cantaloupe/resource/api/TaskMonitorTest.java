package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.async.MockTask;
import edu.illinois.library.cantaloupe.async.Task;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class TaskMonitorTest extends BaseTest {

    private TaskMonitor instance;

    @Before
    public void setUp() {
        instance = new TaskMonitor();
    }

    /* add() */

    @Test
    public void testAdd() {
        APITask task = new PurgeDelegateMethodInvocationCacheTask();
        assertNull(instance.get(task.getUUID()));
        instance.add(task);
        assertNotNull(instance.get(task.getUUID()));
    }

    /* get() */

    @Test
    public void testGet() {
        APITask task = new PurgeDelegateMethodInvocationCacheTask();
        instance.add(task);
        assertNotNull(instance.get(task.getUUID()));
    }

}
