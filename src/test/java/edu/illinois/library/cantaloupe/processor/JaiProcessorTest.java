package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import org.junit.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JaiProcessorTest extends ImageIOProcessorTest {

    @Override
    protected JaiProcessor newInstance() {
        return new JaiProcessor();
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        try (Processor instance = newInstance()) {
            instance.setSourceFormat(getAnySupportedSourceFormat(instance));

            Set<ProcessorFeature> expectedFeatures = EnumSet.of(
                    ProcessorFeature.MIRRORING,
                    ProcessorFeature.REGION_BY_PERCENT,
                    ProcessorFeature.REGION_BY_PIXELS,
                    ProcessorFeature.REGION_SQUARE,
                    ProcessorFeature.ROTATION_ARBITRARY,
                    ProcessorFeature.ROTATION_BY_90S,
                    ProcessorFeature.SIZE_ABOVE_FULL,
                    ProcessorFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                    ProcessorFeature.SIZE_BY_HEIGHT,
                    ProcessorFeature.SIZE_BY_PERCENT,
                    ProcessorFeature.SIZE_BY_WIDTH,
                    ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
            assertEquals(expectedFeatures, instance.getSupportedFeatures());
        }
    }

}
