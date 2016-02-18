package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class KakaduProcessorTest extends ProcessorTest {

    KakaduProcessor instance;

    @Before
    public void setUp() throws Exception {
        Application.getConfiguration().setProperty(
                KakaduProcessor.PATH_TO_BINARIES_CONFIG_KEY, "/usr/local/bin");

        instance = new KakaduProcessor();
        instance.setSourceFormat(Format.JP2);
    }

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    @Override
    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(100, 88);
        instance.setSourceFile(TestUtil.getImage("jp2"));
        assertEquals(expectedSize, instance.getSize());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
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
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testGetTileSizes() throws Exception {
        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-monotiled-lossy.jp2"));
        Dimension expectedSize = new Dimension(56, 64);
        List<Dimension> tileSizes = instance.getTileSizes();
        assertEquals(1, tileSizes.size());
        assertEquals(expectedSize, tileSizes.get(0));

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedSize = new Dimension(32, 28);
        tileSizes = instance.getTileSizes();
        assertEquals(expectedSize, tileSizes.get(0));
    }

}
