package edu.illinois.library.cantaloupe.processor.codec.bmp;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class BMPImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtil.getImage("bmp");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtil.getImage("png");
    }

    @Override
    protected BMPImageReader newInstance() throws IOException {
        BMPImageReader reader = new BMPImageReader();
        reader.setSource(getSupportedFixture());
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

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(BMPImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((BMPImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((BMPImageReader) instance).
                getApplicationPreferredIIOImplementations();

        String[] expected = new String[appImpls.length + 1];
        expected[0] = userImpl;
        System.arraycopy(appImpls, 0, expected, 1, appImpls.length);

        assertArrayEquals(expected,
                ((BMPImageReader) instance).getPreferredIIOImplementations());
    }

    @Test
    public void testGetUserPreferredIIOImplementation() {
        Configuration config = Configuration.getInstance();
        config.setProperty(BMPImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((BMPImageReader) instance).getUserPreferredIIOImplementation());
    }

}
