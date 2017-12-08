package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TIFFImageReaderTest extends BaseTest {

    private TIFFImageReader instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-multires-64x56x16-tiled-uncompressed.tif").toFile());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        instance.dispose();
    }

    @Test
    public void testGetCompressionWithUncompressedImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif").toFile());
        assertEquals(Compression.UNCOMPRESSED, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithJPEGImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-jpeg.tif").toFile());
        assertEquals(Compression.JPEG, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithLZWImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-lzw.tif").toFile());
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithPackBitsImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-packbits.tif").toFile());
        assertEquals(Compression.RLE, instance.getCompression(0));
    }

    @Test
    public void testGetCompressionWithDeflateImage() throws Exception {
        instance.dispose();
        instance = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-zip.tif").toFile());
        assertEquals(Compression.DEFLATE, instance.getCompression(0));
    }

    @Test
    public void testGetIIOReader() {
        assertTrue(instance.getIIOReader() instanceof
                it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader);
    }

    @Test
    public void testGetMetadata() throws Exception {
        assertNotNull(instance.getMetadata(0));
    }

    @Test
    public void testGetNumResolutions() throws Exception {
        assertEquals(3, instance.getNumResolutions());
    }

    @Test
    public void testGetSize() throws Exception {
        assertEquals(new Dimension(64, 56), instance.getSize());
    }

    @Test
    public void testGetSizeWithIndex() throws Exception {
        assertEquals(new Dimension(64, 56), instance.getSize(0));
    }

    @Test
    public void testGetTileSize() throws Exception {
        assertEquals(new Dimension(16, 16), instance.getTileSize(0));
    }

    @Test
    public void testPreferredIIOImplementations() {
        String[] expected = new String[2];
        expected[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.class.getName();
        if (SystemUtils.getJavaVersion() >= 9) {
            expected[1] = "com.sun.imageio.plugins.tiff.TIFFImageReader";
        } else {
            expected[1] = "com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader";
        }
        assertArrayEquals(expected, instance.preferredIIOImplementations());
    }

    @Test
    public void testReadWithMonoResolutionImageAndNoScaleFactor() throws Exception {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
        Crop crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(40f);
        crop.setHeight(40f);
        ops.add(crop);
        Scale scale = new Scale(35, 35, Scale.Mode.ASPECT_FIT_INSIDE);
        ops.add(scale);
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ImageReader.Hint> hints = new HashSet<>();

        BufferedImage image = instance.read(ops, orientation, rf, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
        assertTrue(hints.contains(ImageReader.Hint.ALREADY_CROPPED));
    }

    @Test
    public void testReadWithMultiResolutionImage() {
        // TODO: write this
    }

}
