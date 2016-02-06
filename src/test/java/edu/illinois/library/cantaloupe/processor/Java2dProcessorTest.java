package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

public class Java2dProcessorTest extends ProcessorTest {

    Java2dProcessor instance = new Java2dProcessor();

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        instance.setSourceFormat(SourceFormat.JPG);
        Set<OutputFormat> expectedFormats = Java2dProcessor.
                availableOutputFormats().get(SourceFormat.JPG);
        assertEquals(expectedFormats, instance.getAvailableOutputFormats());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
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
        instance.setStreamSource(new TestStreamSource(TestUtil.getImage("jpg")));
        instance.setSourceFormat(SourceFormat.JPG);
        Dimension expectedSize = new Dimension(64, 56);
        List<Dimension> tileSizes = instance.getTileSizes();
        assertEquals(1, tileSizes.size());
        assertEquals(expectedSize, tileSizes.get(0));

        // tiled image (this processor doesn't recognize tiles)
        instance.setStreamSource(new TestStreamSource(
                TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif")));
        instance.setSourceFormat(SourceFormat.TIF);
        expectedSize = new Dimension(16, 16);
        tileSizes = instance.getTileSizes();
        assertEquals(expectedSize, tileSizes.get(0));
    }

}
