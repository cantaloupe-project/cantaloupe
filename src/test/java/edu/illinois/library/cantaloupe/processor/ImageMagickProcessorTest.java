package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
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
            Configuration config = ConfigurationFactory.getInstance();
            config.clear();
            String pathPrefix = config.getString("ImageMagickProcessor.path_to_binaries");
            if (pathPrefix != null) {
                cmdPath = pathPrefix + File.separator + cmdPath;
            }

            String[] commands = {cmdPath, "-list", "format"};
            Process proc = runtime.exec(commands);
            BufferedReader stdInput = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
            String s;

            while ((s = stdInput.readLine()) != null) {
                s = s.trim();
                if (s.startsWith("BMP")) {
                    sourceFormats.add(Format.BMP);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.BMP);
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
                    outputFormats.add(Format.PDF);
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

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndNonTransparentOutputFormat()
            throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        config.setProperty(ImageMagickProcessor.BACKGROUND_COLOR_CONFIG_KEY, "blue");

        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("bla"));
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.setOutputFormat(Format.JPG);

        ImageInfo imageInfo = new ImageInfo(64, 58);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final StreamProcessor instance = newInstance();
        instance.setSourceFormat(Format.JPG);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamSource(streamSource);
        instance.process(ops, imageInfo, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        int pixel = rotatedImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        // "ImageMagick blue"
        assertEquals(255, alpha);
        assertTrue(red < 5);
        assertTrue(green < 5);
        assertTrue(blue > 250);
    }

}
