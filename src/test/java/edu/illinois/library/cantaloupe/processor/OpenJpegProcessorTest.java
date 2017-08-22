package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class OpenJpegProcessorTest extends ProcessorTest {

    private OpenJpegProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    protected Format getSupported16BitSourceFormat() throws IOException {
        return Format.JP2;
    }

    @Override
    protected File getSupported16BitImage() throws IOException {
        return TestUtil.getImage("jp2-rgb-64x56x16-monotiled-lossy.jp2");
    }

    @Override
    protected OpenJpegProcessor newInstance() {
        OpenJpegProcessor proc = new OpenJpegProcessor();
        try {
            proc.setSourceFormat(Format.JP2);
        } catch (UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return proc;
    }

    @Test
    public void getSupportedFeatures() throws Exception {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
                ProcessorFeature.ROTATION_ARBITRARY,
                ProcessorFeature.ROTATION_BY_90S,
                ProcessorFeature.SIZE_ABOVE_FULL,
                ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT,
                ProcessorFeature.SIZE_BY_HEIGHT,
                ProcessorFeature.SIZE_BY_PERCENT,
                ProcessorFeature.SIZE_BY_WIDTH,
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT));
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Override
    @Test
    public void processOf16BitImageWithEncodeOperationLimitingTo8Bits() {
        // Skipped. See OpenJpegProcessor's class doc for an explanation of why
        // it doesn't support 16-bit output.
    }

    @Override
    @Test
    public void processOf16BitImageWithEncodeOperationWithNoLimit() {
        // Skipped. See OpenJpegProcessor's class doc for an explanation of why
        // it doesn't support 16-bit output.
    }

    @Test
    public void getWarningsWithNoWarnings() {
        boolean initialValue = OpenJpegProcessor.isQuietModeSupported();
        try {
            OpenJpegProcessor.setQuietModeSupported(true);
            assertEquals(0, instance.getWarnings().size());
        } finally {
            OpenJpegProcessor.setQuietModeSupported(initialValue);
        }
    }

    @Test
    public void getWarningsWithWarnings() {
        boolean initialValue = ImageMagickProcessor.isUsingVersion7();
        try {
            OpenJpegProcessor.setQuietModeSupported(false);
            assertEquals(1, instance.getWarnings().size());
        } finally {
            OpenJpegProcessor.setQuietModeSupported(initialValue);
        }
    }

    @Test
    @Override
    public void readImageInfo() throws Exception {
        Info expectedInfo = new Info(100, 88, 100, 88, Format.JP2);

        instance.setSourceFile(TestUtil.getImage("jp2"));
        assertEquals(expectedInfo, instance.readImageInfo());

        // untiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-monotiled-lossy.jp2"));
        expectedInfo = new Info(64, 56, 64, 56, Format.JP2);
        assertEquals(expectedInfo, instance.readImageInfo());

        // tiled image
        instance.setSourceFile(TestUtil.getImage("jp2-rgb-64x56x8-multitiled-lossy.jp2"));
        expectedInfo = new Info(64, 56, Format.JP2);
        expectedInfo.getImages().get(0).tileWidth = 32;
        expectedInfo.getImages().get(0).tileHeight = 28;
        assertEquals(expectedInfo, instance.readImageInfo());
    }

}
