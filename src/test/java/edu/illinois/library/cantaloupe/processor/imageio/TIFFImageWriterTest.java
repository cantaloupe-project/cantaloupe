package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.MetadataCopy;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.test.BaseTest;
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
import java.nio.file.Path;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TIFFImageWriterTest extends BaseTest {

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        final Path fixture = TestUtil.getImage("tif-xmp.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndEXIFMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final Path fixture = TestUtil.getImage("tif-exif.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForEXIFMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndIPTCMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final Path fixture = TestUtil.getImage("tif-iptc.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForIPTCMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXMPMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final Path fixture = TestUtil.getImage("tif-xmp.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final BufferedImage image = reader.read();

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForXMPMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        final Path fixture = TestUtil.getImage("tif-xmp.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithPlanarImageAndEXIFMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);

        final Path fixture = TestUtil.getImage("tif-exif.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForEXIFMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndIPTCMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final Path fixture = TestUtil.getImage("tif-iptc.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForIPTCMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndXMPMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.PROCESSOR_PRESERVE_METADATA, true);
        final Path fixture = TestUtil.getImage("tif-xmp.tif");
        final TIFFImageReader reader = new TIFFImageReader(fixture.toFile());
        final Metadata metadata = reader.getMetadata(0);
        final PlanarImage image =
                PlanarImage.wrapRenderedImage(reader.readRendered());

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter(metadata).write(image, os);
        checkForXMPMetadata(os.toByteArray());
    }

    private void checkForIccProfile(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
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
        final ImageReader reader = getReader();
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
        final ImageReader reader = getReader();
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
        final ImageReader reader = getReader();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(700));
        } finally {
            reader.dispose();
        }
    }

    private it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader getReader() {
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

    private TIFFImageWriter newWriter(Metadata metadata) {
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.TIF);
        if (Configuration.getInstance().
                getBoolean(Key.PROCESSOR_PRESERVE_METADATA, false)) {
            opList.add(new MetadataCopy());
        }
        return new TIFFImageWriter(opList, metadata);
    }

}
