package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessorFactoryTest {

    @Before
    public void setUp() {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        config.setProperty("GraphicsMagickProcessor.path_to_binaries", "/usr/local/bin");
        config.setProperty("ImageMagickProcessor.path_to_binaries", "/usr/local/bin");
    }

    @Test
    public void testGetAllProcessors() {
        assertEquals(8, ProcessorFactory.getAllProcessors().size());
    }

    /**
     * Assert that we can get Java2dProcessor for JPEGs when
     * <code>processor.jpg = Java2dProcessor</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithSupportedAssignedFormat() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.jpg", "Java2dProcessor");
        assertTrue(ProcessorFactory.getProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    /**
     * Assert that we get GraphicsMagickProcessor for JPEGs when:
     *
     * <code>
     * processor.webp = Java2dProcessor
     * processor.fallback = GraphicsMagickProcessor
     * </code>
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithUnsupportedAssignedFormat() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.webp", "Java2dProcessor");
        ConfigurationFactory.getInstance().
                setProperty("processor.fallback", "GraphicsMagickProcessor");
        try {
            ProcessorFactory.getProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals(e.getMessage(),
                    "Java2dProcessor does not support the WebP source format");
        }
    }

    /**
     * Assert that we get Java2dProcessor for JPEGs when
     * <code>processor.fallback = Java2dProcessor</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithFormatSupportedByFallback() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.fallback", "Java2dProcessor");
        assertTrue(ProcessorFactory.getProcessor(Format.JPG) instanceof Java2dProcessor);
    }

    /**
     * Assert that an UnsupportedFormatException is thrown when we try to get
     * an unsupported format when
     * <code>processor.fallback = GraphicsMagickProcessor</code>
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithFormatUnsupportedByFallback() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.fallback", "Java2dProcessor");
        try {
            ProcessorFactory.getProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals("Java2dProcessor does not support the WebP source format",
                    e.getMessage());
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when there is no processor
     * assigned to the requested format and no fallback processor defined
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithFormatUnsupportedByFallbackAndNoFallback() throws Exception {
        try {
            ProcessorFactory.getProcessor(Format.WEBP);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(), "A fallback processor is not defined.");
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when the processor
     * assigned to the requested format does not exist
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithUnknownProcessor() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.jpg", "AmazingFakeProcessor");
        try {
            ProcessorFactory.getProcessor(Format.JPG);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(),
                    "edu.illinois.library.cantaloupe.processor.AmazingFakeProcessor");
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when the processor
     * assigned to the requested format does not exist
     *
     * @throws Exception
     */
    @Test
    public void testGetProcessorWithUnknownFallbackProcessor() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty("processor.fallback", "AmazingFakeProcessor");
        try {
            ProcessorFactory.getProcessor(Format.JPG);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(),
                    "edu.illinois.library.cantaloupe.processor.AmazingFakeProcessor");
        }
    }

}
