package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageWriterTest;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ImageReaderFactory;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class GIFImageWriterTest extends AbstractImageWriterTest {

    private BufferedImage bufferedImage;
    private Metadata metadata;
    private FileOutputStream outputStream;
    private PlanarImage planarImage;
    private File tempFile;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        // Read an image fixture into memory
        final Path fixture = TestUtil.getImage("gif-xmp.gif");

        GIFImageReader reader = new GIFImageReader();
        reader.setSource(fixture);
        metadata = reader.getMetadata(0);

        reader.setSource(fixture);
        bufferedImage = reader.read();

        reader.setSource(fixture);
        planarImage =  PlanarImage.wrapRenderedImage(reader.readRendered());

        // Create a temp file and output stream to write to
        tempFile = File.createTempFile("test", "tmp");
        outputStream = new FileOutputStream(tempFile);

        reader.dispose();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();

        outputStream.close();
        tempFile.delete();
    }

    @Override
    protected GIFImageWriter newInstance() {
        GIFImageWriter writer = new GIFImageWriter();
        Encode encode = new Encode(Format.GIF);
        Metadata metadata = new Metadata();
        encode.setMetadata(metadata);
        writer.setEncode(encode);
        return writer;
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((GIFImageWriter) instance).getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageWriter", impls[0]);
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((GIFImageWriter) instance).getUserPreferredIIOImplementation();
        String[] appImpls = ((GIFImageWriter) instance).getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((GIFImageWriter) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((GIFImageWriter) instance).getUserPreferredIIOImplementation());
    }

    /* write() */

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        instance.write(bufferedImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    @Disabled // this writer doesn't support XMP metadata.
    public void testWriteWithBufferedImageWritesXMPMetadata()  throws Exception {
        Encode encode = new Encode(Format.GIF);
        encode.setMetadata(metadata);
        instance.setEncode(encode);
        instance.write(bufferedImage, outputStream);
        checkForMetadata();
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        instance.write(planarImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    @Disabled // this writer doesn't support XMP metadata.
    public void testWriteWithPlanarImageWritesXMPMetadata() throws Exception {
        Encode encode = new Encode(Format.GIF);
        encode.setMetadata(metadata);
        instance.setEncode(encode);
        instance.write(planarImage, outputStream);
        checkForMetadata();
    }

    @Test
    public void testWriteWithSequence() throws Exception {
        Path image = TestUtil.getImage("gif-animated-looping.gif");
        edu.illinois.library.cantaloupe.processor.codec.ImageReader reader = null;
        try {
            reader = new ImageReaderFactory().newImageReader(Format.GIF, image);
            BufferedImageSequence sequence = reader.readSequence();

            try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
                instance.write(sequence, os);

                try (ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray())) {
                    reader.dispose();
                    reader = null;
                    try {
                        reader = new ImageReaderFactory().newImageReader(Format.GIF, is);
                        assertEquals(2, reader.getNumImages());
                    } finally {
                        if (reader != null) {
                            reader.dispose();
                        }
                    }
                }
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
        }
    }

    private void checkForICCProfile() throws Exception {
        // Read it back in
        final Iterator<javax.imageio.ImageReader> readers =
                ImageIO.getImageReadersByFormatName("GIF");
        final javax.imageio.ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(iis);
            // Check for the profile in its metadata
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());

            final NamedNodeMap attrs = tree.getChildNodes().item(3).
                    getChildNodes().item(0).getAttributes();
            assertEquals("ICCRGBG1", attrs.getNamedItem("applicationID").getNodeValue());
            assertEquals("012", attrs.getNamedItem("authenticationCode").getNodeValue());
        } finally {
            reader.dispose();
        }
    }

    private void checkForMetadata() throws Exception {
        final Iterator<javax.imageio.ImageReader> readers =
                ImageIO.getImageReadersByFormatName("GIF");
        final javax.imageio.ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            final NamedNodeMap attrs =
                    tree.getElementsByTagName("ApplicationExtensions").item(0).
                            getChildNodes().item(0).getAttributes();
            assertEquals("XMP Data", attrs.getNamedItem("applicationID").getNodeValue());
            assertEquals("XMP", attrs.getNamedItem("authenticationCode").getNodeValue());
        } finally {
            reader.dispose();
        }
    }

}
