package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ffmpeg and ffprobe binaries must be on the PATH.
 */
public class FfmpegProcessorTest extends ProcessorTest {

    private FfmpegProcessor instance;

    @Before
    public void setUp() {
        instance = newInstance();
    }

    protected FfmpegProcessor newInstance() {
        FfmpegProcessor instance = new FfmpegProcessor();
        try {
            final Format format = Format.MPG;
            final File fixture = TestUtil.
                    getFixture("images/" + format.getPreferredExtension());
            instance.setSourceFile(fixture);
            instance.setSourceFormat(format);
        } catch (IOException | UnsupportedSourceFormatException e) {
            fail("Huge bug");
        }
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        for (Format format : Format.values()) {
            try {
                instance = newInstance();
                Set<Format> expectedFormats = new HashSet<>();
                if (format.getType() != null &&
                        format.getType().equals(Format.Type.VIDEO)) {
                    expectedFormats.add(Format.JPG);
                }
                instance.setSourceFormat(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Override
    @Test
    public void testGetImageInfo() throws Exception {
        instance.setSourceFile(TestUtil.getImage("mpg"));
        instance.setSourceFormat(Format.MPG);
        ImageInfo expectedInfo = new ImageInfo(640, 360, 640, 360, Format.MPG);
        assertEquals(expectedInfo.toString(),
                instance.getImageInfo().toString());
    }

    @Test
    public void testGetSharpenAmount() {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(FfmpegProcessor.SHARPEN_CONFIG_KEY, "0");
        assertEquals(0, instance.getSharpenAmount());
        config.setProperty(FfmpegProcessor.SHARPEN_CONFIG_KEY, "0.1");
        assertEquals(3, instance.getSharpenAmount());
        config.setProperty(FfmpegProcessor.SHARPEN_CONFIG_KEY, "0.2");
        assertEquals(3, instance.getSharpenAmount());
        config.setProperty(FfmpegProcessor.SHARPEN_CONFIG_KEY, "0.3");
        assertEquals(3, instance.getSharpenAmount());
        config.setProperty(FfmpegProcessor.SHARPEN_CONFIG_KEY, "1");
        assertEquals(13, instance.getSharpenAmount());
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<ProcessorFeature> expectedFeatures = new HashSet<>(Arrays.asList(
                ProcessorFeature.MIRRORING,
                ProcessorFeature.REGION_BY_PERCENT,
                ProcessorFeature.REGION_BY_PIXELS,
                ProcessorFeature.REGION_SQUARE,
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

    @Test
    public void testProcessWithFrameOption() throws Exception {
        final ImageInfo imageInfo = instance.getImageInfo();

        // time option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, imageInfo, outputStream);
        final byte[] zeroSecondFrame = outputStream.toByteArray();

        // time option present
        ops.getOptions().put("time", "00:00:05");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        final byte[] fiveSecondFrame = outputStream.toByteArray();

        assertFalse(Arrays.equals(zeroSecondFrame, fiveSecondFrame));
    }

    @Test
    @Override
    public void testGetSupportedIiif11Qualities() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);
        assertEquals(expectedQualities,
                instance.getSupportedIiif1_1Qualities());
    }

    @Test
    @Override
    public void testGetSupportedIiif20Qualities() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedIiif2_0Qualities());
    }

}
