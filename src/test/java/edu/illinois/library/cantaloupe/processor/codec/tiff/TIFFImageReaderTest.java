package edu.illinois.library.cantaloupe.processor.codec.tiff;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPercent;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPercent;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class TIFFImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtil.getImage("tif-rgb-3res-64x56x16-tiled-uncompressed.tif");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtil.getImage("jpg");
    }

    @Override
    protected TIFFImageReader newInstance() throws IOException {
        TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(getSupportedFixture());
        return reader;
    }

    /* canSeek() */

    @Test
    public void testCanSeek() {
        assertTrue(instance.canSeek());
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] expected = new String[2];
        expected[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.class.getName();
        expected[1] = "com.sun.imageio.plugins.tiff.TIFFImageReader";
        assertArrayEquals(expected,
                ((TIFFImageReader) instance).getApplicationPreferredIIOImplementations());
    }

    /* getCompression() */

    @Test
    @Override
    public void testGetCompression() {}

    @Test
    public void testGetCompressionWithUncompressedImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader();
        instance.setSource(TestUtil.getImage("tif-rgb-1res-64x56x16-striped-uncompressed.tif"));
        assertEquals(Compression.UNCOMPRESSED, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithJPEGImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader();
        instance.setSource(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-jpeg.tif"));
        assertEquals(Compression.JPEG, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithLZWImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader();
        instance.setSource(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-lzw.tif"));
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithPackBitsImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader();
        instance.setSource(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-packbits.tif"));
        assertEquals(Compression.RLE, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithDeflateImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader();
        instance.setSource(TestUtil.getImage("tif-rgb-1res-64x56x8-striped-zip.tif"));
        assertEquals(Compression.DEFLATE, instance.getCompression(0));
    }

    /* getNumImages() */

    @Override
    @Test
    public void testGetNumImages() throws Exception {
        assertEquals(3, instance.getNumImages());
    }

    /* getNumResolutions() */

    @Override
    @Test
    public void testGetNumResolutions() throws Exception {
        assertEquals(3, instance.getNumResolutions());
    }

    @Test
    public void testGetNumResolutionsWithNonPyramidalMultiImageTIFF()
            throws Exception {
        instance.setSource(TestUtil.getImage("tif-bw-9image.tif"));
        assertEquals(1, instance.getNumResolutions());
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(TIFFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((TIFFImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((TIFFImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((TIFFImageReader) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(TIFFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((TIFFImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* getTileSize() */

    @Test
    public void testGetTileSize() throws Exception {
        assertEquals(new Dimension(16, 16), instance.getTileSize(0));
    }

    /* read() */

    @Test
    void testRead1WithMultiResolutionImage() {
        // TODO: write this
    }

    @Test
    void testRead1WithMultiImageImage() throws Exception {
        instance.setSource(TestUtil.getImage("tif-bw-9image.tif"));
        // Read two distinct images.
        BufferedImage image1 = instance.read(0);
        BufferedImage image2 = instance.read(2);
        // Assert that they are different.
        assertNotEquals(image1.getWidth(), image2.getWidth());
    }

    @Test
    void testRead2WithMultiImageImage() throws Exception {
        instance.setSource(TestUtil.getImage("tif-bw-9image.tif"));
        Crop crop                       = new CropByPercent(0, 0, 1, 1);
        Scale scale                     = new ScaleByPercent(1.0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        ReductionFactor reductionFactor = new ReductionFactor(1);
        Set<ReaderHint> hints           = new HashSet<>();
        // Read two distinct images.
        BufferedImage image1 = instance.read(
                1, crop, scale, scaleConstraint, reductionFactor, hints);
        BufferedImage image2 = instance.read(
                2, crop, scale, scaleConstraint, reductionFactor, hints);
        // Assert that they are different.
        assertNotEquals(image1.getWidth(), image2.getWidth());
    }

    /* readSequence() */

    @Test
    @Override
    public void testReadSequenceWithStaticImage() throws Exception {
        assertEquals(3, instance.readSequence().length());
    }

}
