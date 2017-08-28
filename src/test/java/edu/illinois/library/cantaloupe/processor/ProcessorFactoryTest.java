package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessorFactoryTest extends BaseTest {

    private ProcessorFactory instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ProcessorFactory();
    }

    @Test
    public void getAllProcessors() {
        assertEquals(8, ProcessorFactory.getAllProcessors().size());
    }

    /**
     * Assert that we can get Java2dProcessor for JPEGs when
     * <code>processor.jpg = Java2dProcessor</code>.
     */
    @Test
    public void newProcessorWithSupportedAssignedFormat() throws Exception {
        Configuration.getInstance().setProperty("processor.jpg",
                Java2dProcessor.class.getSimpleName());
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    /**
     * Assert that we get GraphicsMagickProcessor for JPEGs when:
     *
     * <code>
     * processor.webp = Java2dProcessor
     * processor.fallback = GraphicsMagickProcessor
     * </code>
     */
    @Test
    public void newProcessorWithUnsupportedAssignedFormat() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty("processor.webp",
                Java2dProcessor.class.getSimpleName());
        config.setProperty(Key.PROCESSOR_FALLBACK,
                GraphicsMagickProcessor.class.getSimpleName());
        try {
            instance.newProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals(e.getMessage(),
                    "Java2dProcessor does not support the WebP source format");
        }
    }

    /**
     * Assert that we get Java2dProcessor for JPEGs when
     * <code>processor.fallback = Java2dProcessor</code>.
     */
    @Test
    public void newProcessorWithFormatSupportedByFallback() throws Exception {
        Configuration.getInstance().setProperty(Key.PROCESSOR_FALLBACK,
                Java2dProcessor.class.getSimpleName());
        assertTrue(instance.newProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    /**
     * Assert that an UnsupportedFormatException is thrown when we try to get
     * an unsupported format when
     * <code>processor.fallback = GraphicsMagickProcessor</code>.
     */
    @Test
    public void newProcessorWithFormatUnsupportedByFallback() throws Exception {
        Configuration.getInstance().setProperty(Key.PROCESSOR_FALLBACK,
                Java2dProcessor.class.getSimpleName());
        try {
            instance.newProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals("Java2dProcessor does not support the WebP source format",
                    e.getMessage());
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when there is no processor
     * assigned to the requested format and no fallback processor defined.
     */
    @Test
    public void newProcessorWithFormatUnsupportedByFallbackAndNoFallback()
            throws Exception {
        try {
            instance.newProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(), "A fallback processor is not defined.");
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when the processor
     * assigned to the requested format does not exist.
     */
    @Test
    public void newProcessorWithUnknownProcessor() throws Exception {
        Configuration.getInstance().setProperty("processor.jpg",
                "AmazingFakeProcessor");
        try {
            instance.newProcessor(Format.JPG);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(),
                    "edu.illinois.library.cantaloupe.processor.AmazingFakeProcessor");
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when the processor
     * assigned to the requested format does not exist.
     */
    @Test
    public void newProcessorWithUnknownFallbackProcessor() throws Exception {
        Configuration.getInstance().setProperty(Key.PROCESSOR_FALLBACK,
                "AmazingFakeProcessor");
        try {
            instance.newProcessor(Format.JPG);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(),
                    "edu.illinois.library.cantaloupe.processor.AmazingFakeProcessor");
        }
    }

    @Test
    public void newProcessorWithMisconfiguredProcessor() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_FALLBACK,
                GraphicsMagickProcessor.class.getSimpleName());
        config.setProperty(Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");

        try {
            instance.newProcessor(Format.JPG);
            fail("Expected InitializationException");
        } catch (InitializationException e) {
            // pass
        }
    }

}
