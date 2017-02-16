package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class TIFFImageReaderTest extends BaseTest {

    private TIFFImageReader reader;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-multires-64x56x16-tiled-uncompressed.tif"));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        reader.dispose();
    }

    @Test
    public void testGetCompression() throws Exception {
        reader.dispose();

        // uncompressed
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x16-striped-uncompressed.tif"));
        assertEquals(Compression.UNCOMPRESSED, reader.getCompression(0));
        reader.dispose();

        // JPEG
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-jpeg.tif"));
        assertEquals(Compression.JPEG, reader.getCompression(0));
        reader.dispose();

        // LZW
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-lzw.tif"));
        assertEquals(Compression.LZW, reader.getCompression(0));
        reader.dispose();

        // PackBits
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-packbits.tif"));
        assertEquals(Compression.RLE, reader.getCompression(0));
        reader.dispose();

        // Deflate/ZLib/Zip
        reader = new TIFFImageReader(
                TestUtil.getImage("tif-rgb-monores-64x56x8-striped-zip.tif"));
        assertEquals(Compression.ZLIB, reader.getCompression(0));
        reader.dispose();
    }

    @Test
    public void testGetMetadata() throws Exception {
        assertNotNull(reader.getMetadata(0));
    }

    @Test
    public void testReadWithMonoResolutionImageAndNoScaleFactor() throws Exception {
        OperationList ops = new OperationList();
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

        BufferedImage image = reader.read(ops, orientation, rf, hints);

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
