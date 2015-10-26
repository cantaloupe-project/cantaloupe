package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import org.apache.commons.configuration.BaseConfiguration;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    protected File getFixture(String filename) throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path testPath = Paths.get(cwd, "src", "test", "resources");
        return new File(testPath + File.separator + filename);
    }

    protected abstract Processor getProcessor();

    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(594, 522);
        if (getProcessor() instanceof StreamProcessor) {
            StreamProcessor proc = (StreamProcessor) getProcessor();
            try (InputStream inputStream = new FileInputStream(
                    getFixture("escher_lego.jpg"))) {
                Dimension actualSize = proc.getSize(inputStream, SourceFormat.JPG);
                assertEquals(expectedSize, actualSize);
            }
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = proc.getSize(getFixture("escher_lego.jpg"),
                    SourceFormat.JPG);
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
                            getFixture(sourceFormat.getPreferredExtension()));
                    InputStream sizeInputStream = new FileInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, processInputStream,
                                outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    } finally {
                        processInputStream.close();
                        sizeInputStream.close();
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    FileProcessor proc = (FileProcessor) getProcessor();
                    File file = getFixture(sourceFormat.getPreferredExtension());
                    Dimension size = proc.getSize(file, sourceFormat);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    proc.process(params, sourceFormat, size, file, outputStream);
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
                            getFixture(sourceFormat.getPreferredExtension()));
                    InputStream processInputStream = new FileInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        Dimension size = proc.getSize(sizeInputStream, sourceFormat);
                        proc.process(params, sourceFormat, size,
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
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file,
                                new NullOutputStream());
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
                                getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                                getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension fullSize = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, fullSize,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension fullSize = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, fullSize, file, outputStream);
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
                                getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                                getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
                                    processInputStream, outputStream);
                            assertTrue(outputStream.toByteArray().length > 100);
                        } finally {
                            sizeInputStream.close();
                            processInputStream.close();
                        }
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                                getFixture(sourceFormat.getPreferredExtension()));
                        InputStream processInputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        try {
                            StreamProcessor proc = (StreamProcessor) getProcessor();
                            Dimension size = proc.getSize(sizeInputStream,
                                    sourceFormat);
                            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                            proc.process(params, sourceFormat, size,
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
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                        proc.process(params, sourceFormat, size, file,
                                outputStream);
                        assertTrue(outputStream.toByteArray().length > 100);
                    }
                }
            }
        }
    }

}
