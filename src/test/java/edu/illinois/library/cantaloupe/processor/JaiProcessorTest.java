package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("deprecation")
public class JaiProcessorTest extends AbstractImageIOProcessorTest {

    @Override
    protected JaiProcessor newInstance() {
        return new JaiProcessor();
    }

    @Test
    void testGetSupportedFeatures() throws Exception {
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

    @Test
    void testIsSeekingWithNonSeekableSource() throws Exception {
        try (StreamProcessor instance = newInstance()) {
            instance.setSourceFormat(Format.BMP);
            instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("bmp")));
            assertFalse(instance.isSeeking());
        }
    }

    @Test
    void testIsSeekingWithSeekableSource() throws Exception {
        try (StreamProcessor instance = newInstance()) {
            instance.setSourceFormat(Format.TIF);
            instance.setStreamFactory(new PathStreamFactory(TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-jpeg.tif")));
            assertTrue(instance.isSeeking());
        }
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

    @Test
    @Override
    public void testProcessWithTurboJPEGNotAvailable() {
        // This processor doesn't use TurboJPEG ever.
    }

}
