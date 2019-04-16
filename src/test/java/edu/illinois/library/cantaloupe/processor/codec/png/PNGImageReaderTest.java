package edu.illinois.library.cantaloupe.processor.codec.png;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class PNGImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected PNGImageReader newInstance() throws IOException {
        PNGImageReader reader =new PNGImageReader();
        reader.setSource(TestUtil.getImage("png"));
        return reader;
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

        String userImpl = ((PNGImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((PNGImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((PNGImageReader) instance).getPreferredIIOImplementations());
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
