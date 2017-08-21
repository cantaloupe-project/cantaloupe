package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Ignore;
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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class JPEGImageWriterTest extends BaseTest {

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final File fixture = TestUtil.getImage("jpg-xmp.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-exif.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-iptc.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-xmp.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final File fixture = TestUtil.getImage("jpg-xmp.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =  PlanarImage.wrapRenderedImage(
                reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-exif.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =  PlanarImage.wrapRenderedImage(
                reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-iptc.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =  PlanarImage.wrapRenderedImage(
                reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    @Ignore
    // TODO: JPEGImageWriter is writing JPEGs that JPEGImageReader thinks are
    // invalid, even though other readers have no problems. Maybe they are
    // invalid and other readers are more lenient.
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final File fixture = TestUtil.getImage("jpg-xmp.jpg");
        final JPEGImageReader reader = new JPEGImageReader(fixture);
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =  PlanarImage.wrapRenderedImage(
                reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        getWriter(metadata).write(image, os);
        checkForXmpMetadata(os.toByteArray());
    }

    private void checkForIccProfile(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
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

    private void checkForExifMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
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

    private void checkForIptcMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
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

    private void checkForXmpMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList unknowns = tree.getElementsByTagName("unknown");
            for (int i = 0; i < unknowns.getLength(); i++) {
                if ("225".equals(unknowns.item(i).getAttributes().
                        getNamedItem("MarkerTag").getNodeValue())) {
                    found = true;
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private JPEGImageWriter getWriter(Metadata metadata) throws IOException {
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG);
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            opList.add(new MetadataCopy());
        }
        return new JPEGImageWriter(opList, metadata);
    }

}
