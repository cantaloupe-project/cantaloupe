package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
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

public class PNGImageWriterTest extends AbstractImageWriterTest {

    @Override
    PNGImageWriter newInstance() {
        PNGImageWriter writer = new PNGImageWriter();
        writer.setEncode(new Encode(Format.PNG));
        return writer;
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((PNGImageWriter) instance).getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.png.PNGImageWriter", impls[0]);
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(PNGImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((PNGImageWriter) instance).getUserPreferredIIOImplementation();
        String[] appImpls = ((PNGImageWriter) instance).getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((PNGImageWriter) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(PNGImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((PNGImageWriter) instance).getUserPreferredIIOImplementation());
    }

    /* write() */

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-xmp.png"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.setMetadata(metadata);
        instance.write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndNativeMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-nativemetadata.png"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForNativeMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXMPMetadata()  throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-xmp.png"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForXMPMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-xmp.png"));
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.setMetadata(metadata);
        instance.write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithPlanarImageAndNativeMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-nativemetadata.png"));
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForNativeMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final PNGImageReader reader = new PNGImageReader();
        reader.setSource(TestUtil.getImage("png-xmp.png"));
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForXMPMetadata(os.toByteArray());
    }

    private void checkForICCProfile(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final Node tree = metadata.getAsTree(metadata.getNativeMetadataFormatName());
            final Node iccNode = tree.getChildNodes().item(1);
            assertEquals("iCCP", iccNode.getNodeName());
        } finally {
            reader.dispose();
        }
    }

    private void checkForNativeMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final IIOMetadataNode tree = (IIOMetadataNode)
                    metadata.getAsTree(metadata.getNativeMetadataFormatName());

            boolean found = false;
            final NodeList textNodes = tree.getElementsByTagName("tEXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("Title".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
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
            final NodeList textNodes = tree.getElementsByTagName("iTXt").
                    item(0).getChildNodes();
            for (int i = 0; i < textNodes.getLength(); i++) {
                final Node keywordAttr = textNodes.item(i).getAttributes().
                        getNamedItem("keyword");
                if (keywordAttr != null) {
                    if ("XML:com.adobe.xmp".equals(keywordAttr.getNodeValue())) {
                        found = true;
                        break;
                    }
                }
            }
            assertTrue(found);
        } finally {
            reader.dispose();
        }
    }

    private ImageReader getIIOReader() {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("PNG");
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            String readerName = reader.getClass().getName();
            String preferredName = new PNGImageReader().
                    getPreferredIIOImplementations()[0];

            if (readerName.equals(preferredName)) {
                return reader;
            }
        }
        return null;
    }

}
