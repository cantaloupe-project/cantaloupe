package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Crop;
import edu.illinois.library.cantaloupe.image.Filter;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.image.Scale;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Transpose;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends CantaloupeTestCase {

    private static final String IMAGE = "escher_lego.jpg";

    static {
        Application.setConfiguration(new BaseConfiguration());
    }

    protected SourceFormat getAnySupportedSourceFormat(Processor processor) {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (processor.getAvailableOutputFormats(sourceFormat).size() > 0) {
                return sourceFormat;
            }
        }
        return null;
    }

    protected abstract Processor getProcessor();

    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(594, 522);
        if (getProcessor() instanceof StreamProcessor) {
            StreamProcessor proc = (StreamProcessor) getProcessor();
            try (InputStream inputStream = new FileInputStream(
                    TestUtil.getFixture(IMAGE))) {
                Dimension actualSize = proc.getSize(inputStream,
                        SourceFormat.JPG);
                assertEquals(expectedSize, actualSize);
            }
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = null;
            if (proc.getAvailableOutputFormats(SourceFormat.JPG).size() > 0) {
                actualSize = proc.getSize(TestUtil.getFixture(IMAGE),
                        SourceFormat.JPG);
            } else if (proc.getAvailableOutputFormats(SourceFormat.MPG).size() > 0) {
                expectedSize = new Dimension(640, 360);
                actualSize = proc.getSize(TestUtil.getFixture("mpg"),
                        SourceFormat.MPG);
            }
            assertEquals(expectedSize, actualSize);
        }
    }

    public void testProcessWithSupportedSourceFormatsAndNoOperations()
            throws Exception {
        doProcessTest(TestUtil.newOperationList());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.JPG);
        doProcessTest(ops);
    }

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
        ops.setOutputFormat(OutputFormat.JPG);
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() == 0) {
                if (getProcessor() instanceof StreamProcessor) {
                    InputStream sizeInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    InputStream processInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        proc.process(ops, sourceFormat, size,
                                processInputStream, new NullOutputStream());
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        assertEquals("Unsupported source format: " +
                                        sourceFormat.getPreferredExtension(),
                                e.getMessage());
                    } finally {
                        sizeInputStream.close();
                        processInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    try {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(
                                sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(ops, sourceFormat, size,
                                file, new NullOutputStream());
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        assertEquals("Unsupported source format: " +
                                sourceFormat.getPreferredExtension(),
                                e.getMessage());
                    }
                }
            }
        }
    }

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

    public void testProcessWithRotateOperation() throws Exception {
        Rotate[] rotates = {
                new Rotate(0), new Rotate(15), new Rotate(275) };
        for (Rotate rotate : rotates) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(rotate);
            doProcessTest(ops);
        }
    }

    public void testProcessWithFilterOperation() throws Exception {
        for (Filter filter : Filter.values()) {
            OperationList ops = TestUtil.newOperationList();
            ops.add(filter);
            doProcessTest(ops);
        }
    }

    public void testProcessWithSupportedOutputFormats() throws Exception {
        Set<OutputFormat> outputFormats = getProcessor().
                getAvailableOutputFormats(SourceFormat.JPG);
        for (OutputFormat outputFormat : outputFormats) {
            OperationList ops = TestUtil.newOperationList();
            ops.setOutputFormat(outputFormat);
            doProcessTest(ops);
        }
    }

    /**
     * Tests for the presernce of all available IIIF 1.1 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    public void testGetSupportedIiif11Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.BITONAL);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.GRAY);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v1_1.Quality.NATIVE);
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(getAnySupportedSourceFormat(getProcessor())));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(SourceFormat.UNKNOWN));
    }

    /**
     * Tests for the presernce of all available IIIF 2.0 qualities. Subclasses
     * must override if they lack support for any of these.
     */
    public void testGetSupportedIiif20Qualities() {
        Set<edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality>
                expectedQualities = new HashSet<>();
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.BITONAL);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.COLOR);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.DEFAULT);
        expectedQualities.add(
                edu.illinois.library.cantaloupe.resource.iiif.v2_0.Quality.GRAY);
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif2_0Qualities(getAnySupportedSourceFormat(getProcessor())));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                getProcessor().getSupportedIiif1_1Qualities(SourceFormat.UNKNOWN));
    }

    private void doProcessTest(OperationList ops) throws Exception {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                if (getProcessor() instanceof StreamProcessor) {
                    InputStream sizeInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    InputStream processInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream,
                                sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(ops, sourceFormat, size,
                                processInputStream, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100); // TODO: actually read this
                    } finally {
                        sizeInputStream.close();
                        processInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    FileProcessor proc = (FileProcessor) getProcessor();
                    File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                    Dimension size = proc.getSize(file, sourceFormat);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    proc.process(ops, sourceFormat, size, file, outputStream);
                    assertTrue(outputStream.toByteArray().length > 100); // TODO: actually read this
                }
            }
        }
    }

}
