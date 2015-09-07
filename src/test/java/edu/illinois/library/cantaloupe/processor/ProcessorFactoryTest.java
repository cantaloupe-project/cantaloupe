package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

public class ProcessorFactoryTest extends TestCase {

    public void testGetAllProcessors() {
        assertEquals(3, ProcessorFactory.getAllProcessors().size());
    }

    public void testGetProcessor() {
        BaseConfiguration config = new BaseConfiguration();
        Application.setConfiguration(config);

        config.setProperty("processor.jpg", "GraphicsMagickProcessor");
        config.setProperty("processor.tif", "ImageIoProcessor");
        config.setProperty("processor.fallback", "ImageMagickProcessor");
        config.setProperty("GraphicsMagickProcessor.path_to_binaries", "/usr/local/bin");
        config.setProperty("ImageMagickProcessor.path_to_binaries", "/usr/local/bin");

        assertTrue(ProcessorFactory.getProcessor(SourceFormat.JPG) instanceof GraphicsMagickProcessor);
        assertTrue(ProcessorFactory.getProcessor(SourceFormat.TIF) instanceof ImageIoProcessor);
        assertTrue(ProcessorFactory.getProcessor(SourceFormat.BMP) instanceof ImageMagickProcessor);
        assertTrue(ProcessorFactory.getProcessor(SourceFormat.UNKNOWN) instanceof ImageMagickProcessor);
    }

}
