package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Encode;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.TestUtil;
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TIFFImageWriterTest extends AbstractImageWriterTest {

    @Override
    TIFFImageWriter newInstance() {
        OperationList opList = new OperationList(new Encode(Format.TIF));
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            opList.add(new MetadataCopy());
        }
        TIFFImageWriter writer = new TIFFImageWriter();
        writer.setOperationList(opList);
        return writer;
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((TIFFImageWriter) instance).getApplicationPreferredIIOImplementations();
        assertEquals(2, impls.length);
        assertEquals("it.geosolutions.imageioimpl.plugins.tiff.TIFFImageWriter",
                impls[0]);
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(TIFFImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((TIFFImageWriter) instance).getUserPreferredIIOImplementation();
        String[] appImpls = ((TIFFImageWriter) instance).getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((TIFFImageWriter) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(TIFFImageWriter.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((TIFFImageWriter) instance).getUserPreferredIIOImplementation());
    }

    /* write() */

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(TestUtil.getImage("tif-xmp.tif"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();
        reader.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.setMetadata(metadata);
        instance.write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndEXIFMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(TestUtil.getImage("tif-exif.tif"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();
        reader.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForEXIFMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndIPTCMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(TestUtil.getImage("tif-iptc.tif"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();
        reader.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForIPTCMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXMPMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(TestUtil.getImage("tif-xmp.tif"));
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();
        reader.dispose();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        instance.dispose();
        instance = newInstance();
        instance.setMetadata(metadata);
        instance.write(image, os);
        checkForXMPMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final TIFFImageReader reader = new TIFFImageReader();
        try {
            reader.setSource(TestUtil.getImage("tif-xmp.tif"));
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image =
                    PlanarImage.wrapRenderedImage(reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            instance.setMetadata(metadata);
            instance.write(image, os);
            ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithPlanarImageAndEXIFMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        try {
            reader.setSource(TestUtil.getImage("tif-exif.tif"));
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image =
                    PlanarImage.wrapRenderedImage(reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            instance.setMetadata(metadata);
            instance.write(image, os);
            checkForEXIFMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithPlanarImageAndIPTCMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        try {
            reader.setSource(TestUtil.getImage("tif-iptc.tif"));
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image =
                    PlanarImage.wrapRenderedImage(reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            instance.dispose();
            instance = newInstance();
            instance.setMetadata(metadata);
            instance.write(image, os);
            checkForIPTCMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    @Test
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final TIFFImageReader reader = new TIFFImageReader();
        try {
            reader.setSource(TestUtil.getImage("tif-xmp.tif"));
            final Metadata metadata = reader.getMetadata(0);
            final PlanarImage image =
                    PlanarImage.wrapRenderedImage(reader.readRendered());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            instance.dispose();
            instance = newInstance();
            instance.setMetadata(metadata);
            instance.write(image, os);
            checkForXMPMetadata(os.toByteArray());
        } finally {
            reader.dispose();
        }
    }

    private void checkForICCProfile(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            final TIFFTag tag = dir.getTag(BaselineTIFFTagSet.TAG_ICC_PROFILE);
            assertNotNull(tag);
        } finally {
            reader.dispose();
        }
    }

    private void checkForEXIFMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            final TIFFTag tag = dir.getTag(EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER);
            assertNotNull(tag);
        } finally {
            reader.dispose();
        }
    }

    private void checkForIPTCMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(33723));
        } finally {
            reader.dispose();
        }
    }

    private void checkForXMPMetadata(byte[] imageData) throws Exception {
        final ImageReader reader = getIIOReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(700));
        } finally {
            reader.dispose();
        }
    }

    private it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader getIIOReader() {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        while (readers.hasNext()) {
            ImageReader reader = readers.next();
            if (reader instanceof it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) {
                return (it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader) reader;
            }
        }
        return null;
    }

}
