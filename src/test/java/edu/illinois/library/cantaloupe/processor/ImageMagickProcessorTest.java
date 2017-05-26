package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ImageMagick binaries must be on the PATH.
 */
public class ImageMagickProcessorTest extends MagickProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<Format> sourceFormats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            String cmdPath = "identify";
            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            Configuration config = Configuration.getInstance();
            config.clear();
            String pathPrefix =
                    config.getString(Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES);
            if (pathPrefix != null) {
                cmdPath = pathPrefix + File.separator + cmdPath;
            }

            String[] commands = {cmdPath, "-list", "format"};
            Process proc = runtime.exec(commands);
            InputStreamReader reader = new InputStreamReader(proc.getInputStream());
            try (BufferedReader buffReader = new BufferedReader(reader)) {
                String s;
                while ((s = buffReader.readLine()) != null) {
                    s = s.trim();
                    if (s.startsWith("BMP")) {
                        sourceFormats.add(Format.BMP);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.BMP);
                        }
                    } else if (s.startsWith("DCM")) {
                        sourceFormats.add(Format.DCM);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.DCM);
                        }
                    } else if (s.startsWith("GIF")) {
                        sourceFormats.add(Format.GIF);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.GIF);
                        }
                    } else if (s.startsWith("JP2")) {
                        sourceFormats.add(Format.JP2);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.JP2);
                        }
                    } else if (s.startsWith("JPEG")) {
                        sourceFormats.add(Format.JPG);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.JPG);
                        }
                    } else if (s.startsWith("PNG")) {
                        sourceFormats.add(Format.PNG);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.PNG);
                        }
                    } else if (s.startsWith("PDF") && s.contains(" rw")) {
                        sourceFormats.add(Format.PDF);
                    } else if (s.startsWith("TIFF")) {
                        sourceFormats.add(Format.TIF);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.TIF);
                        }
                    } else if (s.startsWith("WEBP")) {
                        sourceFormats.add(Format.WEBP);
                        if (s.contains(" rw")) {
                            outputFormats.add(Format.WEBP);
                        }
                    }
                }
            }

            supportedFormats = new HashMap<>();
            for (Format format : Format.values()) {
                supportedFormats.put(format, new HashSet<>());
            }
            for (Format format : sourceFormats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    protected ImageMagickProcessor newInstance() {
        return new ImageMagickProcessor();
    }

}
