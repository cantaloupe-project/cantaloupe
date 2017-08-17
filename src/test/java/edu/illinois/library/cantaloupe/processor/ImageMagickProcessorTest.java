package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.operation.overlay.ImageOverlay;
import edu.illinois.library.cantaloupe.operation.overlay.Position;
import edu.illinois.library.cantaloupe.resolver.FileInputStreamStreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the ImageMagick binaries must be on the PATH.
 */
public class ImageMagickProcessorTest extends MagickProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    private ImageMagickProcessor instance;

    @Before
    public void setUp() {
        Configuration.getInstance().clearProperty(
                Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES);
        instance = newInstance();
    }

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
                    } else if (s.startsWith("SGI") && s.contains("  r")) {
                        sourceFormats.add(Format.SGI);
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

    @Test
    public void getIMOverlayGeometry() {
        ImageMagickProcessor instance = newInstance();

        // top left
        ImageOverlay overlay = new ImageOverlay(
                new File("/"), Position.TOP_LEFT, 2);
        assertEquals("+2+2", instance.getIMOverlayGeometry(overlay));
        // top center
        overlay = new ImageOverlay(new File("/"), Position.TOP_CENTER, 2);
        assertEquals("+0+2", instance.getIMOverlayGeometry(overlay));
        // top right
        overlay = new ImageOverlay(new File("/"), Position.TOP_RIGHT, 2);
        assertEquals("+2+2", instance.getIMOverlayGeometry(overlay));
        // left center
        overlay = new ImageOverlay(new File("/"), Position.LEFT_CENTER, 2);
        assertEquals("+2+0", instance.getIMOverlayGeometry(overlay));
        // center
        overlay = new ImageOverlay(new File("/"), Position.CENTER, 2);
        assertEquals("+0+0", instance.getIMOverlayGeometry(overlay));
        // right center
        overlay = new ImageOverlay(new File("/"), Position.RIGHT_CENTER, 2);
        assertEquals("+2+0", instance.getIMOverlayGeometry(overlay));
        // bottom left
        overlay = new ImageOverlay(new File("/"), Position.BOTTOM_LEFT, 2);
        assertEquals("+2+2", instance.getIMOverlayGeometry(overlay));
        // bottom center
        overlay = new ImageOverlay(new File("/"), Position.BOTTOM_CENTER, 2);
        assertEquals("+0+2", instance.getIMOverlayGeometry(overlay));
        // bottom right
        overlay = new ImageOverlay(new File("/"), Position.BOTTOM_RIGHT, 2);
        assertEquals("+2+2", instance.getIMOverlayGeometry(overlay));
    }

    @Test
    public void getIMOverlayGravity() {
        ImageMagickProcessor instance = newInstance();

        assertEquals("northwest",
                instance.getIMOverlayGravity(Position.TOP_LEFT));
        assertEquals("north",
                instance.getIMOverlayGravity(Position.TOP_CENTER));
        assertEquals("northeast",
                instance.getIMOverlayGravity(Position.TOP_RIGHT));
        assertEquals("west",
                instance.getIMOverlayGravity(Position.LEFT_CENTER));
        assertEquals("center",
                instance.getIMOverlayGravity(Position.CENTER));
        assertEquals("east",
                instance.getIMOverlayGravity(Position.RIGHT_CENTER));
        assertEquals("southwest",
                instance.getIMOverlayGravity(Position.BOTTOM_LEFT));
        assertEquals("south",
                instance.getIMOverlayGravity(Position.BOTTOM_CENTER));
        assertEquals("southeast",
                instance.getIMOverlayGravity(Position.BOTTOM_RIGHT));
    }

    @Test
    public void getInitializationException() {
        Configuration.getInstance().setProperty(
                Key.IMAGEMAGICKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        ImageMagickProcessor.resetInitialization();
        instance = newInstance();
        assertNotNull(instance.getInitializationException());
    }

    @Test
    public void getOverlayTempFile() throws Exception {
        URL url = new URL("file://" + TestUtil.getImage("jpg").getAbsolutePath());
        ImageOverlay overlay = new ImageOverlay(url, Position.TOP_LEFT, 2);

        ImageMagickProcessor instance = newInstance();
        File overlayFile = instance.getOverlayTempFile(overlay);
        assertTrue(overlayFile.getAbsolutePath().contains(ImageMagickProcessor.OVERLAY_TEMP_FILE_PREFIX));
        assertTrue(overlayFile.exists());
    }

    @Test
    public void getWarningsWithNoWarnings() {
        boolean initialValue = ImageMagickProcessor.isUsingVersion7();
        try {
            ImageMagickProcessor.setUsingVersion7(true);
            assertEquals(0, instance.getWarnings().size());
        } finally {
            ImageMagickProcessor.setUsingVersion7(initialValue);
        }
    }

    @Test
    public void getWarningsWithWarnings() {
        boolean initialValue = ImageMagickProcessor.isUsingVersion7();
        try {
            ImageMagickProcessor.setUsingVersion7(false);
            assertEquals(1, instance.getWarnings().size());
        } finally {
            ImageMagickProcessor.setUsingVersion7(initialValue);
        }
    }

    @Test
    public void processWithPageOption() throws Exception {
        final File fixture = TestUtil.getImage("pdf-multipage.pdf");
        byte[] page1, page2;
        instance.setSourceFormat(Format.PDF);
        Info imageInfo;

        // page option missing
        instance.setStreamSource(new FileInputStreamStreamSource(fixture));
        imageInfo = instance.readImageInfo();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = TestUtil.newOperationList();
        instance.process(ops, imageInfo, outputStream);
        page1 = outputStream.toByteArray();

        // page option present
        instance.setStreamSource(new FileInputStreamStreamSource(fixture));

        ops = TestUtil.newOperationList();
        ops.getOptions().put("page", "2");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    public void validate() throws Exception {
        instance.setSourceFormat(Format.PDF);
        instance.setStreamSource(new FileInputStreamStreamSource(
                TestUtil.getImage("pdf.pdf")));

        OperationList ops = TestUtil.newOperationList();
        Dimension fullSize = new Dimension(1000, 1000);
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "1");
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "0");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }

        ops.getOptions().put("page", "-1");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }
    }

}
