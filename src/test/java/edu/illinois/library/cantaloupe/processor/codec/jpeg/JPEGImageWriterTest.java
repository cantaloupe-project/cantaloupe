package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Metadata;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageWriterTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
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

public class JPEGImageWriterTest extends AbstractImageWriterTest {

    @Override
    protected JPEGImageWriter newInstance() {
        JPEGImageWriter writer = new JPEGImageWriter();
        writer.setEncode(new Encode(Format.JPG));
        return writer;
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((JPEGImageWriter) instance).getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.jpeg.JPEGImageWriter", impls[0]);
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(JPEGImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((JPEGImageWriter) instance).getUserPreferredIIOImplementation();
        String[] appImpls = ((JPEGImageWriter) instance).getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((JPEGImageWriter) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(JPEGImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((JPEGImageWriter) instance).getUserPreferredIIOImplementation());
    }

    /* write() */

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Encode encode = new Encode(Format.JPG);
            Metadata outMetadata = new Metadata();
            outMetadata.setXMP(metadata.getXMP().orElseThrow());
            encode.setMetadata(outMetadata);
            instance.setEncode(encode);
            instance.write(image, os);
            os.close();
            ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithBufferedImageAndXMPMetadata() throws Exception {
        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final BufferedImage image = reader.read();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Encode encode = new Encode(Format.JPG);
            Metadata outMetadata = new Metadata();
            outMetadata.setXMP(metadata.getXMP().orElseThrow());
            encode.setMetadata(outMetadata);
            instance.setEncode(encode);
            instance.write(image, os);
            os.close();
            checkForXMPMetadata(os.toByteArray());
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
            Encode encode = new Encode(Format.JPG);
            Metadata outMetadata = new Metadata();
            outMetadata.setXMP(metadata.getXMP().orElseThrow());
            encode.setMetadata(outMetadata);
            instance.setEncode(encode);
            instance.write(image, os);
            os.close();
            ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        final JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg-xmp.jpg"));
        try {
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image = PlanarImage.wrapRenderedImage(
                    reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            Encode encode = new Encode(Format.JPG);
            Metadata outMetadata = new Metadata();
            outMetadata.setXMP(metadata.getXMP().orElseThrow());
            encode.setMetadata(outMetadata);
            instance.setEncode(encode);
            instance.write(image, os);
            os.close();
            checkForXMPMetadata(os.toByteArray());
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

    private void checkForXMPMetadata(byte[] imageData) throws Exception {
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
        final String preferredImpl =
                new JPEGImageReader().getPreferredIIOImplementations()[0];
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("JPEG");
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            if (reader.getClass().getName().equals(preferredImpl)) {
                return reader;
            }
        }
        return null;
    }

}
