package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.NoSuchFileException;

import static org.junit.jupiter.api.Assertions.*;

abstract class AbstractSourceTest extends BaseTest {

    abstract void destroyEndpoint() throws Exception;
    abstract void initializeEndpoint() throws Exception;
    abstract Source newInstance();
    abstract void useBasicLookupStrategy();
    abstract void useScriptLookupStrategy();

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        useBasicLookupStrategy();
    }

    /* getFormatIterator() */

    @Test
    void testGetFormatIteratorConsecutiveInvocationsReturnSameInstance() {
        Source instance = newInstance();
        var it = instance.getFormatIterator();
        assertSame(it, instance.getFormatIterator());
    }

    /* newStreamFactory() */

    @Test
    void testNewStreamFactoryInvokedMultipleTimes() throws Exception {
        Source instance = newInstance();
        try {
            initializeEndpoint();
            instance.newStreamFactory();
            instance.newStreamFactory();
            instance.newStreamFactory();
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testNewStreamFactoryReturnedInstanceIsReusable()
            throws Exception {
        Source instance = newInstance();
        try {
            initializeEndpoint();
            StreamFactory source = instance.newStreamFactory();

            try (InputStream is = source.newInputStream();
                 OutputStream os = OutputStream.nullOutputStream()) {
                is.transferTo(os);
            }

            try (InputStream is = source.newInputStream();
                 OutputStream os = OutputStream.nullOutputStream()) {
                is.transferTo(os);
            }
        } finally {
            destroyEndpoint();
        }
    }

    /* stat() */

    @Test
    void testStatUsingBasicLookupStrategyWithPresentReadableImage()
            throws Exception {
        try {
            initializeEndpoint();

            newInstance().stat();
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testStatUsingBasicLookupStrategyWithMissingImage()
            throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.setIdentifier(new Identifier("bogus"));
            assertThrows(NoSuchFileException.class, instance::stat);
        } finally {
            destroyEndpoint();
        }
    }

    @Test
    void testStatReturnsCorrectInstance() throws Exception {
        try {
            initializeEndpoint();

            StatResult result = newInstance().stat();
            assertNotNull(result.getLastModified());
        } finally {
            destroyEndpoint();
        }
    }

    /**
     * Tests that {@link Source#stat()} can be invoked multiple times without
     * throwing an exception.
     */
    @Test
    void testStatInvokedMultipleTimes() throws Exception {
        try {
            initializeEndpoint();

            Source instance = newInstance();
            instance.stat();
            instance.stat();
            instance.stat();
        } finally {
            destroyEndpoint();
        }
    }

}
