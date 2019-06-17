package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
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

    /* canSeek() */

    @Test
    public void testCanSeek() {
        assertFalse(instance.canSeek());
    }

    /* getApplicationPreferredIIOImplementations() */

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((BMPImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.bmp.BMPImageReader", impls[0]);
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(BMPImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

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
        config.setProperty(BMPImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((BMPImageReader) instance).getUserPreferredIIOImplementation());
    }

}
