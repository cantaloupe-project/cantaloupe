package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.resolver.InputStreamStreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
    public void testProcessWithPageOption() throws Exception {
        final File fixture = TestUtil.getImage("pdf-multipage.pdf");
        byte[] page1, page2;
        instance.setSourceFormat(Format.PDF);
        Info imageInfo;

        // page option missing
        try (FileInputStream fis = new FileInputStream(fixture)) {
            instance.setStreamSource(new InputStreamStreamSource(fis));
            imageInfo = instance.readImageInfo();

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            OperationList ops = TestUtil.newOperationList();
            instance.process(ops, imageInfo, outputStream);
            page1 = outputStream.toByteArray();
        }

        // page option present
        try (FileInputStream fis = new FileInputStream(fixture)) {
            instance.setStreamSource(new InputStreamStreamSource(fis));

            OperationList ops = TestUtil.newOperationList();
            ops.getOptions().put("page", "2");
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            instance.process(ops, imageInfo, outputStream);
            page2 = outputStream.toByteArray();
        }

        assertFalse(Arrays.equals(page1, page2));
    }

    @Test
    public void testValidate() throws Exception {
        instance.setSourceFormat(Format.PDF);
        instance.setStreamSource(new InputStreamStreamSource(
                new FileInputStream(TestUtil.getImage("pdf.pdf"))));

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
