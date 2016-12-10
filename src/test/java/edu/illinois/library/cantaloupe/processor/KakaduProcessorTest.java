package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static edu.illinois.library.cantaloupe.processor.KakaduProcessor.DOWNSCALE_FILTER_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.KakaduProcessor.UPSCALE_FILTER_CONFIG_KEY;
import static org.junit.Assert.*;

public class KakaduProcessorTest extends ProcessorTest {

    private KakaduProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    protected KakaduProcessor newInstance() {
        KakaduProcessor proc = new KakaduProcessor();
        try {
            proc.setSourceFormat(Format.JP2);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void testGetDownscaleFilter() {
        assertNull(instance.getDownscaleFilter());

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bell");
        assertEquals(Scale.Filter.BELL, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bicubic");
        assertEquals(Scale.Filter.BICUBIC, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "bspline");
        assertEquals(Scale.Filter.BSPLINE, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "box");
        assertEquals(Scale.Filter.BOX, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "hermite");
        assertEquals(Scale.Filter.HERMITE, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "lanczos3");
        assertEquals(Scale.Filter.LANCZOS3, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "mitchell");
        assertEquals(Scale.Filter.MITCHELL, instance.getDownscaleFilter());
        config.setProperty(DOWNSCALE_FILTER_CONFIG_KEY, "triangle");
        assertEquals(Scale.Filter.TRIANGLE, instance.getDownscaleFilter());
    }

    @Test
    public void testGetUpscaleFilter() {
        assertNull(instance.getUpscaleFilter());

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bell");
        assertEquals(Scale.Filter.BELL, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bicubic");
        assertEquals(Scale.Filter.BICUBIC, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "bspline");
        assertEquals(Scale.Filter.BSPLINE, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "box");
        assertEquals(Scale.Filter.BOX, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "hermite");
        assertEquals(Scale.Filter.HERMITE, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "lanczos3");
        assertEquals(Scale.Filter.LANCZOS3, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "mitchell");
        assertEquals(Scale.Filter.MITCHELL, instance.getUpscaleFilter());
        config.setProperty(UPSCALE_FILTER_CONFIG_KEY, "triangle");
        assertEquals(Scale.Filter.TRIANGLE, instance.getUpscaleFilter());
    }

    @Test
    @Override
    public void testGetImageInfo() throws Exception {
        ImageInfo expectedInfo = new ImageInfo(100, 88, 100, 88, Format.JP2);

        instance.setSourceFile(TestUtil.getImage("jp2"));
        assertEquals(expectedInfo, instance.getImageInfo());

        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-monotiled-lossy.jp2"));
        expectedInfo = new ImageInfo(64, 56, 64, 56, Format.JP2);
        assertEquals(expectedInfo, instance.getImageInfo());

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedInfo = new ImageInfo(64, 56, Format.JP2);
        expectedInfo.getImages().get(0).tileWidth = 32;
        expectedInfo.getImages().get(0).tileHeight = 28;
        assertEquals(expectedInfo, instance.getImageInfo());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.REGION_SQUARE);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

}
