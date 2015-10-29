package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;
import org.apache.commons.configuration.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * For this to work, the ImageMagick binaries must be on the PATH.
 */
public class ImageMagickProcessorTest extends ProcessorTest {

    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    ImageMagickProcessor instance = new ImageMagickProcessor();

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<SourceFormat, Set<OutputFormat>> getFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<>();
            final Set<OutputFormat> outputFormats = new HashSet<>();

            String cmdPath = "identify";
            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            Configuration config = Application.getConfiguration();
            if (config != null) {
                String pathPrefix = config.getString("ImageMagickProcessor.path_to_binaries");
                if (pathPrefix != null) {
                    cmdPath = pathPrefix + File.separator + cmdPath;
                }
            }
            String[] commands = {cmdPath, "-list", "format"};
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

            supportedFormats = new HashMap<>();
            for (SourceFormat sourceFormat : SourceFormat.values()) {
                supportedFormats.put(sourceFormat, new HashSet<OutputFormat>());
            }
            for (SourceFormat sourceFormat : sourceFormats) {
                supportedFormats.put(sourceFormat, outputFormats);
            }
        }
        return supportedFormats;
    }

    protected Processor getProcessor() {
        return instance;
    }

    public void testGetAvailableOutputFormats() throws IOException {
        for (SourceFormat sourceFormat : SourceFormat.values()) {
            Set<OutputFormat> expectedFormats = getFormats().get(sourceFormat);
            assertEquals(expectedFormats,
                    instance.getAvailableOutputFormats(sourceFormat));
        }
    }

    public void testGetAvailableOutputFormatsForUnsupportedSourceFormat() {
        Set<OutputFormat> expectedFormats = new HashSet<>();
        assertEquals(expectedFormats,
                instance.getAvailableOutputFormats(SourceFormat.UNKNOWN));
    }

    public void testGetSupportedFeatures() {
        Set<ProcessorFeature> expectedFeatures = new HashSet<>();
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
                instance.getSupportedFeatures(getAnySupportedSourceFormat(instance)));

        expectedFeatures = new HashSet<>();
        assertEquals(expectedFeatures,
                instance.getSupportedFeatures(SourceFormat.UNKNOWN));
    }

    public void testGetSupportedQualities() {
        Set<Quality> expectedQualities = new HashSet<>();
        expectedQualities.add(Quality.BITONAL);
        expectedQualities.add(Quality.COLOR);
        expectedQualities.add(Quality.DEFAULT);
        expectedQualities.add(Quality.GRAY);
        assertEquals(expectedQualities,
                instance.getSupportedQualities(getAnySupportedSourceFormat(instance)));

        expectedQualities = new HashSet<>();
        assertEquals(expectedQualities,
                instance.getSupportedQualities(SourceFormat.UNKNOWN));
    }

}
