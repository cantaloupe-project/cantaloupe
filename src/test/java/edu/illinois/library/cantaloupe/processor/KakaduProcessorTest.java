package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Quality;

import javax.imageio.stream.FileImageInputStream;
import java.awt.Dimension;
import java.util.HashSet;
import java.util.Set;

public class KakaduProcessorTest extends ProcessorTest {

    KakaduProcessor instance = new KakaduProcessor();

    public void setUp() {
        Application.getConfiguration().setProperty(
                "KakaduProcessor.path_to_binaries", "/usr/local/bin");
        Application.getConfiguration().setProperty(
                "KakaduProcessor.path_to_stdout_symlink",
                "/Users/alexd/Projects/Cantaloupe/stdout.ppm");
    }

    protected Processor getProcessor() {
        return instance;
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    @Override
    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(100, 88);
        if (getProcessor() instanceof StreamProcessor) {
            StreamProcessor proc = (StreamProcessor) getProcessor();
            Dimension actualSize = proc.getSize(
                    new FileImageInputStream(getFixture("jp2")),
                    SourceFormat.JP2);
            assertEquals(expectedSize, actualSize);
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = proc.getSize(getFixture("jp2"),
                    SourceFormat.JP2);
            assertEquals(expectedSize, actualSize);
        }
    }

    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
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
        Set<Quality> expectedQualities = new HashSet<>();
        expectedQualities.add(Quality.BITONAL);
        expectedQualities.add(Quality.COLOR);
        expectedQualities.add(Quality.DEFAULT);
        expectedQualities.add(Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedQualities(SourceFormat.UNKNOWN));
    }

}
