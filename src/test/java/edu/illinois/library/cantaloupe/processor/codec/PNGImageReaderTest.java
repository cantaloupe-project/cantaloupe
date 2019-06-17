package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
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

    /* canSeek() */

    @Test
    public void testCanSeek() {
        assertFalse(instance.canSeek());
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((PNGImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.png.PNGImageReader", impls[0]);
    }

    /* getCompression() */

    @Test
    public void testGetCompression() throws Exception {
        assertEquals(Compression.DEFLATE, instance.getCompression(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(PNGImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((AbstractIIOImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((AbstractIIOImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((AbstractIIOImageReader) instance).getPreferredIIOImplementations());
    }

    /* getUserPreferredIIOImplementation() */

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(PNGImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((PNGImageReader) instance).getUserPreferredIIOImplementation());
    }

}
