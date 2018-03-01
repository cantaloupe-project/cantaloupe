package edu.illinois.library.cantaloupe.processor.codec;

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
import java.util.Iterator;

import static org.junit.Assert.*;

/**
 * N.B.: {@link com.sun.imageio.plugins.jpeg.JPEGImageReader} strictly
 * interprets the JFIF spec, which requires that the JFIF application marker
 * be placed immediately after SOI in the header. But EXIF requires that its
 * own marker be immediately after SOI, so there is no way to produce a strict
 * JFIF file with embedded EXIF info, thus all EXIF JPEGs are noncompliant and
 * JPEGImageReader won't read them.
 */
public class JPEGImageWriterTest extends BaseTest {

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-exif.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForExifMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-iptc.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForIptcMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForXmpMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image = PlanarImage.wrapRenderedImage(
                    reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-exif.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image = PlanarImage.wrapRenderedImage(
                    reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForExifMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-iptc.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image = PlanarImage.wrapRenderedImage(
                    reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForIptcMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    @Ignore // see N.B. in class doc
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image = PlanarImage.wrapRenderedImage(
                    reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            getWriter(metadata).write(image, os);
            os.close();
            checkForXmpMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    private void checkForIccProfile(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
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
        final ImageReader reader = getIIOReader();
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
        final ImageReader reader = getIIOReader();
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
        final ImageReader reader = getIIOReader();
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

    private ImageReader getIIOReader() {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            if (reader.getClass().getName().equals(JPEGImageReader.getPreferredIIOImplementations()[0])) {
                return reader;
            }
        }
        return null;
    }

    private JPEGImageWriter getWriter(Metadata metadata) {
        OperationList opList = new OperationList(
                new Identifier("cats"), Format.JPG);
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            opList.add(new MetadataCopy());
        }
        return new JPEGImageWriter(opList, metadata);
    }

}
