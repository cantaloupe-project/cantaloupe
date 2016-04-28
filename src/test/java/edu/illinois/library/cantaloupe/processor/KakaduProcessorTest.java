package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class KakaduProcessorTest extends ProcessorTest {

    KakaduProcessor instance;

    @Before
    public void setUp() throws Exception {
        Configuration.getInstance().setProperty(
                KakaduProcessor.PATH_TO_BINARIES_CONFIG_KEY, "/usr/local/bin");

        instance = new KakaduProcessor();
        instance.setSourceFormat(Format.JP2);
    }

    protected Processor getProcessor() {
        return instance;
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
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

}
