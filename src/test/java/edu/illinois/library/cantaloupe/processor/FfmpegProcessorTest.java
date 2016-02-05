package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ffmpeg and ffprobe binaries must be on the PATH.
 */
public class FfmpegProcessorTest extends ProcessorTest {

    FfmpegProcessor instance;

    protected Processor getProcessor() {
        return instance;
    }

    @Before
    public void setUp() {
        instance = new FfmpegProcessor();
    }

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            try {
                Set<OutputFormat> expectedFormats = new HashSet<>();
                if (sourceFormat.getType() != null &&
                        sourceFormat.getType().equals(SourceFormat.Type.VIDEO)) {
                    expectedFormats.add(OutputFormat.JPG);
                }
                instance.setSourceFormat(sourceFormat);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        //expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
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
    public void testProcessWithFrameOption() throws Exception {
        final SourceFormat sourceFormat = SourceFormat.MPG;
        final File fixture = TestUtil.
                getFixture("images/" + sourceFormat.getPreferredExtension());
        instance.setSourceFile(fixture);
        instance.setSourceFormat(sourceFormat);
        final Dimension size = instance.getSize();

        // time option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, size, outputStream);
        final byte[] zeroSecondFrame = outputStream.toByteArray();

        // time option present
        ops.getOptions().put("time", "00:00:05");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, size, outputStream);
        final byte[] fiveSecondFrame = outputStream.toByteArray();

        assertFalse(Arrays.equals(zeroSecondFrame, fiveSecondFrame));
    }

    @Test
    @Override
    public void testGetSupportedIiif11Qualities() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(getProcessor()));
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
        instance.setSourceFormat(getAnySupportedSourceFormat(getProcessor()));
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
