package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the GraphicsMagick binary must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends AbstractMagickProcessorTest {

    private static Map<Format, Set<Format>> supportedFormats;

    private GraphicsMagickProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES);
        GraphicsMagickProcessor.resetInitialization();

        instance = newInstance();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        instance.close();
    }

    Map<Format, Set<Format>> getAvailableOutputFormats() throws IOException {
        if (supportedFormats == null) {
            final Set<Format> sourceFormats = EnumSet.noneOf(Format.class);
            final Set<Format> outputFormats = EnumSet.noneOf(Format.class);

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
                        sourceFormats.add(Format.JP2);
                        outputFormats.add(Format.JP2);
                    }
                    if (s.startsWith("JPEG  ") && s.endsWith(" yes")) {
                        sourceFormats.add(Format.JPG);
                        outputFormats.add(Format.JPG);
                    }
                    if (s.startsWith("PNG  ") && s.endsWith(" yes")) {
                        sourceFormats.add(Format.PNG);
                        outputFormats.add(Format.PNG);
                    }
                    if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                        sourceFormats.add(Format.PDF);
                    }
                    if (s.startsWith("TIFF  ") && s.endsWith(" yes")) {
                        sourceFormats.add(Format.TIF);
                        outputFormats.add(Format.TIF);
                    }
                    if (s.startsWith("WebP  ") && s.endsWith(" yes")) {
                        sourceFormats.add(Format.WEBP);
                        outputFormats.add(Format.WEBP);
                    }
                }
            }

            // add formats that are definitely available
            // (http://www.graphicsmagick.org/formats.html)
            sourceFormats.add(Format.BMP);
            sourceFormats.add(Format.DCM);
            sourceFormats.add(Format.GIF);
            outputFormats.add(Format.GIF);

            supportedFormats = new HashMap<>();
            for (Format format : Format.values()) {
                supportedFormats.put(format, EnumSet.noneOf(Format.class));
            }
            for (Format format : sourceFormats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    protected GraphicsMagickProcessor newInstance() {
        return new GraphicsMagickProcessor();
    }

    @Test
    public void testGetInitializationErrorWithNoError() {
        assertNull(instance.getInitializationError());
    }

    @Test
    public void testGetInitializationErrorWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        GraphicsMagickProcessor.resetInitialization();

        assertNotNull(instance.getInitializationError());
    }

    /* getWarnings() */

    @Test
    public void testGetWarnings() {
        assertEquals(0, instance.getWarnings().size());
    }

    /* process() */

    @Override
    @Ignore
    @Test
    public void testProcessWithAllSupportedOutputFormats() {
        // TODO: The parent fails on a lot of fixtures.
    }

}
