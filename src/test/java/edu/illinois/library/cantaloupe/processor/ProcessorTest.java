package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

/**
 * Contains base tests common to all Processors.
 */
public abstract class ProcessorTest extends TestCase {

    static {
        Application.setConfiguration(new BaseConfiguration());
        System.setProperty("com.sun.media.jai.disableMediaLib", "true");
    }

    protected OutputStream outputStream = new NullOutputStream();

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
            Dimension actualSize = proc.getSize(
                    new FileInputStream(getFixture("escher_lego.jpg")),
                    SourceFormat.JPG);
            assertEquals(expectedSize, actualSize);
        }
        if (getProcessor() instanceof FileProcessor) {
            FileProcessor proc = (FileProcessor) getProcessor();
            Dimension actualSize = proc.getSize(getFixture("escher_lego.jp2"),
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
                    StreamProcessor proc = (StreamProcessor) getProcessor();
                    InputStream inputStream = new FileInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    Dimension size = proc.getSize(inputStream, sourceFormat);
                    inputStream = new FileInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    proc.process(params, sourceFormat, size, inputStream,
                            outputStream);
                }
                if (getProcessor() instanceof FileProcessor) {
                    FileProcessor proc = (FileProcessor) getProcessor();
                    File file = getFixture(sourceFormat.getPreferredExtension());
                    Dimension size = proc.getSize(file, sourceFormat);
                    proc.process(params, sourceFormat, size, file, outputStream);
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
                    try {
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension size = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, size, inputStream,
                                outputStream);
                        fail("Expected exception");
                    } catch (ProcessorException e) {
                        e.printStackTrace();
                        assertEquals("Unsupported source format: " +
                                        sourceFormat.getPreferredExtension(),
                                e.getMessage());
                    }
                }
                if (getProcessor() instanceof FileProcessor) {
                    try {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension size = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, size, inputStream,
                                outputStream);
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension fullSize = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, fullSize, inputStream,
                                outputStream);
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension fullSize = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, fullSize, file, outputStream);
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
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension size = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, size, inputStream,
                                outputStream);
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension size = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, size ,inputStream,
                                outputStream);
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file, outputStream);
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
                        StreamProcessor proc = (StreamProcessor) getProcessor();
                        InputStream inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        Dimension size = proc.getSize(inputStream, sourceFormat);
                        inputStream = new FileInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        proc.process(params, sourceFormat, size, inputStream,
                                outputStream);
                    }
                    if (getProcessor() instanceof FileProcessor) {
                        FileProcessor proc = (FileProcessor) getProcessor();
                        File file = getFixture(sourceFormat.getPreferredExtension());
                        Dimension size = proc.getSize(file, sourceFormat);
                        proc.process(params, sourceFormat, size, file,
                                outputStream);
                    }
                }
            }
        }
    }

}
