package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Callable;

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
        Callable<?> callable = new PurgeDelegateMethodInvocationCacheCommand();
        APITask<?> message = new APITask<>(callable);
        assertNull(instance.get(message.getUUID()));
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

    /* get() */

    @Test
    public void testGet() {
        Callable<?> callable = new PurgeDelegateMethodInvocationCacheCommand();
        APITask<?> message = new APITask<>(callable);
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

}
