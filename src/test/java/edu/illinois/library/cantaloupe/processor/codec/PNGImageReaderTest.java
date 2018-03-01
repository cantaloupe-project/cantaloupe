package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PNGImageReaderTest extends AbstractImageReaderTest {

    @Override
    PNGImageReader newInstance() throws IOException {
        PNGImageReader reader =new PNGImageReader();
        reader.setSource(TestUtil.getImage("png"));
        return reader;
    }

    @Test
    public void testGetCompression() throws Exception {
        assertEquals(Compression.DEFLATE, instance.getCompression(0));
    }

    @Test
    public void testPreferredIIOImplementations() {
        String[] impls = ((PNGImageReader) instance).preferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.png.PNGImageReader", impls[0]);
    }

}
