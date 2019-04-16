package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.Color;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;

abstract class AbstractMagickProcessorTest extends AbstractProcessorTest {

    abstract Map<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException;

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        for (Format format : Format.values()) {
            try {
                StreamProcessor instance = (StreamProcessor) newInstance();
                instance.setSourceFormat(format);
                Set<Format> expectedFormats = getAvailableOutputFormats().
                        get(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.REGION_SQUARE);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_CONFINED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_DISTORTED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testProcessWithPageOption() throws Exception {
        try (StreamProcessor proc = (StreamProcessor) newInstance()) {
            // Skip if PDF is not supported.
            try {
                proc.setSourceFormat(Format.PDF);
            } catch (UnsupportedSourceFormatException e) {
                return;
            }

            final Path fixture = TestUtil.getImage("pdf-multipage.pdf");
            byte[] page1, page2;
            Info imageInfo;

            // page option missing
            proc.setStreamFactory(new PathStreamFactory(fixture));
            imageInfo = proc.readInfo();

            java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
            OperationList ops = new OperationList();
            proc.process(ops, imageInfo, outputStream);
            page1 = outputStream.toByteArray();

            // page option present
            proc.setStreamFactory(new PathStreamFactory(fixture));

            ops = new OperationList();
            ops.getOptions().put("page", "2");
            outputStream = new java.io.ByteArrayOutputStream();
            proc.process(ops, imageInfo, outputStream);
            page2 = outputStream.toByteArray();

            assertFalse(Arrays.equals(page1, page2));
        }
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndTransparentOutputFormat()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(Key.PROCESSOR_BACKGROUND_COLOR, "blue");

        OperationList ops = new OperationList();
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.add(new Encode(Format.PNG));

        Info imageInfo = Info.builder().withSize(64, 58).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamFactory streamFactory = new PathStreamFactory(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamFactory(streamFactory);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        assertRGBA(rotatedImage.getRGB(0, 0), 0, 0, 0, 0);
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndNonTransparentOutputFormat()
            throws Exception {
        OperationList ops = new OperationList();
        Rotate rotation = new Rotate(15);
        ops.add(rotation);

        Encode encode = new Encode(Format.JPG);
        encode.setBackgroundColor(Color.fromString("#0000FF"));
        ops.add(encode);

        Info imageInfo = Info.builder().withSize(64, 58).build();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = (StreamProcessor) newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamFactory streamFactory = new PathStreamFactory(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamFactory(streamFactory);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        int pixel = rotatedImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(255, alpha);
        assertTrue(red < 10);
        assertTrue(green < 20);
        assertTrue(blue > 230);
    }

    @Override
    @Ignore // This processor doesn't support XMP writing.
    @Test
    public void testProcessWritesXMPMetadataIntoGIF() throws Exception {
        super.testProcessWritesXMPMetadataIntoGIF();
    }

    @Override
    @Ignore // This processor doesn't support XMP writing.
    @Test
    public void testProcessWritesXMPMetadataIntoJPEG() throws Exception {
        super.testProcessWritesXMPMetadataIntoJPEG();
    }

    @Override
    @Ignore // This processor doesn't support XMP writing.
    @Test
    public void testProcessWritesXMPMetadataIntoPNG() throws Exception {
        super.testProcessWritesXMPMetadataIntoPNG();
    }

    @Override
    @Ignore // This processor doesn't support XMP writing.
    @Test
    public void testProcessWritesXMPMetadataIntoTIFF() throws Exception {
        super.testProcessWritesXMPMetadataIntoTIFF();
    }

    /* readInfo() */

    /**
     * Override that doesn't check {@link Info#getNumResolutions()}.
     */
    @Override
    @Test
    public void testReadInfoOnAllFixtures() throws Exception {
        try (final Processor proc = newInstance()) {
            for (Format format : Format.values()) {
                try {
                    proc.setSourceFormat(format);

                    for (Path fixture : TestUtil.getImageFixtures(format)) {
                        // TODO: address this
                        if (fixture.getFileName().toString().equals("jpg-rgb-594x522x8-baseline.jpg") ||
                                fixture.getFileName().toString().contains("pdf")) {
                            continue;
                        }

                        StreamProcessor sproc = (StreamProcessor) proc;
                        StreamFactory streamFactory = new PathStreamFactory(fixture);
                        sproc.setStreamFactory(streamFactory);

                        try {
                            // We don't know the dimensions of the source image
                            // and we can't get them because that would require
                            // using the method we are now testing, so the best
                            // we can do is to assert that they are nonzero.
                            final Info actualInfo = proc.readInfo();
                            assertEquals(format, actualInfo.getSourceFormat());
                            assertTrue(actualInfo.getSize().width() >= 1);
                            assertTrue(actualInfo.getSize().height() >= 1);

                            assertEquals(-1, actualInfo.getNumResolutions());
                        } catch (Exception e) {
                            System.err.println(format + " : " + fixture);
                            throw e;
                        }
                    }
                } catch (UnsupportedSourceFormatException ignore) {
                    // The processor doesn't support this format, which is
                    // fine. No processor supports all formats.
                }
            }
        }
    }

    @Test
    public void testReadInfoReturnsAnIncompleteInstance() throws Exception {
        try (final StreamProcessor proc = (StreamProcessor) newInstance()) {
            final Path fixture = TestUtil.getImage("jpg");
            StreamFactory streamFactory = new PathStreamFactory(fixture);
            proc.setStreamFactory(streamFactory);
            proc.setSourceFormat(Format.JPG);

            final Info info = proc.readInfo();
            assertFalse(info.isComplete());
        }
    }

    /* validate() */

    @Test
    public void testValidate() throws Exception {
        try (StreamProcessor proc = (StreamProcessor) newInstance()) {
            // Skip if PDF is not supported.
            try {
                proc.setSourceFormat(Format.PDF);
            } catch (UnsupportedSourceFormatException e) {
                return;
            }

            proc.setStreamFactory(new PathStreamFactory(
                    TestUtil.getImage("pdf.pdf")));

            OperationList ops = new OperationList(
                    new Identifier("cats"), new Encode(Format.JPG));
            Dimension fullSize = new Dimension(1000, 1000);
            proc.validate(ops, fullSize);

            ops.getOptions().put("page", "1");
            proc.validate(ops, fullSize);

            ops.getOptions().put("page", "0");
            try {
                proc.validate(ops, fullSize);
                fail("Expected exception");
            } catch (ValidationException e) {
                // pass
            }

            ops.getOptions().put("page", "-1");
            try {
                proc.validate(ops, fullSize);
                fail("Expected exception");
            } catch (ValidationException e) {
                // pass
            }
        }
    }

}
