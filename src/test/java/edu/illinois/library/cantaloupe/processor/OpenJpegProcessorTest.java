package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class OpenJpegProcessorTest extends ProcessorTest {

    OpenJpegProcessor instance = new OpenJpegProcessor();

    public void setUp() {
        Application.getConfiguration().setProperty(
                OpenJpegProcessor.PATH_TO_BINARIES_CONFIG_KEY, "/usr/local/bin");
    }

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    @Test
    @Override
    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(100, 88);
        if (getProcessor() instanceof ChannelProcessor) {
            ChannelProcessor proc = (ChannelProcessor) getProcessor();
            Dimension actualSize = proc.getSize(
                    new FileInputStream(TestUtil.getFixture("jp2")).getChannel(),
                    SourceFormat.JP2);
            assertEquals(expectedSize, actualSize);
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = proc.getSize(TestUtil.getFixture("jp2"),
                    SourceFormat.JP2);
            assertEquals(expectedSize, actualSize);
        }
    }

    @Test
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
                instance.getSupportedFeatures(SourceFormat.JP2));

        expectedFeatures = new HashSet<>();
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

}
