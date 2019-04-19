package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.*;

public class TaskMonitorTest extends BaseTest {

    private TaskMonitor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = TaskMonitor.getInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.setUp();
        instance = null;
        TaskMonitor.clearInstance();
    }

    /* add() */

    @Test
    void testAdd() {
        Callable<?> callable = new PurgeDelegateMethodInvocationCacheCommand();
        APITask<?> message = new APITask<>(callable);
        assertNull(instance.get(message.getUUID()));
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

    /* get() */

    @Test
    void testGet() {
        Callable<?> callable = new PurgeDelegateMethodInvocationCacheCommand();
        APITask<?> message = new APITask<>(callable);
        instance.add(message);
        assertNotNull(instance.get(message.getUUID()));
    }

    /* getAll() */

    @Test
    void testGetAll() {
        Callable<?> callable = new PurgeDelegateMethodInvocationCacheCommand();
        APITask<?> message = new APITask<>(callable);
        instance.add(message);
        assertEquals(1, instance.getAll().size());
    }

}
