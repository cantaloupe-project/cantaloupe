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
import it.geosolutions.imageio.plugins.tiff.BaselineTIFFTagSet;
import it.geosolutions.imageio.plugins.tiff.TIFFDirectory;
import it.geosolutions.imageio.plugins.tiff.TIFFTag;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.*;

public class TiffImageWriterTest {

    private BufferedImage bufferedImage;
    private Metadata metadata;
    private PlanarImage planarImage;

    @Before
    public void setUp() throws Exception {
        final Configuration config = Configuration.getInstance();
        // Disable ICC profiles (will be re-enabled in certain tests)
        config.setProperty(IccProfileService.ICC_ENABLED_CONFIG_KEY, false);
        // Disable metadata preservation (will be re-enabled in certain tests)
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false);

        // Read an image fixture into memory
        final File fixture = TestUtil.getImage("tif-xmp.tif");
        metadata = new TiffImageReader(fixture).getMetadata(0);
        bufferedImage = new TiffImageReader(fixture).read();
        planarImage =  PlanarImage.wrapRenderedImage(
                new TiffImageReader(fixture).readRendered());
    }

    @Test
    public void testWriteWithBufferedImage() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(bufferedImage, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithBufferedImageAndIccProfile() throws Exception {
        configureIccProfile();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(bufferedImage, os);
        checkForIccProfile(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(bufferedImage, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndIptcMetadata() throws Exception {
        final File fixture = TestUtil.getImage("tif-iptc.tif");
        metadata = new TiffImageReader(fixture).getMetadata(0);
        bufferedImage = new TiffImageReader(fixture).read();

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(bufferedImage, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithBufferedImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(bufferedImage, os);
        checkForXmpMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImage() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(planarImage, os);
        ImageIO.read(new ByteArrayInputStream(os.toByteArray()));
    }

    @Test
    public void testWriteWithPlanarImageAndIccProfile() throws Exception {
        configureIccProfile();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(planarImage, os);
        checkForIccProfile(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndExifMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(planarImage, os);
        checkForExifMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndIptcMetadata() throws Exception {
        final File fixture = TestUtil.getImage("tif-iptc.tif");
        metadata = new TiffImageReader(fixture).getMetadata(0);
        planarImage =  PlanarImage.wrapRenderedImage(
                new TiffImageReader(fixture).readRendered());

        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(planarImage, os);
        checkForIptcMetadata(os.toByteArray());
    }

    @Test
    public void testWriteWithPlanarImageAndXmpMetadata() throws Exception {
        final Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        newWriter().write(planarImage, os);
        checkForXmpMetadata(os.toByteArray());
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

    private void checkForExifMetadata(byte[] imageData) throws Exception {
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

    private void checkForIptcMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(33723));
        } finally {
            reader.dispose();
        }
    }

    private void checkForXmpMetadata(byte[] imageData) throws Exception {
        final Iterator<ImageReader> readers =
                ImageIO.getImageReadersByFormatName("TIFF");
        final ImageReader reader = readers.next();
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(imageData))) {
            reader.setInput(iis);
            final IIOMetadata metadata = reader.getImageMetadata(0);
            final TIFFDirectory dir = TIFFDirectory.createFromMetadata(metadata);
            assertNotNull(dir.getTIFFField(700));
        } finally {
            reader.dispose();
        }
    }

    private TiffImageWriter newWriter() throws IOException {
        OperationList opList = new OperationList();
        if (IccProfileService.isEnabled()) {
            IccProfile profile = new IccProfileService().getProfile(
                    new Identifier("cats"), Format.TIF, null, "127.0.0.1");
            opList.add(profile);
        }
        if (Configuration.getInstance().
                getBoolean(AbstractResource.PRESERVE_METADATA_CONFIG_KEY, false)) {
            opList.add(new MetadataCopy());
        }
        return new TiffImageWriter(opList, metadata);
    }

}
