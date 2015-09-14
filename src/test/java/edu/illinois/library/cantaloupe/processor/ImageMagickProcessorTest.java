package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.request.OutputFormat;
import edu.illinois.library.cantaloupe.request.Parameters;
import edu.illinois.library.cantaloupe.request.Quality;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class ImageMagickProcessorTest extends ProcessorTest {

    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    ImageMagickProcessor instance;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>> getFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<SourceFormat>();
            final Set<OutputFormat> outputFormats = new HashSet<OutputFormat>();

            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            String cmdPath = "";
            Configuration config = Application.getConfiguration();
            if (config != null) {
                cmdPath = config.getString("ImageMagickProcessor.path_to_binaries", "");
            }
            String[] commands = {cmdPath + File.separator + "identify",
                    "-list", "format"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;

            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("JP2")) {
                    sourceFormats.add(SourceFormat.JP2);
                    if (s.contains(" rw")) {
                        outputFormats.add(OutputFormat.JP2);
                    }
                }
                if (s.startsWith("JPEG")) {
                    sourceFormats.add(SourceFormat.JPG);
                    if (s.contains(" rw")) {
                        outputFormats.add(OutputFormat.JPG);
                    }
                }
                if (s.startsWith("PNG")) {
                    sourceFormats.add(SourceFormat.PNG);
                    if (s.contains(" rw")) {
                        outputFormats.add(OutputFormat.PNG);
                    }
                }
                if (s.startsWith("PDF") && s.contains(" rw")) {
                    outputFormats.add(OutputFormat.PDF);
                }
                if (s.startsWith("TIFF")) {
                    sourceFormats.add(SourceFormat.TIF);
                    if (s.contains(" rw")) {
                        outputFormats.add(OutputFormat.TIF);
                    }
                }
                if (s.startsWith("WEBP")) {
                    sourceFormats.add(SourceFormat.WEBP);
                    if (s.contains(" rw")) {
                        outputFormats.add(OutputFormat.WEBP);
                    }
                }

            }

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
        BaseConfiguration config = new BaseConfiguration();
        config.setProperty("ImageMagickProcessor.path_to_binaries", "/usr/local/bin"); // TODO: externalize this
        Application.setConfiguration(config);

        instance = new ImageMagickProcessor();
    }

    public void testGetAvailableOutputFormats() throws IOException {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> expectedFormats = getFormats().get(sourceFormat);
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
