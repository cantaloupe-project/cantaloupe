package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.resolver.StreamSource;
import edu.illinois.library.cantaloupe.resource.iiif.ProcessorFeature;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ImageMagick binaries must be on the PATH.
 */
public class ImageMagickProcessorTest extends ProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    ImageMagickProcessor instance = new ImageMagickProcessor();

    /**
     * @return Map of available output formats for all known source formats,
     * based on information reported by <code>identify -list format</code>.
     */
    private static HashMap<Format, Set<Format>> getFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

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

    protected Processor getProcessor() {
        return instance;
    }

    @Test
    public void testGetAvailableOutputFormats() throws Exception {
        for (Format format : Format.values()) {
            try {
                instance.setSourceFormat(format);
                Set<Format> expectedFormats = getFormats().get(format);
                assertEquals(expectedFormats, instance.getAvailableOutputFormats());
            } catch (UnsupportedSourceFormatException e) {
                // continue
            }
        }
    }

    @Test
    public void testGetSupportedFeatures() throws Exception {
        instance.setSourceFormat(getAnySupportedSourceFormat(instance));
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
        assertEquals(expectedFeatures, instance.getSupportedFeatures());
    }

    @Test
    public void testGetTileSizes() throws Exception {
        // untiled image
        instance.setStreamSource(new TestStreamSource(TestUtil.getImage("jpg")));
        instance.setSourceFormat(Format.JPG);
        Dimension expectedSize = new Dimension(64, 56);
        List<Dimension> tileSizes = instance.getTileSizes();
        assertEquals(1, tileSizes.size());
        assertEquals(expectedSize, tileSizes.get(0));

        try {
            // tiled image (this processor doesn't recognize tiles)
            instance.setStreamSource(new TestStreamSource(
                    TestUtil.getImage("tif-rgb-monores-64x56x8-tiled-uncompressed.tif")));
            instance.setSourceFormat(Format.TIF);
            tileSizes = instance.getTileSizes();
            assertEquals(expectedSize, tileSizes.get(0));
        } catch (UnsupportedSourceFormatException e) {
            // oh well
        }
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndNonTransparentOutputFormat() throws Exception {
        Configuration config = new BaseConfiguration();
        config.setProperty(ImageMagickProcessor.BACKGROUND_COLOR_CONFIG_KEY, "blue");
        Application.setConfiguration(config);

        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("bla"));
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.setOutputFormat(Format.JPG);

        Dimension fullSize = new Dimension(64, 58);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        instance.setSourceFormat(Format.JPG);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamSource(streamSource);
        instance.process(ops, fullSize, outputStream);

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
        assertEquals(1, red);
        assertEquals(0, green);
        assertEquals(254, blue);
    }

    @Test
    public void testProcessWithRotationAndCustomBackgroundColorAndTransparentOutputFormat() throws Exception {
        Configuration config = new BaseConfiguration();
        config.setProperty(ImageMagickProcessor.BACKGROUND_COLOR_CONFIG_KEY, "blue");
        Application.setConfiguration(config);

        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("bla"));
        Rotate rotation = new Rotate(15);
        ops.add(rotation);
        ops.setOutputFormat(Format.PNG);

        Dimension fullSize = new Dimension(64, 58);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        instance.setSourceFormat(Format.JPG);
        StreamSource streamSource = new TestStreamSource(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        instance.setStreamSource(streamSource);
        instance.process(ops, fullSize, outputStream);

        ByteArrayInputStream inputStream =
                new ByteArrayInputStream(outputStream.toByteArray());
        final BufferedImage rotatedImage = ImageIO.read(inputStream);

        int pixel = rotatedImage.getRGB(0, 0);
        int alpha = (pixel >> 24) & 0xff;
        int red = (pixel >> 16) & 0xff;
        int green = (pixel >> 8) & 0xff;
        int blue = (pixel) & 0xff;
        assertEquals(0, alpha);
        assertEquals(0, red);
        assertEquals(0, green);
        assertEquals(0, blue);
    }

}
