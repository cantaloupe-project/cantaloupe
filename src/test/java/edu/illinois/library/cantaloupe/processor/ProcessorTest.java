package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest {

    protected static final String IMAGE = "images/jpg-rgb-64x56x8-baseline.jpg";

    static {
        Application.setConfiguration(new BaseConfiguration());
    }

    protected Format getAnySupportedSourceFormat(Processor processor)
            throws Exception {
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

    protected abstract Processor getProcessor();

    /**
     * This implementation is tile-unaware. Tile-aware processors will need to
     * override it.
     *
     * @throws Exception
     */
    @Test
    public void testGetImageInfo() throws Exception {
        ImageInfo expectedInfo = new ImageInfo(64, 56, 64, 56, Format.JPG);

        if (getProcessor() instanceof StreamProcessor) {
            StreamProcessor proc = (StreamProcessor) getProcessor();
            StreamSource streamSource = new TestStreamSource(
                    TestUtil.getFixture(IMAGE));
            proc.setStreamSource(streamSource);
            proc.setSourceFormat(Format.JPG);
            assertEquals(expectedInfo, proc.getImageInfo());
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            try {
                proc.setSourceFile(TestUtil.getFixture(IMAGE));
                proc.setSourceFormat(Format.JPG);
                assertEquals(expectedInfo.toString(),
                        proc.getImageInfo().toString());
            } catch (UnsupportedSourceFormatException e) {
                // no problem
            }
        }
    }

    @Test
    public void testProcessWithSupportedSourceFormatsAndNoOperations()
            throws Exception {
        doProcessTest(TestUtil.newOperationList());
    }

    @Test
    public void testProcessWithSupportedSourceFormatsAndNoOpOperations() throws Exception {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("bla"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.JPG);
        doProcessTest(ops);
    }

    @Test
    public void testProcessWithUnsupportedSourceFormats() throws Exception {
        Crop crop = new Crop();
        crop.setX(20f);
        crop.setY(20f);
        crop.setWidth(50f);
        crop.setHeight(50f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.8f);
        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("bla"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(15));
        ops.setOutputFormat(Format.JPG);

        final Processor proc = getProcessor();
        for (Format format : Format.values()) {
            try {
                proc.setSourceFormat(format);
                if (getProcessor().getAvailableOutputFormats().size() == 0) {
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
                        proc.process(ops, proc.getImageInfo(),
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
    public void testProcessWithCropOperation() throws Exception {
        List<Crop> crops = new ArrayList<>();
        Crop crop = new Crop();
        crop.setFull(true);
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
    public void testProcessWithScaleOperation() throws Exception {
        List<Scale> scales = new ArrayList<>();
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        scales.add(scale);
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_WIDTH);
        scale.setWidth(20);
        scales.add(scale);
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_HEIGHT);
        scale.setHeight(20);
        scales.add(scale);
        scale = new Scale();
        scale.setPercent(0.5f);
        scales.add(scale);
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setWidth(20);
        scale.setHeight(20);
        scales.add(scale);
        scale = new Scale();
        scale.setMode(Scale.Mode.NON_ASPECT_FILL);
        scale.setWidth(20);
        scale.setHeight(20);
        scales.add(scale);
        for (Scale scale_ : scales) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(scale_);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithTransposeOperation() throws Exception {
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
    public void testProcessWithRotateOperation() throws Exception {
        Rotate[] rotates = {
                new Rotate(0), new Rotate(15), new Rotate(275) };
        for (Rotate rotate : rotates) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(rotate);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithFilterOperation() throws Exception {
        for (Filter filter : Filter.values()) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(filter);
            doProcessTest(ops);
        }
    }

    @Test
    public void testProcessWithSupportedOutputFormats() throws Exception {
        Processor proc = getProcessor();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));
        Set<Format> outputFormats = proc.getAvailableOutputFormats();
        for (Format outputFormat : outputFormats) {
            OperationList ops = TestUtil.newOperationList();
            ops.setOutputFormat(outputFormat);
            doProcessTest(ops);
        }
    }

    /**
     * Tests for the presernce of all available IIIF 1.1 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIiif11Qualities() throws Exception {
        Processor proc = getProcessor();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v1.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.BITONAL);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.GRAY);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1.Quality.NATIVE);
        assertEquals(expectedQualities, proc.getSupportedIiif1_1Qualities());
    }

    /**
     * Tests for the presernce of all available IIIF 2.0 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    @Test
    public void testGetSupportedIiif20Qualities() throws Exception {
        Processor proc = getProcessor();
        proc.setSourceFormat(getAnySupportedSourceFormat(proc));

        Set<edu.illinois.library.cantaloupe.resource.iiif.v2.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.BITONAL);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.DEFAULT);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2.Quality.GRAY);
        assertEquals(expectedQualities, proc.getSupportedIiif2_0Qualities());
    }

    /**
     * Tests Processor.process() for every one of the fixtures for every source
     * format the processor supports.
     *
     * @param ops
     * @throws Exception
     */
    private void doProcessTest(OperationList ops) throws Exception {
        final Collection<File> fixtures = FileUtils.
                listFiles(TestUtil.getFixture("images"), null, false);
        final Processor proc = getProcessor();
        for (Format format : Format.values()) {
            try {
                proc.setSourceFormat(format);
                if (getProcessor().getAvailableOutputFormats().size() > 0) {
                    for (File fixture : fixtures) {
                        final String fixtureName = fixture.getName();
                        if (fixtureName.startsWith(format.name().toLowerCase())) {
                            // Don't test various compressed 16-bit TIFFs in
                            // Java2dProcessor or JaiProcessor because
                            // TIFFImageReader doesn't support them.
                            if ((this instanceof Java2dProcessorTest ||
                                    this instanceof JaiProcessorTest) &&
                                    format.equals(Format.TIF) &&
                                    fixtureName.contains("x16-") &&
                                    (fixtureName.contains("-zip") ||
                                            fixtureName.contains("-lzw"))) {
                                continue;
                            }
                            // TIFFImageReader doesn't like JPEG-encoded TIFFs
                            // either.
                            if ((this instanceof Java2dProcessorTest ||
                                    this instanceof JaiProcessorTest) &&
                                    format.equals(Format.TIF) &&
                                    StringUtils.contains(fixtureName, "-jpeg")) {
                                continue;
                            }
                            // Don't test 1x1 images as they are problematic
                            // with cropping & scaling.
                            if (fixtureName.contains("-1x1")) {
                                continue;
                            }
                            // The JAI bandcombine operation does not like to
                            // work with GIFs, apparently only when testing. (?)
                            if (this instanceof JaiProcessorTest &&
                                    ops.contains(Filter.class) &&
                                    fixture.getName().endsWith("gif")) {
                                continue;
                            }
                            doProcessTest(fixture, ops);
                        }
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    private void doProcessTest(final File fixture,
                               final OperationList opList)
            throws IOException, ProcessorException {
        final Processor proc = getProcessor();
        if (proc instanceof StreamProcessor) {
            StreamSource source = new TestStreamSource(fixture);
            ((StreamProcessor) proc).setStreamSource(source);

        }
        if (proc instanceof FileProcessor) {
            ((FileProcessor) proc).setSourceFile(fixture);
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        proc.process(opList, proc.getImageInfo(), outputStream);
        // TODO: verify that this is a valid image
        assertTrue(outputStream.toByteArray().length > 100);
    }

}
