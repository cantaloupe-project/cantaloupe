package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenSourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.source.MockFileSource;
import edu.illinois.library.cantaloupe.source.MockStreamSource;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HealthCheckerTest extends BaseTest {

    private HealthChecker instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new HealthChecker();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        HealthChecker.getSourceUsages().clear();
    }

    @Test
    public void testAddSourceUsage() {
        assertTrue(HealthChecker.getSourceUsages().isEmpty());

        Source source = new MockStreamSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(1, HealthChecker.getSourceUsages().size());

        // Add new source of the same class
        source = new MockStreamSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(1, HealthChecker.getSourceUsages().size());

        // Add unique source
        source = new MockFileSource();
        HealthChecker.addSourceUsage(source);
        assertEquals(2, HealthChecker.getSourceUsages().size());
    }

    @Test
    public void testCheckSuccessfully() {
        Health health = instance.check();
        assertEquals(Health.Color.GREEN, health.getColor());
        assertNull(health.getMessage());
    }

    @Test
    public void testCheckWithSourceCacheFailure() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE,
                MockBrokenSourceCache.class.getName());

        Health health = instance.check();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals("MockBrokenSourceCache: I'm broken",
                health.getMessage());
    }

    @Test
    public void testCheckWithDerivativeCacheFailure() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, "true");
        config.setProperty(Key.DERIVATIVE_CACHE,
                MockBrokenDerivativeCache.class.getName());

        Health health = instance.check();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals("MockBrokenDerivativeCache: I'm broken",
                health.getMessage());
    }

}
