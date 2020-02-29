package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.cache.MockBrokenDerivativeCache;
import edu.illinois.library.cantaloupe.cache.MockBrokenSourceCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.FileProcessor;
import edu.illinois.library.cantaloupe.processor.MockBrokenFileProcessor;
import edu.illinois.library.cantaloupe.processor.MockFileProcessor;
import edu.illinois.library.cantaloupe.processor.MockStreamProcessor;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.source.MockFileSource;
import edu.illinois.library.cantaloupe.source.MockStreamSource;
import edu.illinois.library.cantaloupe.source.Source;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HealthCheckerTest extends BaseTest {

    private HealthChecker instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new HealthChecker();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        HealthChecker.getSourceProcessorPairs().clear();
    }

    @Test
    void testAddSourceProcessorPair() {
        assertTrue(HealthChecker.getSourceProcessorPairs().isEmpty());

        Source source     = new MockStreamSource();
        Processor proc    = new MockStreamProcessor();
        OperationList ops = new OperationList();
        HealthChecker.addSourceProcessorPair(source, proc, ops);
        assertEquals(1, HealthChecker.getSourceProcessorPairs().size());

        // Add new source & processor of the same class
        source = new MockStreamSource();
        proc = new MockStreamProcessor();
        HealthChecker.addSourceProcessorPair(source, proc, ops);
        assertEquals(1, HealthChecker.getSourceProcessorPairs().size());

        // Add unique source
        source = new MockFileSource();
        proc = new MockStreamProcessor();
        HealthChecker.addSourceProcessorPair(source, proc, ops);
        assertEquals(2, HealthChecker.getSourceProcessorPairs().size());

        // Add unique processor
        source = new MockStreamSource();
        proc = new MockFileProcessor();
        HealthChecker.addSourceProcessorPair(source, proc, ops);
        assertEquals(3, HealthChecker.getSourceProcessorPairs().size());
    }

    @Test
    void testCheckSuccessfully() {
        Health health = instance.check();
        assertEquals(Health.Color.GREEN, health.getColor());
        assertNull(health.getMessage());
    }

    @Test
    void testCheckWithProcessorFailure() throws IOException {
        Identifier identifier = new Identifier("cats");
        Path file = TestUtil.getImage("jpg");

        Source source = new MockFileSource() {
            @Override
            public Iterator<Format> getFormatIterator() {
                return List.of(Format.JPG).iterator();
            }
        };
        source.setIdentifier(identifier);
        FileProcessor proc = new MockBrokenFileProcessor();
        proc.setSourceFile(file);
        proc.setSourceFormat(Format.JPG);

        HealthChecker.addSourceProcessorPair(source, proc, new OperationList());

        Health health = instance.check();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals("I'm broken (cats -> " + getClass().getName() + "$1 -> " +
                        "edu.illinois.library.cantaloupe.processor.MockBrokenFileProcessor)",
                health.getMessage());
    }

    @Test
    void testCheckWithSourceCacheFailure() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SOURCE_CACHE,
                MockBrokenSourceCache.class.getName());

        Health health = instance.check();
        assertEquals(Health.Color.RED, health.getColor());
        assertEquals("MockBrokenSourceCache: I'm broken",
                health.getMessage());
    }

    @Test
    void testCheckWithDerivativeCacheFailure() {
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
