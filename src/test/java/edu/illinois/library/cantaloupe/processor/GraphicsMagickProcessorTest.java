package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.SourceFormat;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.image.Quality;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * For this to work, the GraphicsMagick binaries must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends ProcessorTest {

    private static HashMap<SourceFormat, Set<OutputFormat>> supportedFormats;

    GraphicsMagickProcessor instance = new GraphicsMagickProcessor();

    private static HashMap<SourceFormat, Set<OutputFormat>>
    getAvailableOutputFormats() throws IOException {
        if (supportedFormats == null) {
            final Set<SourceFormat> sourceFormats = new HashSet<>();
            final Set<OutputFormat> outputFormats = new HashSet<>();

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
            Set<OutputFormat> expectedFormats = getAvailableOutputFormats().
                    get(sourceFormat);
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
