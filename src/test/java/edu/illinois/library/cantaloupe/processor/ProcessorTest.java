package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.restlet.data.Form;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Set;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends CantaloupeTestCase {

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
                    TestUtil.getFixture("escher_lego.jpg"))) {
                Dimension actualSize = proc.getSize(inputStream, SourceFormat.JPG);
                assertEquals(expectedSize, actualSize);
            }
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = null;
            if (proc.getAvailableOutputFormats(SourceFormat.JPG).size() > 0) {
                actualSize = proc.getSize(TestUtil.getFixture("escher_lego.jpg"),
                        SourceFormat.JPG);
            } else if (proc.getAvailableOutputFormats(SourceFormat.MPG).size() > 0) {
                expectedSize = new Dimension(640, 360);
                actualSize = proc.getSize(TestUtil.getFixture("mpg"),
                        SourceFormat.MPG);
            }
            assertEquals(expectedSize, actualSize);
        }
    }

    public void testProcessWithSupportedSourceFormatsAndNoTransformation() throws Exception {
        Parameters params = new Parameters("bla", "full", "full", "0",
                "default", "jpg");
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                if (getProcessor() instanceof StreamProcessor) {
                    InputStream processInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    InputStream sizeInputStream = new FileInputStream(
                            TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, new Form(), sourceFormat, size,
                                processInputStream, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    } finally {
                        processInputStream.close();
                        sizeInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    FileProcessor proc = (FileProcessor) getProcessor();
                    File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                    Dimension size = proc.getSize(file, sourceFormat);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    proc.process(params, new Form(), sourceFormat, size, file,
                            outputStream);
                    assertTrue(outputStream.toByteArray().length > 100);
                }
            }
        }
    }

    public void testProcessWithUnsupportedSourceFormats() throws Exception {
        Parameters params = new Parameters("bla", "20,20,50,50", "pct:80",
                "15", "color", "jpg");
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
                        proc.process(params, new Form(), sourceFormat, size,
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
                        proc.process(params, new Form(), sourceFormat, size,
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

    public void testProcessWithRegionTransformation() throws Exception {
        String[] regions = {"full", "10,10,50,50", "pct:20,20,20,20"};
        for (String region : regions) {
            Parameters params = new Parameters("bla", region, "full", "0",
                    "default", "jpg");
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
                            proc.process(params, new Form(), sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
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
                        proc.process(params, new Form(), sourceFormat, size,
                                file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithSizeTransformation() throws Exception {
        String[] sizes = {"full", "20,", ",20", "pct:50", "20,20", "!20,20"};
        for (String size : sizes) {
            Parameters params = new Parameters("bla", "10,10,50,50", size, "0",
                    "default", "jpg");
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                    if (getProcessor() instanceof StreamProcessor) {
                        InputStream sizeInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                TestUtil.getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension fullSize = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, new Form(), sourceFormat,
                                    fullSize, processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = TestUtil.getFixture(sourceFormat.getPreferredExtension());
                        Dimension fullSize = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, new Form(), sourceFormat, fullSize,
                                file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithRotationTransformation() throws Exception {
        String[] rotations = {"0", "15", "275", "!15"};
        for (String rotation : rotations) {
            Parameters params = new Parameters("bla", "10,10,50,50", "20,20",
                    rotation, "default", "jpg");
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
                            proc.process(params, new Form(), sourceFormat,
                                    size, processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
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
                        proc.process(params, new Form(), sourceFormat, size,
                                file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithQualityTransformation() throws Exception {
        String[] qualities = {"default", "color", "gray", "bitonal"};
        for (String quality : qualities) {
            Parameters params = new Parameters("bla", "10,10,50,50", "20,20",
                    "10", quality, "jpg");
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
                            proc.process(params, new Form(), sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
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
                        proc.process(params, new Form(), sourceFormat, size,
                                file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

    public void testProcessWithSupportedOutputFormats() throws Exception {
        Set<OutputFormat> outputFormats = getProcessor().
                getAvailableOutputFormats(SourceFormat.JPG);
        for (OutputFormat outputFormat : outputFormats) {
            Parameters params = new Parameters("bla", "10,10,50,50", "20,20",
                    "10", "default", outputFormat.getExtension());
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
                            proc.process(params, new Form(), sourceFormat, size,
                                    processInputStream, outputStream);
                            System.out.println(outputFormat.getExtension());
                            assertTrue(outputStream.toByteArray().length > 100);
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
                        proc.process(params, new Form(), sourceFormat, size,
                                file, outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

}
