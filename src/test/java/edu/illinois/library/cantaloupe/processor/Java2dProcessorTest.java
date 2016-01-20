package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class Java2dProcessorTest extends ProcessorTest {

    Java2dProcessor instance = new Java2dProcessor();

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() {
        Set<OutputFormat> expectedFormats = Java2dProcessor.
                getAvailableOutputFormats().get(SourceFormat.JPG);
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.JPG));
    }

    @Test
    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
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
                instance.getSupportedFeatures(getAnySupportedSourceFormat(instance)));

        expectedFeatures = new HashSet<>();
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

}
