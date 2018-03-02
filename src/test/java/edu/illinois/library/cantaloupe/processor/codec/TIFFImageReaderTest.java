package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.Test;

import java.awt.Dimension;
import java.io.IOException;

import static org.junit.Assert.*;

public class TIFFImageReaderTest extends AbstractImageReaderTest {

    @Override
    TIFFImageReader newInstance() throws IOException {
        TIFFImageReader reader = new TIFFImageReader();
        reader.setSource(TestUtil.getImage("tif-rgb-3res-64x56x16-tiled-uncompressed.tif"));
        return reader;
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

    /* getTileSize() */

    @Test
    public void testGetTileSize() throws Exception {
        assertEquals(new Dimension(16, 16), instance.getTileSize(0));
    }

    /* preferredIIOImplementations() */

    @Test
    public void testPreferredIIOImplementations() {
        String[] expected = new String[2];
        expected[0] = it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader.class.getName();
        if (SystemUtils.getJavaMajorVersion() >= 9) {
            expected[1] = "com.sun.imageio.plugins.tiff.TIFFImageReader";
        } else {
            expected[1] = "com.sun.media.imageioimpl.plugins.tiff.TIFFImageReader";
        }
        assertArrayEquals(expected,
                ((TIFFImageReader) instance).preferredIIOImplementations());
    }

    @Test
    public void testReadWithMultiResolutionImage() {
        // TODO: write this
    }

}
