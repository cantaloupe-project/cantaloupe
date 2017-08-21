package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class GIFImageWriterTest extends BaseTest {

    private BufferedImage bufferedImage;
    private Metadata metadata;
    private FileOutputStream outputStream;
    private PlanarImage planarImage;
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        // Disable metadata preservation (will be re-enabled in certain tests)
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, false);

        // Read an image fixture into memory
        final File fixture = TestUtil.getImage("gif-xmp.gif");
        metadata = new GIFImageReader(fixture).getMetadata(0);
        bufferedImage = new GIFImageReader(fixture).read();
        planarImage =  PlanarImage.wrapRenderedImage(
                new GIFImageReader(fixture).readRendered());

        // Create a temp file and output stream to write to
        tempFile = File.createTempFile("test", "tmp");
        outputStream = new FileOutputStream(tempFile);
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        outputStream.close();
        tempFile.delete();
    }

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        getWriter().write(bufferedImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    @Ignore // Disabled, as GIFMetadata.getXMP() is disabled.
    public void testWriteWithBufferedImageAndMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        getWriter().write(bufferedImage, outputStream);
        checkForMetadata();
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        getWriter().write(planarImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    @Ignore // Disabled, as GIFMetadata.getXMP() is disabled.
    public void testWriteWithPlanarImageAndMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        getWriter().write(planarImage, outputStream);
        checkForMetadata();
    }

    private void checkForIccProfile() throws Exception {
        // Read it back in
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("GIF");
        final ImageReader reader = readers.next();
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
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("GIF");
        final ImageReader reader = readers.next();
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

    private GIFImageWriter getWriter() throws IOException {
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            opList.add(new MetadataCopy());
        }
        return new GIFImageWriter(opList, metadata);
    }

}
