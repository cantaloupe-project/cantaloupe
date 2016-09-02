package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
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
public class ImageMagickProcessorTest extends Im4JavaProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

            String cmdPath = "identify";
            // retrieve the output of the `identify -list format` command,
            // which contains a list of all supported formats
            Runtime runtime = Runtime.getRuntime();
            Configuration config = Configuration.getInstance();
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
                if (s.startsWith("JP2")) {
                    formats.add(Format.JP2);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.JP2);
                    }
                }
                if (s.startsWith("JPEG")) {
                    formats.add(Format.JPG);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.JPG);
                    }
                }
                if (s.startsWith("PNG")) {
                    formats.add(Format.PNG);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.PNG);
                    }
                }
                if (s.startsWith("PDF") && s.contains(" rw")) {
                    outputFormats.add(Format.PDF);
                }
                if (s.startsWith("TIFF")) {
                    formats.add(Format.TIF);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.TIF);
                    }
                }
                if (s.startsWith("WEBP")) {
                    formats.add(Format.WEBP);
                    if (s.contains(" rw")) {
                        outputFormats.add(Format.WEBP);
                    }
                }
            }

            supportedFormats = new HashMap<>();
            for (Format format : Format.values()) {
                supportedFormats.put(format, new HashSet<Format>());
            }
            for (Format format : formats) {
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
        Configuration config = Configuration.getInstance();
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
        assertEquals(4, green);
        assertEquals(254, blue);
    }

}
