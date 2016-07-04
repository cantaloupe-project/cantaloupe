package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetadataCopy;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.icc.IccProfile;
import edu.illinois.library.cantaloupe.image.icc.IccProfileService;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
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

public class ImageIoGifImageWriterTest {

    private BufferedImage bufferedImage;
    private Metadata metadata;
    private FileOutputStream outputStream;
    private PlanarImage planarImage;
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        final Configuration config = Configuration.getInstance();
        // Disable ICC profiles (will be re-enabled in certain tests)
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, false);
        // Disable metadata preservation (will be re-enabled in certain tests)
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false);

        // Read an image fixture into memory
        final File fixture = TestUtil.getImage("gif-xmp.gif");
        metadata = new GifImageReader(fixture).getMetadata(0);
        bufferedImage = new GifImageReader(fixture).read();
        planarImage =  PlanarImage.wrapRenderedImage(
                new GifImageReader(fixture).readRendered());

        // Create a temp file and output stream to write to
        tempFile = File.createTempFile("test", "tmp");
        outputStream = new FileOutputStream(tempFile);
    }

    @After
    public void tearDown() throws Exception {
        outputStream.close();
        tempFile.delete();
    }

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        getWriter().write(bufferedImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    public void testWriteWithBufferedImageAndIccProfile()  throws Exception {
        configureIccProfile();
        getWriter().write(bufferedImage, outputStream);
        checkForIccProfile();
    }

    @Test
    public void testWriteWithBufferedImageAndMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(bufferedImage, outputStream);
        checkForMetadata();
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        getWriter().write(planarImage, outputStream);
        ImageIO.read(tempFile);
    }

    @Test
    public void testWriteWithPlanarImageAndIccProfile() throws Exception {
        configureIccProfile();
        getWriter().write(planarImage, outputStream);
        checkForIccProfile();
    }

    @Test
    public void testWriteWithPlanarImageAndMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(planarImage, outputStream);
        checkForMetadata();
    }

    private void configureIccProfile() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, true);
        config.setProperty(IccProfileService.ICC_STRATEGY_CONFIG_KEY,
                "BasicStrategy");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_NAME_CONFIG_KEY,
                "test");
        config.setProperty(IccProfileService.ICC_BASIC_STRATEGY_PROFILE_CONFIG_KEY,
                TestUtil.getFixture("AdobeRGB1998.icc").getAbsolutePath());
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

    private GifImageWriter getWriter() throws IOException {
        OperationList opList = new OperationList();
        if (IccProfileService.isEnabled()) {
            IccProfile profile = new IccProfileService().getProfile(
                    new Identifier("cats"), Format.GIF, null, "127.0.0.1");
            opList.add(profile);
        }
        if (Configuration.getInstance().
                getBoolean(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }
        return new GifImageWriter(opList, metadata);
    }

}
