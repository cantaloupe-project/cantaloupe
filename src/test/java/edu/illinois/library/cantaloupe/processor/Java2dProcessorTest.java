package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.processor.imageio.ImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static edu.illinois.library.cantaloupe.processor.Processor.RESPECT_ORIENTATION_CONFIG_KEY;
import static org.junit.Assert.*;

public class Java2dProcessorTest extends ProcessorTest {

    private Java2dProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    protected Java2dProcessor newInstance() {
        return new Java2dProcessor();
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

    /**
     * Tile-aware override.
     *
     * @throws Exception
     */
    @Test
    @Override
    public void testReadImageInfo() throws Exception {
        Info expectedInfo = new Info(64, 56, Format.TIF);
        expectedInfo.getImages().get(0).tileWidth = 16;
        expectedInfo.getImages().get(0).tileHeight = 16;

        final File fixture = TestUtil.
                getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif");

        // test as a StreamProcessor
        StreamProcessor sproc = newInstance();
        StreamSource streamSource = new TestStreamSource(fixture);
        sproc.setStreamSource(streamSource);
        sproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, sproc.readImageInfo());

        // test as a FileProcessor
        FileProcessor fproc = newInstance();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.TIF);
        assertEquals(expectedInfo, fproc.readImageInfo());

        try {
            fproc.setSourceFile(TestUtil.getImage("mpg"));
            fproc.setSourceFormat(Format.MPG);
            expectedInfo = new Info(640, 360, Format.MPG);
            assertEquals(expectedInfo, fproc.readImageInfo());
        } catch (UnsupportedSourceFormatException e) {
            // pass
        }
    }

    @Test
    public void testReadImageInfoWithOrientation() throws Exception {
        ConfigurationFactory.getInstance().
                setProperty(RESPECT_ORIENTATION_CONFIG_KEY, true);

        final File fixture = TestUtil.getImage("jpg-rotated.jpg");

        final FileProcessor fproc = newInstance();
        fproc.setSourceFile(fixture);
        fproc.setSourceFormat(Format.JPG);

        final Info info = fproc.readImageInfo();
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
