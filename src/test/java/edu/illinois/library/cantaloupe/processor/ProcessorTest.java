package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.ColorTransform;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Transpose;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.NullOutputStream;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static edu.illinois.library.cantaloupe.test.Assert.ImageAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends BaseTest {

    protected static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    Format getAnySupportedSourceFormat(Processor processor) throws Exception {
        for (Format format : Format.values()) {
            try {
                processor.setSourceFormat(format);
                return format;
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
        return null;
    }

    /**
     * @return Format aligning with that of {@link #getSupported16BitImage()}.
     */
    protected abstract Format getSupported16BitSourceFormat()
            throws IOException;

    /**
     * @return Supported 16-bit image file as returned by
     *         {@link TestUtil#getImage(String)}.
     */
    protected abstract File getSupported16BitImage() throws IOException;

    protected abstract Processor newInstance();

    /* readImageInfo() */

    /**
     * This implementation is tile-unaware. Tile-aware processors will need to
     * override it.
     */
    @Test
    public void readImageInfo() throws Exception {
        Info expectedInfo = new Info(64, 56, 64, 56, Format.JPG);

        Processor proc = newInstance();
        if (proc instanceof StreamProcessor) {
            StreamProcessor sproc = (StreamProcessor) proc;
            StreamSource streamSource = new TestStreamSource(
                    TestUtil.getImage(IMAGE));
            sproc.setStreamSource(streamSource);
            sproc.setSourceFormat(Format.JPG);
            assertEquals(expectedInfo, sproc.readImageInfo());
        }
        if (proc instanceof FileProcessor) {
            FileProcessor fproc = (FileProcessor) proc;
            try {
                fproc.setSourceFile(TestUtil.getImage(IMAGE));
                fproc.setSourceFormat(Format.JPG);
                assertEquals(expectedInfo.toString(),
                        fproc.readImageInfo().toString());
            } catch (UnsupportedSourceFormatException e) {
                // no problem
            }
        }
    }

    /* process() */

    @Test
    public void processWithSupportedSourceFormatsAndNoOperations()
            throws Exception {
        doProcessTest(TestUtil.newOperationList());
    }

    @Test
    public void processWithSupportedSourceFormatsAndNoOpOperations()
            throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        OperationList ops = new OperationList(new Identifier("bla"), Format.JPG);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        doProcessTest(ops);
    }

    @Test
    public void processWithUnsupportedSourceFormats() throws Exception {
        Crop crop = new Crop();
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale(0.8f);
        OperationList ops = new OperationList(new Identifier("bla"), Format.JPG);
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(15));

        for (Format format : Format.values()) {
            try {
                final Processor proc = newInstance();
                proc.setSourceFormat(format);
                if (proc.getAvailableOutputFormats().size() == 0) {
                    final Collection<File> fixtures = TestUtil.
                            getImageFixtures(format);
                    final File fixture = (File) fixtures.toArray()[0];
                    if (proc instanceof StreamProcessor) {
                        StreamSource source = new TestStreamSource(fixture);
                        ((StreamProcessor) proc).setStreamSource(source);
                    }
                    if (proc instanceof FileProcessor) {
                        File file = TestUtil.getImage(
                                format.getPreferredExtension());
                        ((FileProcessor) proc).setSourceFile(file);
                    }
                    try {
                        proc.process(ops, proc.readImageInfo(),
                                new NullOutputStream());
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        assertEquals("Unsupported source format: " +
                                        format.getPreferredExtension(),
                                e.getMessage());
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void processWithCropOperation() throws Exception {
        List<Crop> crops = new ArrayList<>();
        Crop crop = new Crop();
        crop.setFull(true);
        crops.add(crop);

        crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        crops.add(crop);

        crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        crops.add(crop);

        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        crops.add(crop);

        for (Crop crop_ : crops) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(crop_);
            doProcessTest(ops);
        }
    }

    @Test
    public void processWithNormalizeOperation() {
        // TODO: write this
    }

    @Test
    public void processWithScaleOperation() throws Exception {
        List<Scale> scales = new ArrayList<>();
        Scale scale = new Scale();
        scales.add(scale);
        scale = new Scale(20, null, Scale.Mode.ASPECT_FIT_WIDTH);
        scales.add(scale);
        scale = new Scale(null, 20, Scale.Mode.ASPECT_FIT_HEIGHT);
        scales.add(scale);
        scale = new Scale(0.5f);
        scales.add(scale);
        scale = new Scale(20, 20, Scale.Mode.ASPECT_FIT_INSIDE);
        scales.add(scale);
        scale = new Scale(20, 20, Scale.Mode.NON_ASPECT_FILL);
        scales.add(scale);
        for (Scale scale_ : scales) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(scale_);
            doProcessTest(ops);
        }
    }

    @Test
    public void processWithTransposeOperation() throws Exception {
        List<Transpose> transposes = new ArrayList<>();
        transposes.add(Transpose.HORIZONTAL);
        // we aren't using this yet
        //transposes.add(new Transpose(Transpose.Axis.VERTICAL));
        for (Transpose transpose : transposes) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(transpose);
            doProcessTest(ops);
        }
    }

    @Test
    public void processWithRotateOperation() throws Exception {
        Rotate[] rotates = {
                new Rotate(0), new Rotate(15), new Rotate(275) };
        for (Rotate rotate : rotates) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(rotate);
            doProcessTest(ops);
        }
    }

    @Test
    public void processWithFilterOperation() throws Exception {
        for (ColorTransform transform : ColorTransform.values()) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(transform);
            doProcessTest(ops);
        }
    }

    @Test
    public void processOf16BitImageWithEncodeOperationLimitingTo8Bits()
            throws Exception {
        final File fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(8);

        OperationList ops = new OperationList(new Identifier("cats"),
                Format.PNG);
        ops.add(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(8, os.toByteArray());
    }

    @Test
    public void processOf16BitImageWithEncodeOperationWithNoLimit()
            throws Exception {
        final File fixture = getSupported16BitImage();
        assumeNotNull(fixture);

        final Format sourceFormat = getSupported16BitSourceFormat();

        final Encode encode = new Encode(Format.PNG);
        encode.setMaxSampleSize(null);

        OperationList ops = new OperationList(new Identifier("cats"), Format.PNG);
        ops.setOutputFormat(Format.PNG);
        ops.add(encode);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        doProcessTest(fixture, sourceFormat, ops, os);

        assertSampleSize(16, os.toByteArray());
    }

    @Test
    public void processWithAllSupportedOutputFormats() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));
        Set<Format> outputFormats = proc.getAvailableOutputFormats();
        for (Format outputFormat : outputFormats) {
            OperationList ops = TestUtil.newOperationList();
            ops.setOutputFormat(outputFormat);
            doProcessTest(ops);
        }
    }

    /* getSupportedIiif11Qualities() */

    /**
     * Tests for the presence of all available IIIF 1.1 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF1Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                expectedQualities = new HashSet<>(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE));
        assertEquals(expectedQualities, proc.getSupportedIIIF1Qualities());
    }

    /* getSupportedIiif20Qualities() */

    /**
     * Tests for the presence of all available IIIF 2.0 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIIIF2Qualities() throws Exception {
        Processor proc = newInstance();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                expectedQualities = new HashSet<>(Arrays.asList(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY,
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT));
        assertEquals(expectedQualities, proc.getSupportedIIIF2Qualities());
    }

    /**
     * Tests {@link Processor#process} for every one of the fixtures for every
     * source format the processor supports.
     */
    private void doProcessTest(OperationList ops) throws Exception {
        final Collection<File> fixtures = FileUtils.
                listFiles(TestUtil.getFixture("images"), null, false);

        for (Format sourceFormat : Format.values()) {
            try {
                if (newInstance().getAvailableOutputFormats().size() > 0) {
                    for (File fixture : fixtures) {
                        final String fixtureName = fixture.getName();
                        if (fixtureName.startsWith(sourceFormat.name().toLowerCase())) {
                            // Don't test 1x1 images as they are problematic
                            // with cropping & scaling.
                            if (fixtureName.contains("-1x1")) {
                                continue;
                            }
                            doProcessTest(fixture, sourceFormat, ops);
                        }
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * tests that {@link Processor#process} writes a result without throwing
     * any exceptions.
     */
    private void doProcessTest(final File fixture,
                               final Format sourceFormat,
                               final OperationList opList)
            throws IOException, ProcessorException {
        final Processor proc = newConfiguredProcessor(fixture, sourceFormat);
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            proc.process(opList, proc.readImageInfo(), outputStream);
            // TODO: verify that this is a valid image
            assertTrue(outputStream.toByteArray().length > 100);
        } catch (Exception e) {
            System.out.println("Fixture: " + fixture);
            System.out.println("Ops: " + opList);
            throw e;
        }
    }

    /**
     * Instantiates a processor, configures it with the given arguments, and
     * invokes {@link Processor#process} to write the result to the given
     * output stream.
     */
    private void doProcessTest(final File fixture,
                               final Format sourceFormat,
                               final OperationList opList,
                               final OutputStream os)
            throws IOException, ProcessorException {
        final Processor proc = newConfiguredProcessor(fixture, sourceFormat);
        try {
            proc.process(opList, proc.readImageInfo(), os);
        } catch (Exception e) {
            System.out.println("Fixture: " + fixture);
            System.out.println("Ops: " + opList);
            throw e;
        }
    }

    private Processor newConfiguredProcessor(File fixture, Format sourceFormat)
            throws UnsupportedSourceFormatException {
        Processor proc = newInstance();
        proc.setSourceFormat(sourceFormat);

        if (proc instanceof FileProcessor) {
            ((FileProcessor) proc).setSourceFile(fixture);
        } else if (proc instanceof StreamProcessor) {
            StreamSource source = new TestStreamSource(fixture);
            ((StreamProcessor) proc).setStreamSource(source);
        }
        return proc;
    }

}
