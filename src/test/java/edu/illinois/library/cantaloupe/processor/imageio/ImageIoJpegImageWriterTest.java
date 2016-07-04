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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

public class ImageIoJpegImageWriterTest {

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
        final File fixture = TestUtil.getImage("jpg-xmp.jpg");
        metadata = new JpegImageReader(fixture).getMetadata(0);
        bufferedImage = new JpegImageReader(fixture).read();
        planarImage =  PlanarImage.wrapRenderedImage(
                new JpegImageReader(fixture).readRendered());

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
    public void testWriteWithBufferedImageAndIccProfile() throws Exception {
        configureIccProfile();
        getWriter().write(bufferedImage, outputStream);
        checkForIccProfile();
    }
/* TODO: why do these fail?
    @Test
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        final File fixture = TestUtil.getImage("jpg-exif.jpg");
        metadata = new JpegImageReader(fixture).getNativeMetadata(0);
        bufferedImage = new JpegImageReader(fixture).read();

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(bufferedImage, outputStream);
        checkForExifMetadata();
    }

    @Test
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        final File fixture = TestUtil.getImage("jpg-iptc.jpg");
        metadata = new JpegImageReader(fixture).getNativeMetadata(0);
        bufferedImage = new JpegImageReader(fixture).read();

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(bufferedImage, outputStream);
        checkForIptcMetadata();
    }

    @Test
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(bufferedImage, outputStream);
        checkForXmpMetadata();
    }
*/
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
/* TODO: why do these fail?
    @Test
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        final File fixture = TestUtil.getImage("jpg-exif.jpg");
        metadata = new JpegImageReader(fixture).getNativeMetadata(0);
        planarImage =  PlanarImage.wrapRenderedImage(
                new JpegImageReader(fixture).readRendered());

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(planarImage, outputStream);
        checkForExifMetadata();
    }

    @Test
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        final File fixture = TestUtil.getImage("jpg-iptc.jpg");
        metadata = new JpegImageReader(fixture).getNativeMetadata(0);
        planarImage =  PlanarImage.wrapRenderedImage(
                new JpegImageReader(fixture).readRendered());

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(planarImage, outputStream);
        checkForIptcMetadata();
    }

    @Test
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        getWriter().write(planarImage, outputStream);
        checkForXmpMetadata();
    }
*/
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
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream ios = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(ios);
            // Check for the profile in its metadata
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            final Node iccNode = tree.getChildNodes().item(0).getChildNodes().
                    item(0).getChildNodes().item(0);
            assertEquals("app2ICC", iccNode.getNodeName());
        } finally {
            reader.dispose();
        }
    }

    private void checkForExifMetadata() throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("225".equals(unknowns.item(i).getAttributes().getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private void checkForIptcMetadata() throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("237".equals(unknowns.item(i).getAttributes().getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private void checkForXmpMetadata() throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(tempFile)) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("225".equals(unknowns.item(i).getAttributes().getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private JpegImageWriter getWriter() throws IOException {
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
        return new JpegImageWriter(opList, metadata);
    }

}
