package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ffmpeg and ffprobe binaries must be on the PATH.
 */
public class FfmpegProcessorTest extends AbstractProcessorTest {

    private FfmpegProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.FFMPEGPROCESSOR_PATH_TO_BINARIES);
        FfmpegProcessor.resetInitialization();

        instance = newInstance();
    }

    @Override
    protected Format getSupported16BitSourceFormat() {
        return null;
    }

    @Override
    protected Path getSupported16BitImage() {
        return null;
    }

    @Override
    protected FfmpegProcessor newInstance() {
        FfmpegProcessor instance = new FfmpegProcessor();
        try {
            final Format format = Format.MPG;
            final Path fixture = TestUtil.
                    getFixture("images/" + format.getPreferredExtension());
            instance.setSourceFile(fixture);
            instance.setSourceFormat(format);
        } catch (IOException e) {
            fail("Huge bug");
        }
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() {
        for (Format format : Format.values()) {
            try {
                instance = newInstance();
                Set<Format> expectedFormats = EnumSet.noneOf(Format.class);
                if (format.getType() != null &&
                        format.getType().equals(Format.Type.VIDEO)) {
                    expectedFormats.addAll(ImageWriterFactory.supportedFormats());
                }
                instance.setSourceFormat(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void testGetInitializationExceptionWithNoException() {
        assertNull(instance.getInitializationException());
    }

    @Test
    public void testGetInitializationExceptionWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.FFMPEGPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        FfmpegProcessor.resetInitialization();
        assertNotNull(instance.getInitializationException());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<ProcessorFeature> expectedFeatures = EnumSet.of(
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
                ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    @Override
    public void testGetSupportedIIIF1Qualities() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality> expectedQualities =
                EnumSet.of(
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);
        assertEquals(expectedQualities, instance.getSupportedIIIF1Qualities());
    }

    @Test
    @Override
    public void testGetSupportedIIIF2Qualities() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality> expectedQualities =
                EnumSet.of(
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);
        assertEquals(expectedQualities, instance.getSupportedIIIF2Qualities());
    }

    @Test
    public void testProcessWithTimeOption() throws Exception {
        final Info imageInfo = instance.readImageInfo();

        // time option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, imageInfo, outputStream);
        final byte[] frame1 = outputStream.toByteArray();

        // time option present
        ops.getOptions().put("time", "00:00:05");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] frame2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(frame1, frame2));
    }

    @Test
    public void testProcessWithInvalidFrameOptionThrowsException()
            throws Exception {
        final Info imageInfo = instance.readImageInfo();

        OperationList ops = TestUtil.newOperationList();
        ops.getOptions().put("time", "cats");
        OutputStream outputStream = new NullOutputStream();
        try {
            instance.process(ops, imageInfo, outputStream);
            fail("Expected exception");
        } catch (ProcessorException e) {
            // pass
        }
    }

    @Test
    public void testValidate() throws Exception {
        OperationList ops = TestUtil.newOperationList();
        Dimension fullSize = new Dimension(1000, 1000);
        instance.validate(ops, fullSize);

        // Valid time format
        ops.getOptions().put("time", "00:00:02");
        instance.validate(ops, fullSize);

        // Invalid time format
        ops.getOptions().put("time", "000012");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }

        // Time beyond the video length
        ops.getOptions().put("time", "00:38:06");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

}
