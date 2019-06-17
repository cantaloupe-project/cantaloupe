package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.codec.ImageReader;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class Java2dProcessorTest extends AbstractImageIOProcessorTest {

    private Java2dProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @Override
    protected Java2dProcessor newInstance() {
        return new Java2dProcessor();
    }

    @Test
    void testIsSeekingWithNonSeekableSource() throws Exception {
        instance.setSourceFormat(Format.BMP);
        instance.setSourceFile(TestUtil.getImage("bmp"));
        assertFalse(instance.isSeeking());
    }

    @Test
    void testIsSeekingWithSeekableSource() throws Exception {
        instance.setSourceFormat(Format.TIF);
        instance.setSourceFile(TestUtil.getImage("tif-rgb-1res-64x56x8-tiled-jpeg.tif"));
        assertTrue(instance.isSeeking());
    }

    @Test
    void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));

        Set<ProcessorFeature> expectedFeatures = Set.of(
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

    @Test
    void testProcessWithAnimatedGIF() throws Exception {
        Path image = TestUtil.getImage("gif-animated-looping.gif");
        OperationList ops = new OperationList(new Encode(Format.GIF));
        Info info = Info.builder()
                .withSize(136, 200)
                .withFormat(Format.GIF)
                .build();

        instance.setSourceFile(image);
        instance.setSourceFormat(Format.GIF);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            instance.process(ops, info, os);

            try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                ImageReader reader = null;
                try {
                    reader = new ImageReaderFactory().newImageReader(is, Format.GIF);
                    assertEquals(2, reader.getNumImages());
                } finally {
                    if (reader != null) {
                        reader.dispose();
                    }
                }
            }
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
