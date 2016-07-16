package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static edu.illinois.library.cantaloupe.processor.AbstractProcessor.RESPECT_ORIENTATION_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.Java2dProcessor.DOWNSCALE_FILTER_CONFIG_KEY;
import static edu.illinois.library.cantaloupe.processor.Java2dProcessor.UPSCALE_FILTER_CONFIG_KEY;
import static org.junit.Assert.*;

public class Java2dProcessorTest extends ProcessorTest {

    Java2dProcessor instance = new Java2dProcessor();

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        final HashMap<Format,Set<Format>> formats = new HashMap<>();
        for (Format format : ImageReader.supportedFormats()) {
            formats.put(format, ImageWriter.supportedFormats());
        }

        instance.setSourceFormat(Format.JPG);
        Set<Format> expectedFormats = formats.get(Format.JPG);
        assertEquals(expectedFormats, instance.getAvailableOutputFormats());
    }

    @Test
    public void testGetDownscaleFilter() {
        assertNull(instance.getDownscaleFilter());

        final Configuration config = Configuration.getInstance();
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

        final Configuration config = Configuration.getInstance();
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

    /**
     * Tile-aware override.
     *
     * @throws Exception
     */
    @Test
    @Override
    public void testGetImageInfo() throws Exception {
        ImageInfo expectedInfo = new ImageInfo(64, 56, Format.TIF);
        expectedInfo.getImages().get(0).tileWidth = 16;
        expectedInfo.getImages().get(0).tileHeight = 16;

        final File fixture = TestUtil.
                getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif");

        // test as a StreamProcessor
        StreamProcessor sproc = (StreamProcessor) getProcessor();
        StreamSource streamSource = new TestStreamSource(fixture);
        sproc.setStreamSource(streamSource);
        sproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, sproc.getImageInfo());

        // test as a FileProcessor
        FileProcessor fproc = (FileProcessor) getProcessor();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, fproc.getImageInfo());

        try {
            fproc.setSourceFile(TestUtil.getImage("mpg"));
            fproc.setSourceFormat(Format.MPG);
            expectedInfo = new ImageInfo(640, 360, Format.MPG);
            assertEquals(expectedInfo, fproc.getImageInfo());
        } catch (UnsupportedSourceFormatException e) {
            // pass
        }
    }

    @Test
    public void testGetImageInfoWithOrientation() throws Exception {
        Configuration.getInstance().
                setProperty(RESPECT_ORIENTATION_CONFIG_KEY, true);

        final File fixture = TestUtil.getImage("jpg-rotated.jpg");

        final FileProcessor fproc = (FileProcessor) getProcessor();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.JPG);

        final ImageInfo info = fproc.getImageInfo();
        assertEquals(Orientation.ROTATE_90, info.getOrientation());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
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
