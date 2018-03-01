package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class GIFImageReaderTest extends AbstractImageReaderTest {

    @Override
    GIFImageReader newInstance() throws IOException {
        GIFImageReader reader = new GIFImageReader();
        reader.setSource(TestUtil.getImage("gif"));
        return reader;
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    @Test
    public void testPreferredIIOImplementations() {
        String[] impls = ((GIFImageReader) instance).preferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.codec.plugins.gif.GIFImageReader", impls[0]);
    }

    /* readSequence() */

    @Test
    @Override
    public void testReadSequence() {}

    @Test
    public void testReadSequenceWithStaticImage() throws Exception {
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(1, seq.length());
    }

    @Test
    public void testReadSequenceWithAnimatedImage() throws Exception {
        instance = new GIFImageReader();
        instance.setSource(TestUtil.getImage("gif-animated-looping.gif"));
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(2, seq.length());
    }

}
