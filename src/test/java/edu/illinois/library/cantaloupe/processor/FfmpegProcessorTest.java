package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.codec.ImageWriterFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * For this to work, the ffmpeg and ffprobe binaries must be on the PATH.
 */
public class FfmpegProcessorTest extends AbstractProcessorTest {

    private FfmpegProcessor instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.FFMPEGPROCESSOR_PATH_TO_BINARIES);
        FfmpegProcessor.resetInitialization();

        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
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
    void testGetAvailableOutputFormats() {
        for (Format format : Format.all()) {
            try {
                instance = newInstance();
                Set<Format> expectedFormats = new HashSet<>();
                if (format.isVideo()) {
                    expectedFormats.addAll(ImageWriterFactory.supportedFormats());
                }
                instance.setSourceFormat(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (SourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    void testGetInitializationErrorWithNoError() {
        assertNull(instance.getInitializationError());
    }

    @Test
    void testGetInitializationErrorWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.FFMPEGPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        FfmpegProcessor.resetInitialization();
        assertNotNull(instance.getInitializationError());
    }

    @Disabled // there is currently no way to support this
    @Override
    @Test
    public void testProcessWithActualFormatDifferentFromSetFormat() {
    }

    @Test
    void testProcessWithTimeOption() throws Exception {
        final Info imageInfo = instance.readInfo();

        // time option missing
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = new OperationList(new Encode(Format.JPG));
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
    void testProcessWithInvalidFrameOptionThrowsException() throws Exception {
        final Info imageInfo = instance.readInfo();

        OperationList ops = new OperationList(new Encode(Format.JPG));
        ops.getOptions().put("time", "cats");
        OutputStream outputStream = OutputStream.nullOutputStream();

        assertThrows(ProcessorException.class, () ->
                instance.process(ops, imageInfo, outputStream));
    }

    @Test
    void testValidateWithValidTime() throws Exception {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Dimension fullSize = new Dimension(1000, 1000);
        ops.getOptions().put("time", "00:00:02");

        instance.validate(ops, fullSize);
    }

    @Test
    void testValidateWithInvalidTimeFormat() {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Dimension fullSize = new Dimension(1000, 1000);
        ops.getOptions().put("time", "000012");

        assertThrows(IllegalArgumentException.class,
                () -> instance.validate(ops, fullSize));
    }

    @Test
    void testValidateWithOutOfBoundsTime() {
        OperationList ops = new OperationList(
                new Identifier("cats"), new Encode(Format.JPG));
        Dimension fullSize = new Dimension(1000, 1000);
        ops.getOptions().put("time", "00:38:06");

        assertThrows(IllegalArgumentException.class,
                () -> instance.validate(ops, fullSize));
    }

}
