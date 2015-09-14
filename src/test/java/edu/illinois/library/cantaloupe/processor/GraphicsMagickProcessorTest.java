package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import org.apache.commons.configuration.BaseConfiguration;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * For this to work, the GraphicsMagick binaries must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends ProcessorTest {

    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    GraphicsMagickProcessor instance;

    static {
        Application.setConfiguration(new BaseConfiguration());
    }

    private static HashMap<SourceFormat, Set<OutputFormat>>
    getAvailableOutputFormats() throws IOException {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<SourceFormat>();
            final Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

            // retrieve the output of the `gm version` command, which contains a
            // list of all optional formats
            Runtime runtime = Runtime.getRuntime();
            String[] commands = {"gm", "version"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;
            boolean read = false;
            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.contains("Feature Support")) {
                    read = true;
                } else if (s.contains("Host type:")) {
                    break;
                }
                if (read) {
                    if (s.startsWith("JPEG-2000  ") && s.endsWith(" yes")) {
                        sourceFormats.add(SourceFormat.JP2);
                        outputFormats.add(OutputFormat.JP2);
                    }
                    if (s.startsWith("JPEG  ") && s.endsWith(" yes")) {
                        sourceFormats.add(SourceFormat.JPG);
                        outputFormats.add(OutputFormat.JPG);
                    }
                    if (s.startsWith("PNG  ") && s.endsWith(" yes")) {
                        sourceFormats.add(SourceFormat.PNG);
                        outputFormats.add(OutputFormat.PNG);
                    }
                    if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                        outputFormats.add(OutputFormat.PDF);
                    }
                    if (s.startsWith("TIFF  ") && s.endsWith(" yes")) {
                        sourceFormats.add(SourceFormat.TIF);
                        outputFormats.add(OutputFormat.TIF);
                    }
                    if (s.startsWith("WebP  ") && s.endsWith(" yes")) {
                        sourceFormats.add(SourceFormat.WEBP);
                        outputFormats.add(OutputFormat.WEBP);
                    }
                }
            }

            // add formats that are definitely available
            // (http://www.graphicsmagick.org/formats.html)
            sourceFormats.add(SourceFormat.BMP);
            sourceFormats.add(SourceFormat.GIF);
            outputFormats.add(OutputFormat.GIF);

            supportedFormats = new HashMap<SourceFormat,Set<OutputFormat>>();
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                supportedFormats.put(sourceFormat, new HashSet<OutputFormat>());
            }
            for (SourceFormat sourceFormat : sourceFormats) {
                supportedFormats.put(sourceFormat, outputFormats);
            }
        }
        return supportedFormats;
    }

    public void setUp() {
        instance = new GraphicsMagickProcessor();
    }

    public void testGetAvailableOutputFormats() throws IOException {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> expectedFormats = getAvailableOutputFormats().
                    get(sourceFormat);
            assertEquals(expectedFormats,
                    instance.getAvailableOutputFormats(sourceFormat));
        }
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<OutputFormat>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    public void testGetSize() throws Exception {
        Dimension expectedSize = new Dimension(594, 522);
        Dimension actualSize = instance.getSize(
                new FileImageInputStream(getFixture("escher_lego.jpg")),
                SourceFormat.JPG);
        assertEquals(expectedSize, actualSize);
    }

    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = new HashSet<ProcessorFeature>();
        expectedFeatures.add(ProcessorFeature.MIRRORING);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.REGION_BY_PIXELS);
        expectedFeatures.add(ProcessorFeature.ROTATION_ARBITRARY);
        expectedFeatures.add(ProcessorFeature.ROTATION_BY_90S);
        expectedFeatures.add(ProcessorFeature.SIZE_ABOVE_FULL);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_FORCED_WIDTH_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_HEIGHT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_PERCENT);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH);
        expectedFeatures.add(ProcessorFeature.SIZE_BY_WIDTH_HEIGHT);
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

    public void testGetSupportedQualities() {
        Set<Quality> expectedQualities = new HashSet<Quality>();
        expectedQualities.add(Quality.BITONAL);
        expectedQualities.add(Quality.COLOR);
        expectedQualities.add(Quality.DEFAULT);
        expectedQualities.add(Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedQualities(SourceFormat.UNKNOWN));
    }

    public void testProcessWithSupportedSourceFormatsAndNoTransformation() throws Exception {
        Parameters params = new Parameters("bla", "full", "full", "0",
                "default", "jpg");
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                ImageInputStream inputStream = new FileImageInputStream(
                        getFixture(sourceFormat.getPreferredExtension()));
                OutputStream outputStream = new NullOutputStream();
                instance.process(params, sourceFormat, inputStream, outputStream);
            }
        }
    }

    public void testProcessWithUnsupportedSourceFormats() throws Exception {
        Parameters params = new Parameters("bla", "20,20,50,50", "pct:80",
                "15", "color", "jpg");
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            if (instance.getAvailableOutputFormats(sourceFormat).size() == 0) {
                try {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
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
                if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
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
                if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
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
                if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
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
                if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
                }
            }
        }
    }

    public void testProcessWithSupportedOutputFormats() throws Exception {
        Set<OutputFormat> outputFormats =
                instance.getAvailableOutputFormats(SourceFormat.JPG);
        for (OutputFormat outputFormat : outputFormats) {
            Parameters params = new Parameters("bla", "10,10,50,50", "20,20",
                    "10", "default", outputFormat.getExtension());
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                if (instance.getAvailableOutputFormats(sourceFormat).size() > 0) {
                    ImageInputStream inputStream = new FileImageInputStream(
                            getFixture(sourceFormat.getPreferredExtension()));
                    OutputStream outputStream = new NullOutputStream();
                    instance.process(params, sourceFormat, inputStream, outputStream);
                }
            }
        }
    }

    public void testProcessWithUnsupportedOutputFormats() throws Exception {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> supportedOutputFormats = instance.
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
                            instance.process(params, sourceFormat, inputStream,
                                    outputStream);
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
