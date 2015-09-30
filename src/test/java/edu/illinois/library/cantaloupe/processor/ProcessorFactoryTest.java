package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class ProcessorFactoryTest extends TestCase {

    public void setUp() {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("GraphicsMagickProcessor.path_to_binaries", "/usr/local/bin");
        config.setProperty("ImageMagickProcessor.path_to_binaries", "/usr/local/bin");
        Application.setConfiguration(config);
    }

    public void testGetAllProcessors() {
        assertEquals(5, ProcessorFactory.getAllProcessors().size());
    }

    /**
     * Assert that we can get GraphicsMagickProcessor for JPEGs when
     * <code>processor.jpg = GraphicsMagickProcessor</code>
     *
     * @throws Exception
     */
    public void testGetProcessorWithSupportedAssignedFormat() throws Exception {
        Application.getConfiguration().
                setProperty("processor.jpg", "GraphicsMagickProcessor");
        assertTrue(ProcessorFactory.getProcessor(SourceFormat.JPG) instanceof GraphicsMagickProcessor);
    }

    /**
     * Assert that we get GraphicsMagickProcessor for JPEGs when:
     *
     * <code>
     * processor.webp = ImageIoProcessor
     * processor.fallback = GraphicsMagickProcessor
     * </code>
     *
     * @throws Exception
     */
    public void testGetProcessorWithUnsupportedAssignedFormat() throws Exception {
        Application.getConfiguration().
                setProperty("processor.webp", "ImageIoProcessor");
        Application.getConfiguration().
                setProperty("processor.fallback", "GraphicsMagickProcessor");
        try {
            ProcessorFactory.getProcessor(SourceFormat.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals(e.getMessage(),
                    "Processor assigned to webp, ImageIoProcessor, does not support the webp format");
        }
    }

    /**
     * Assert that we get ImageIoProcessor for JPEGs when
     * <code>processor.fallback = ImageIoProcessor</code>
     *
     * @throws Exception
     */
    public void testGetProcessorWithFormatSupportedByFallback() throws Exception {
        Application.getConfiguration().
                setProperty("processor.fallback", "ImageIoProcessor");
        assertTrue(ProcessorFactory.getProcessor(SourceFormat.JPG) instanceof ImageIoProcessor);
    }

    /**
     * Assert that an UnsupportedFormatException is thrown when we try to get
     * an unsupported format when
     * <code>processor.fallback = GraphicsMagickProcessor</code>
     *
     * @throws Exception
     */
    public void testGetProcessorWithFormatUnsupportedByFallback() throws Exception {
        Application.getConfiguration().
                setProperty("processor.fallback", "ImageIoProcessor");
        try {
            ProcessorFactory.getProcessor(SourceFormat.WEBP);
            fail("Expected exception");
        } catch (UnsupportedSourceFormatException e) {
            assertEquals(e.getMessage(), "No processor assigned to this format " +
                    "(webp), and fallback ImageIoProcessor does not support it either");
        }
    }

    /**
     * Assert that a ClassNotFoundException is thrown when there is no processor
     * assigned to the requested format and no fallback processor defined
     *
     * @throws Exception
     */
    public void testGetProcessorWithFormatUnsupportedByFallbackAndNoFallback() throws Exception {
        try {
            ProcessorFactory.getProcessor(SourceFormat.WEBP);
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
    public void testGetProcessorWithUnknownProcessor() throws Exception {
        Application.getConfiguration().
                setProperty("processor.jpg", "AmazingFakeProcessor");
        try {
            ProcessorFactory.getProcessor(SourceFormat.JPG);
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
    public void testGetProcessorWithUnknownFallbackProcessor() throws Exception {
        Application.getConfiguration().
                setProperty("processor.fallback", "AmazingFakeProcessor");
        try {
            ProcessorFactory.getProcessor(SourceFormat.JPG);
            fail("Expected exception");
        } catch (ClassNotFoundException e) {
            assertEquals(e.getMessage(),
                    "edu.illinois.library.cantaloupe.processor.AmazingFakeProcessor");
        }
    }

}
