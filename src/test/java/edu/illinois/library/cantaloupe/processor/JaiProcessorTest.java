package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.processor.imageio.ImageIoImageReader;
import edu.illinois.library.cantaloupe.processor.imageio.ImageIoImageWriter;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JaiProcessorTest extends ProcessorTest {

    static {
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    JaiProcessor instance;

    @Before
    public void setUp() {
        instance = new JaiProcessor();
    }

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testAvailableOutputFormats() throws Exception {
        final HashMap<Format,Set<Format>> formats = new HashMap<>();
        for (Format format : ImageIoImageReader.supportedFormats()) {
            formats.put(format, ImageIoImageWriter.supportedFormats());
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
