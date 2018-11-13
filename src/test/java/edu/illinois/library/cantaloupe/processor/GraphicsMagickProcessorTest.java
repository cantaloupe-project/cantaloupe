package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.source.PathStreamFactory;
import edu.illinois.library.cantaloupe.source.StreamFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * For this to work, the GraphicsMagick binary must be on the PATH.
 */
public class GraphicsMagickProcessorTest extends MagickProcessorTest {

    private static final double DELTA = 0.00000001;

    private static HashMap<Format, Set<Format>> supportedFormats;

    private GraphicsMagickProcessor instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        Configuration.getInstance().clearProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES);
        GraphicsMagickProcessor.resetInitialization();

        instance = newInstance();
    }

    protected HashMap<Format, Set<Format>> getAvailableOutputFormats()
            throws IOException {
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
    public void testGetInitializationExceptionWithNoException() {
        assertNull(instance.getInitializationException());
    }

    @Test
    public void testGetInitializationExceptionWithMissingBinaries() {
        Configuration.getInstance().setProperty(
                Key.GRAPHICSMAGICKPROCESSOR_PATH_TO_BINARIES,
                "/bogus/bogus/bogus");
        GraphicsMagickProcessor.resetInitialization();

        assertNotNull(instance.getInitializationException());
    }

    /* getWarnings() */

    @Test
    public void testGetWarnings() {
        assertEquals(0, instance.getWarnings().size());
    }

    /* process() */

    @Test
    public void testProcessWithPageOption() throws Exception {
        // Skip if GraphicsMagick does not support PDF.
        try {
            instance.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            return;
        }

        final Path fixture = TestUtil.getImage("pdf-multipage.pdf");
        byte[] page1, page2;
        Info imageInfo;

        // page option missing
        instance.setStreamFactory(new PathStreamFactory(fixture));
        imageInfo = instance.readInfo();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OperationList ops = new OperationList();
        instance.process(ops, imageInfo, outputStream);
        page1 = outputStream.toByteArray();

        // page option present
        instance.setStreamFactory(new PathStreamFactory(fixture));

        ops = new OperationList();
        ops.getOptions().put("page", "2");
        outputStream = new ByteArrayOutputStream();
        instance.process(ops, imageInfo, outputStream);
        page2 = outputStream.toByteArray();

        assertFalse(Arrays.equals(page1, page2));
    }

    @Override
    @Ignore
    @Test
    public void testProcessWithAllSupportedOutputFormats() {
        // TODO: The parent fails on a lot of fixtures.
    }

    /* readInfo() */

    /**
     * Override that doesn't check {@link Info#getNumResolutions()}.
     */
    @Override
    @Test
    public void testReadImageInfoOnAllFixtures() throws Exception {
        final Processor proc = newInstance();

        for (Format format : Format.values()) {
            try {
                // The processor will throw an exception if it doesn't support
                // this format, which is fine. No processor supports all
                // formats.
                proc.setSourceFormat(format);

                for (Path fixture : TestUtil.getImageFixtures(format)) {
                    // TODO: address this
                    if (fixture.getFileName().toString().equals("jpg-rgb-594x522x8-baseline.jpg")) {
                        continue;
                    }

                    StreamProcessor sproc = (StreamProcessor) proc;
                    StreamFactory streamFactory =
                            new PathStreamFactory(fixture);
                    sproc.setStreamFactory(streamFactory);

                    try {
                        // We don't know the dimensions of the source image and
                        // we can't get them because that would require using
                        // the method we are now testing, so the best we can do
                        // is to assert that they are nonzero.
                        final Info actualInfo = proc.readInfo();
                        assertEquals(format, actualInfo.getSourceFormat());
                        assertTrue(actualInfo.getSize().width() > DELTA);
                        assertTrue(actualInfo.getSize().height() > DELTA);

                        assertEquals(-1, actualInfo.getNumResolutions());
                    } catch (Exception e) {
                        System.err.println(format + " : " + fixture);
                        throw e;
                    }
                }
            } catch (UnsupportedSourceFormatException e) {
                // OK, continue
            }
        }
    }

    /* validate() */

    @Test
    public void testValidate() throws Exception {
        // Skip if GraphicsMagick does not support PDF.
        try {
            instance.setSourceFormat(Format.PDF);
        } catch (UnsupportedSourceFormatException e) {
            return;
        }

        instance.setStreamFactory(new PathStreamFactory(
                TestUtil.getImage("pdf.pdf")));

        OperationList ops = new OperationList();
        Dimension fullSize = new Dimension(1000, 1000);
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "1");
        instance.validate(ops, fullSize);

        ops.getOptions().put("page", "0");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }

        ops.getOptions().put("page", "-1");
        try {
            instance.validate(ops, fullSize);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

}
