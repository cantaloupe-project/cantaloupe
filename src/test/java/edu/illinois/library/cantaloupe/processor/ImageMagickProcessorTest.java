package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Quality;
import org.apache.commons.configuration.BaseConfiguration;

import javax.imageio.stream.FileImageInputStream;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;

public class ImageMagickProcessorTest extends ProcessorTest {

    ImageMagickProcessor instance;

    public void setUp() {
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("ImageMagickProcessor.path_to_binaries", "/usr/local/bin"); // TODO: externalize this
        Application.setConfiguration(config);

        instance = new ImageMagickProcessor();
    }

    public void testGetAvailableOutputFormats() {
        /*
        Set<OutputFormat> expectedFormats = new HashSet<OutputFormat>();
        TODO: write this
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.JPG));
        */
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<OutputFormat>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(594, 522);
        Dimension actualSize = instance.getSize(
                new FileImageInputStream(getFixture("escher_lego.jpg")),
                SourceFormat.JPG);
        assertEquals(expectedSize, actualSize);
    }

    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = new HashSet<ProcessorFeature>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

    public void testGetSupportedQualities() {
        Set<Quality> expectedQualities = new HashSet<Quality>();
        expectedQualities.add(Quality.BITONAL);
        expectedQualities.add(Quality.COLOR);
        expectedQualities.add(Quality.DEFAULT);
        expectedQualities.add(Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedQualities(SourceFormat.UNKNOWN));
    }

    public void testProcess() {
        // This is not easily testable in code, so will have to be tested by
        // human eyes.
    }

}
