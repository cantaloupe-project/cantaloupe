package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ValidationException;
import edu.illinois.library.cantaloupe.resolver.FileInputStreamStreamSource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

/**
 * For this to work, the GraphicsMagick binary must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends MagickProcessorTest {

    private static HashMap<Format, Set<Format>> supportedFormats;

    private GraphicsMagickProcessor instance;

    @Before
    public void setUp() {
        Configuration.getInstance().clearProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES);
        instance = newInstance();
    }

    protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException {
        if (supportedFormats == null) {
            final Set<Format> formats = new HashSet<>();
            final Set<Format> outputFormats = new HashSet<>();

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
                        formats.add(Format.JP2);
                        outputFormats.add(Format.JP2);
                    }
                    if (s.startsWith("JPEG  ") && s.endsWith(" yes")) {
                        formats.add(Format.JPG);
                        outputFormats.add(Format.JPG);
                    }
                    if (s.startsWith("PNG  ") && s.endsWith(" yes")) {
                        formats.add(Format.PNG);
                        outputFormats.add(Format.PNG);
                    }
                    if (s.startsWith("Ghostscript") && s.endsWith(" yes")) {
                        formats.add(Format.PDF);
                    }
                    if (s.startsWith("TIFF  ") && s.endsWith(" yes")) {
                        formats.add(Format.TIF);
                        outputFormats.add(Format.TIF);
                    }
                    if (s.startsWith("WebP  ") && s.endsWith(" yes")) {
                        formats.add(Format.WEBP);
                        outputFormats.add(Format.WEBP);
                    }
                }
            }

            // add formats that are definitely available
            // (http://www.graphicsmagick.org/formats.html)
            formats.add(Format.BMP);
            formats.add(Format.DCM);
            formats.add(Format.GIF);
            formats.add(Format.SGI);
            outputFormats.add(Format.GIF);

            supportedFormats = new HashMap<>();
            for (Format format : Format.values()) {
                supportedFormats.put(format, new HashSet<>());
            }
            for (Format format : formats) {
                supportedFormats.put(format, outputFormats);
            }
        }
        return supportedFormats;
    }

    @Override
    protected Format getSupported16BitSourceFormat() throws IOException {
        return Format.PNG;
    }

    @Override
    protected File getSupported16BitImage() throws IOException {
        return TestUtil.getImage("png-rgb-64x56x16.png");
    }

    @Override
    protected GraphicsMagickProcessor newInstance() {
        return new GraphicsMagickProcessor();
    }

    @Test
    public void getInitializationException() {
        Configuration.getInstance().setProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        GraphicsMagickProcessor.resetInitialization();
        instance = newInstance();
        assertNotNull(instance.getInitializationException());
    }

    @Override
    @Test
    public void processOf16BitImageWithEncodeOperationLimitingTo8Bits() {
        // >8-bit output is not currently available in this processor.
        // GM only has a -depth argument that forces all output to that depth.
        // In order to accomplish this, we would probably need readImageInfo()
        // to return a sample size in the Info and use that to determine
        // whether we need to call -depth.
    }

    @Override
    @Test
    public void processOf16BitImageWithEncodeOperationWithNoLimit() {
        // See above method.
    }

    @Test
    public void processWithPageOption() throws Exception {
        // Skip if GraphicsMagick does not support PDF.
        try {
            instance.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            return;
        }

        final File fixture = TestUtil.getImage("pdf-multipage.pdf");
        byte[] page1, page2;
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
        // Skip if GraphicsMagick does not support PDF.
        try {
            instance.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            return;
        }

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
