package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class BMPImageReaderTest extends AbstractImageReaderTest {

    @Override
    BMPImageReader newInstance() throws IOException {
        BMPImageReader reader = new BMPImageReader();
        reader.setSource(TestUtil.getImage("bmp"));
        return reader;
    }

    /* preferredIIOImplementations() */

    @Test
    public void testPreferredIIOImplementations() {
        String[] impls = ((BMPImageReader) instance).preferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.codec.plugins.bmp.BMPImageReader", impls[0]);
    }

}
