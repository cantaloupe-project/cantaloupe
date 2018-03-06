package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.resolver.PathStreamSource;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Ignore;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * <p>Contains tests common to all {@link Processor}s.</p>
 *
 * <p>The goal of this class is to test every {@link Processor} with every
 * {@link edu.illinois.library.cantaloupe.operation.Operation} on every
 * fixture image whose format it supports, with every output {@link Format}.</p>
 *
 * <p>Fixtures are obtained from {@link TestUtil#getImage(String)}. Fixture
 * names must start with the lowercased {@link Format#name()} and contain
 * {@literal WxHxS} (width, height, sample size) somewhere after that.</p>
 */
abstract class AbstractProcessorTest extends BaseTest {

    private static final double DELTA = 0.00000001f;

    /**
     * @return Supported 16-bit image file as returned by
     *         {@link TestUtil#getImage(String)}.
     */
    protected abstract Path getSupported16BitImage() throws IOException;

    /**
     * @return Format aligning with that of {@link #getSupported16BitImage()}.
     */
    protected abstract Format getSupported16BitSourceFormat()
            throws IOException;

    protected abstract Processor newInstance();

    private Processor newInstance(Path fixture, Format sourceFormat)
            throws UnsupportedSourceFormatException {
        Processor proc = newInstance();
        proc.setSourceFormat(sourceFormat);

        if (proc instanceof FileProcessor) {
            ((FileProcessor) proc).setSourceFile(fixture);
        } else if (proc instanceof StreamProcessor) {
            StreamSource source = new PathStreamSource(fixture);
            ((StreamProcessor) proc).setStreamSource(source);
        }
        return proc;
    }

    Format getAnySupportedSourceFormat(Processor processor) {
        return getSupportedSourceFormats(processor).stream().findFirst().
                orElse(Format.UNKNOWN);
    }

    private Set<Format> getSupportedSourceFormats(Processor processor) {
        Set<Format> formats = EnumSet.noneOf(Format.class);
        for (Format format : Format.values()) {
            try {
                processor.setSourceFormat(format);
                formats.add(format);
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
        return formats;
    }

    /* getSupportedIIIF11Qualities() */

    /**
     * Tests for the presence of all available IIIF 1.x qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF1Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality> expectedQualities =
                EnumSet.of(
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                        edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);
        assertEquals(expectedQualities, proc.getSupportedIIIF1Qualities());
    }

    /* getSupportedIIIF20Qualities() */

    /**
     * Tests for the presence of all available IIIF 2.x qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF2Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality> expectedQualities =
                EnumSet.of(
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY,
                        edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT);
        assertEquals(expectedQualities, proc.getSupportedIIIF2Qualities());
    }

    /* process() */

    @Test
    public void testProcessWithNoOperations() throws Exception {
        OperationList ops = new OperationList(new Encode(Format.JPG)); // OK, one operation, but it's required

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithOnlyNoOpOperations() throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        Rotate rotate = new Rotate(0);
        OperationList ops = new OperationList(
                crop, scale, rotate, new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithFullCropOperation() throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        OperationList ops = new OperationList(crop, new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithSquareCropOperation() throws Exception {
        Crop crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        OperationList ops = new OperationList(crop, new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    int expectedSize = (this.sourceSize.width > this.sourceSize.height) ?
                            this.sourceSize.height : this.sourceSize.width;
                    assertEquals(expectedSize, this.image.getWidth());
                    assertEquals(expectedSize, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithCropToPixelsOperation() throws Exception {
        OperationList ops = new OperationList(
                new Crop(10, 10, 35, 30),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                assertEquals(35, this.image.getWidth());
                assertEquals(30, this.image.getHeight());
            }
        });
    }

    @Test
    public void testProcessWithCropByPercentOperation() throws Exception {
        final float width = 0.2f;
        final float height = 0.2f;
        Crop crop = new Crop(0.2f, 0.2f, width, height);
        crop.setUnit(Crop.Unit.PERCENT);
        OperationList ops = new OperationList(crop, new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    // Be a little lenient.
                    int expectedW = Math.round(this.sourceSize.width * width);
                    int expectedH = Math.round(this.sourceSize.height * height);
                    assertTrue(Math.abs(expectedW - this.image.getWidth()) < 2);
                    assertTrue(Math.abs(expectedH - this.image.getHeight()) < 2);
                }
            }
        });
    }

    @Ignore // TODO: write this
    @Test
    public void testProcessWithNormalizeOperation() throws Exception {
    }

    @Test
    public void testProcessWithNullScaleOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(), new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithScaleAspectFitWidthOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(20, null, Scale.Mode.ASPECT_FIT_WIDTH),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    int expectedHeight =
                            (int) Math.round((20 / (double) this.sourceSize.width) * this.sourceSize.height);
                    assertEquals(20, this.image.getWidth());
                    assertEquals(expectedHeight, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithScaleAspectFitHeightOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(null, 20, Scale.Mode.ASPECT_FIT_HEIGHT),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    int expectedWidth =
                            (int) Math.round(20 / (double) this.sourceSize.height * this.sourceSize.width);
                    assertEquals(expectedWidth, this.image.getWidth());
                    assertEquals(20, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithDownscaleByPercentageOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(0.5f),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width * 0.5f,
                            this.image.getWidth(), DELTA);
                    assertEquals(this.sourceSize.height * 0.5f,
                            this.image.getHeight(), DELTA);
                }
            }
        });
    }

    @Test
    public void testProcessWithUpscaleByPercentageOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(1.5f),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width * 1.5f,
                            this.image.getWidth(), DELTA);
                    assertEquals(this.sourceSize.height * 1.5f,
                            this.image.getHeight(), DELTA);
                }
            }
        });
    }

    @Test
    public void testProcessWithAspectFitInsideScaleOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(20, 20, Scale.Mode.ASPECT_FIT_INSIDE),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    int expectedW = 20, expectedH = 20;
                    if (this.sourceSize.width > this.sourceSize.height) {
                        expectedH = (int) Math.round((this.sourceSize.height / (double) this.sourceSize.width) * 20);
                    } else if (this.sourceSize.width < this.sourceSize.height) {
                        expectedW = (int) Math.round((this.sourceSize.width / (double) this.sourceSize.height) * 20);
                    }
                    assertEquals(expectedW, this.image.getWidth());
                    assertEquals(expectedH, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithNonAspectFillScaleOperation() throws Exception {
        OperationList ops = new OperationList(
                new Scale(20, 20, Scale.Mode.NON_ASPECT_FILL),
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                assertEquals(20, this.image.getWidth());
                assertEquals(20, this.image.getHeight());
            }
        });
    }

    @Test
    public void testProcessWithTransposeOperation() throws Exception {
        OperationList ops = new OperationList(
                Transpose.HORIZONTAL,
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithRotate0DegreesOperation() throws Exception {
        OperationList ops = new OperationList(
                new Rotate(0), new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    assertEquals(this.sourceSize.width, this.image.getWidth());
                    assertEquals(this.sourceSize.height, this.image.getHeight());
                }
            }
        });
    }

    @Test
    public void testProcessWithRotate275DegreesOperation() throws Exception {
        OperationList ops = new OperationList(
                new Rotate(275), new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            @Override
            public void run() {
                if (this.sourceSize != null) {
                    final double radians = Math.toRadians(275);
                    double expectedW = Math.abs(this.sourceSize.width * Math.cos(radians)) +
                            Math.abs(this.sourceSize.height * Math.sin(radians));
                    double expectedH = Math.abs(this.sourceSize.width * Math.sin(radians)) +
                            Math.abs(this.sourceSize.height * Math.cos(radians));
                    expectedW = Math.round(expectedW);
                    expectedH = Math.round(expectedH);

                    // Be a little lenient. Different processors will use
                    // different antialiasing methods and have different ideas
                    // on how much to pad the edges.
                    assertTrue(Math.abs(expectedW - this.image.getWidth()) < 4);
                    assertTrue(Math.abs(expectedH - this.image.getHeight()) < 4);
                }
            }
        });
    }

    @Test
    public void testProcessWithBitonalFilterOperation() throws Exception {
        OperationList ops = new OperationList(
                ColorTransform.BITONAL,
                new Encode(Format.PNG));

        forEachFixture(ops, new ProcessorAssertion() {
            {
                if (newInstance() instanceof JaiProcessor) {
                    // These may be JAI bugs
                    this.skippedFixtures.add("bmp-rgba-64x56x8.bmp");
                    this.skippedFixtures.add("gif");
                    this.skippedFixtures.add("gif-animated-looping.gif");
                    this.skippedFixtures.add("gif-animated-non-looping.gif");
                    this.skippedFixtures.add("gif-rgb-64x56x8.gif");
                    this.skippedFixtures.add("gif-rotated.gif");
                    this.skippedFixtures.add("gif-xmp.gif");
                }
            }
            @Override
            public void run() {
                assertBitonal(this.image);
            }
        });
    }

    @Test
    public void testProcessWithGrayscaleFilterOperation() throws Exception {
        OperationList ops = new OperationList(
                ColorTransform.GRAY,
                new Encode(Format.JPG));

        forEachFixture(ops, new ProcessorAssertion() {
            {
                if (newInstance() instanceof JaiProcessor) {
                    // These may be JAI bugs
                    this.skippedFixtures.add("bmp-rgba-64x56x8.bmp");
                    this.skippedFixtures.add("gif");
                    this.skippedFixtures.add("gif-animated-looping.gif");
                    this.skippedFixtures.add("gif-animated-non-looping.gif");
                    this.skippedFixtures.add("gif-rgb-64x56x8.gif");
                    this.skippedFixtures.add("gif-rotated.gif");
                    this.skippedFixtures.add("gif-xmp.gif");
                }
            }
            @Override
            public void run() {
                assertGray(this.image);
            }
        });
    }

    @Test
    public void testProcessOf16BitImageWithEncodeOperationLimitingTo8Bits()
            throws Exception {
        final Path fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(8);

        OperationList ops = new OperationList(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(8, os.toByteArray());
    }

    @Test
    public void testProcessOf16BitImageWithEncodeOperationWithNoLimit()
            throws Exception {
        final Path fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(null);

        OperationList ops = new OperationList(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(16, os.toByteArray());
    }

    @Test
    public void testProcessWithAllSupportedOutputFormats() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));
        Set<Format> outputFormats = proc.getAvailableOutputFormats();

        for (Format outputFormat : outputFormats) {
            OperationList ops = new OperationList(new Encode(outputFormat));

            forEachFixture(ops, new ProcessorAssertion() {
                {
                    // TODO: why is this necessary?
                    Processor proc = newInstance();
                    if (proc instanceof Java2dProcessor || proc instanceof JaiProcessor) {
                        this.skippedFixtures.add("bmp-rgba-64x56x8.bmp");
                    }
                }
                @Override
                public void run() {
                    if (this.sourceSize != null) {
                        assertEquals(this.sourceSize.width, this.image.getWidth());
                    }
                }
            });
        }
    }

    /* readImageInfo() */

    /**
     * This implementation is tile-unaware. Tile-aware processors will need to
     * override it.
     */
    @Test
    public void testReadImageInfoOnAllFixtures() throws Exception {
        final Processor proc = newInstance();

        for (Format format : Format.values()) {
            try {
                // The processor will throw an exception if it doesn't support
                // this format, which is fine. No processor supports all
                // formats.
                proc.setSourceFormat(format);

                for (Path fixture : TestUtil.getImageFixtures(format)) {
                    // TODO: address these
                    if (proc instanceof GraphicsMagickProcessor) {
                        if (fixture.getFileName().toString().equals("jpg-rgb-594x522x8-baseline.jpg")) {
                            continue;
                        }
                    } else if (proc instanceof ImageMagickProcessor) {
                        if (fixture.getFileName().toString().contains("pdf")) {
                            continue;
                        }
                    }

                    if (proc instanceof StreamProcessor) {
                        StreamProcessor sproc = (StreamProcessor) proc;
                        StreamSource streamSource =
                                new PathStreamSource(fixture);
                        sproc.setStreamSource(streamSource);
                    } else if (proc instanceof FileProcessor) {
                        FileProcessor fproc = (FileProcessor) proc;
                        fproc.setSourceFile(fixture);
                    }

                    try {
                        // We don't know the dimensions of the source image and
                        // we can't get them because that would require using
                        // the method we are now testing, so the best we can do
                        // is to assert that they are nonzero.
                        final Info actualInfo = proc.readImageInfo();
                        assertEquals(format, actualInfo.getSourceFormat());
                        assertTrue(actualInfo.getSize().getWidth() > 0);
                        assertTrue(actualInfo.getSize().getHeight() > 0);

                        // Parse the resolution count from the filename, or
                        // else assert 1.
                        int expectedNumResolutions = 1;
                        if (fixture.getFileName().toString().equals("jp2")) {
                            expectedNumResolutions = 5;
                        } else {
                            Pattern pattern = Pattern.compile("\\dres");
                            Matcher matcher = pattern.matcher(fixture.getFileName().toString());
                            if (matcher.find()) {
                                expectedNumResolutions =
                                        Integer.parseInt(matcher.group(0).substring(0, 1));
                            }
                        }
                        assertEquals(expectedNumResolutions,
                                actualInfo.getNumResolutions());
                    } catch (Exception e) {
                        System.err.println(format + " : " + fixture);
                        throw e;
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // OK, continue
            }
        }
    }

    /* setSourceFormat() */

    @Test
    public void testSetSourceFormatWithUnsupportedSourceFormat() {
        for (Format format : Format.values()) {
            try {
                final Processor proc = newInstance();
                proc.setSourceFormat(format);
                if (proc.getAvailableOutputFormats().isEmpty()) {
                    fail("Expected exception");
                }
            } catch (UnsupportedSourceFormatException e) {
                // pass
            }
        }
    }

    /**
     * Tests {@link Processor#process} with every one of the fixtures for every
     * source format the processor supports.
     */
    void forEachFixture(final OperationList ops,
                        final ProcessorAssertion assertion) throws Exception {
        Files.walkFileTree(TestUtil.getFixture("images"),
                new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             BasicFileAttributes attrs) throws IOException {
                if (assertion.skippedFixtures.contains(file.getFileName().toString())) {
                    return FileVisitResult.CONTINUE;
                }

                Set<Format> supportedFormats =
                        getSupportedSourceFormats(newInstance());
                for (Format sourceFormat : supportedFormats) {
                    final String fixtureName = file.getFileName().toString();

                    Processor proc = newInstance();

                    // These are used in other tests, but ImageIO doesn't like
                    // them.
                    if (new HashSet<>(Arrays.asList(
                            "jpg-ycck.jpg", "jpg-icc-chunked.jpg")).contains(fixtureName)) {
                        continue;
                    }

                    // TODO: address these
                    if (proc instanceof Java2dProcessor || proc instanceof JaiProcessor) {
                        if (fixtureName.equals("tif-rgba-1res-64x56x8-tiled-jpeg.tif") ||
                                fixtureName.equals("tif-rgba-1res-64x56x8-striped-jpeg.tif")) {
                            continue;
                        }
                    } else if (proc instanceof GraphicsMagickProcessor) {
                        if (fixtureName.equals("jpg-rgb-594x522x8-baseline.jpg")) {
                            continue;
                        }
                    } else if (proc instanceof ImageMagickProcessor) {
                        if (fixtureName.contains("pdf")) {
                            continue;
                        }
                    }

                    // Don't test 1x1 images as they are problematic with
                    // cropping & scaling.
                    if (fixtureName.startsWith(sourceFormat.name().toLowerCase()) &&
                            !fixtureName.contains("-1x1")) {
                        // Extract the dimensions and sample size from the
                        // fixture name to use in the assertion.
                        Pattern pattern = Pattern.compile("\\d+x\\d+x\\d+");
                        Matcher matcher = pattern.matcher(fixtureName);
                        if (matcher.find()) {
                            String match = matcher.group();
                            String[] parts = match.split("x");
                            if (parts.length > 1) {
                                assertion.sourceSize = new Dimension(
                                        Integer.parseInt(parts[0]),
                                        Integer.parseInt(parts[1]));
                            }
                            if (parts.length > 2) {
                                assertion.sourceSampleSize =
                                        Integer.parseInt(parts[2]);
                            }
                        } else {
                            assertion.sourceSize = null;
                            assertion.sourceSampleSize = 0;
                        }

                        try {
                            doProcessTest(file, sourceFormat, ops, assertion);
                        } catch (Exception e) {
                            System.err.println("FAILED: " + file);
                            throw new IOException(e.getMessage(), e);
                        }
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * runs the given assertion.
     */
    private void doProcessTest(final Path fixture,
                               final Format sourceFormat,
                               final OperationList opList,
                               final ProcessorAssertion assertion) throws Exception {
        final Processor proc = newInstance(fixture, sourceFormat);
        try {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            proc.process(opList, proc.readImageInfo(), os);

            ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
            BufferedImage image = ImageIO.read(is);

            assertion.opList = opList;
            assertion.image = image;
            assertion.run();
        } catch (Exception e) {
            System.err.println("Errored fixture: " + fixture);
            System.err.println("Errored op list: " + opList);
            throw e;
        }
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * invokes {@link Processor#process} to write the result to the given
     * output stream.
     */
    private void doProcessTest(final Path fixture,
                               final Format sourceFormat,
                               final OperationList opList,
                               final OutputStream os) throws Exception {
        final Processor proc = newInstance(fixture, sourceFormat);
        try {
            proc.process(opList, proc.readImageInfo(), os);
        } catch (Exception e) {
            System.err.println("Errored fixture: " + fixture);
            System.err.println("Errored op list: " + opList);
            throw e;
        }
    }

}
