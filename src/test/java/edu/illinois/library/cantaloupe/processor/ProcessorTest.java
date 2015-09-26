package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import junit.framework.TestCase;
import org.apache.commons.configuration.BaseConfiguration;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.io.File;
import java.io.IOException;
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
        Dimension actualSize = getProcessor().getSize(
                new FileImageInputStream(getFixture("escher_lego.jpg")),
                SourceFormat.JPG);
        assertEquals(expectedSize, actualSize);
    }

    public void testProcessWithSupportedSourceFormatsAndNoTransformation() throws Exception {
        Parameters params = new Parameters("bla", "full", "full", "0",
                "default", "jpg");
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() > 0) {
                ImageInputStream inputStream = new FileImageInputStream(
                        getFixture(sourceFormat.getPreferredExtension()));
                OutputStream outputStream = new NullOutputStream();
                getProcessor().process(params, sourceFormat, inputStream, outputStream);
            }
        }
    }

    public void testProcessWithUnsupportedSourceFormats() throws Exception {
        Parameters params = new Parameters("bla", "20,20,50,50", "pct:80",
                "15", "color", "jpg");
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (getProcessor().getAvailableOutputFormats(sourceFormat).size() == 0) {
                try {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
                    fail("Expected exception");
                } catch (UnsupportedSourceFormatException e) {
                    assertEquals(e.getMessage(), "Unsupported source format");
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
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
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
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
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
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
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
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
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
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    getProcessor().process(params, sourceFormat, inputStream,
                            outputStream);
                }
            }
        }
    }

    public void testProcessWithUnsupportedOutputFormats() throws Exception {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> supportedOutputFormats = getProcessor().
                    getAvailableOutputFormats(sourceFormat);
            if (supportedOutputFormats.size() > 0) {
                for (OutputFormat outputFormat : OutputFormat.values()) {
                    if (!supportedOutputFormats.contains(outputFormat)) {
                        Parameters params = new Parameters("bla", "10,10,50,50", "20,20",
                                "10", "default", outputFormat.getExtension());
                        ImageInputStream inputStream = new FileImageInputStream(
                                getFixture(sourceFormat.getPreferredExtension()));
                        OutputStream outputStream = new NullOutputStream();
                        try {
                            getProcessor().process(params, sourceFormat,
                                    inputStream, outputStream);
                            fail("Expected exception");
                        } catch (UnsupportedOutputFormatException e) {
                            assertEquals("Unsupported output format",
                                    e.getMessage());
                        }
                    }
                }
            }
        }
    }

}
