package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;

import static org.junit.Assert.*;

public class JPEGImageReaderTest extends AbstractImageReaderTest {

    JPEGImageReader newInstance() throws IOException {
        JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(TestUtil.getImage("jpg"));
        return reader;
    }

    @Test
    @Override
    public void testGetCompression() throws Exception {
        assertEquals(Compression.JPEG, instance.getCompression(0));
    }

    @Test
    public void testPreferredIIOImplementations() {
        String[] impls = ((JPEGImageReader) instance).preferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.jpeg.JPEGImageReader", impls[0]);
    }

    /* read() */

    @Test
    public void testReadWithYCCKImage() throws Exception {
        instance = new JPEGImageReader();
        instance.setSource(TestUtil.getImage("jpg-ycck.jpg"));

        BufferedImage result = instance.read();
        assertEquals(64, result.getWidth());
        assertEquals(56, result.getHeight());
    }

}
