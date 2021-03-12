package edu.illinois.library.cantaloupe.processor.codec.jpeg;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class JPEGImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtil.getImage("jpg");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtil.getImage("png");
    }

    protected JPEGImageReader newInstance() throws IOException {
        JPEGImageReader reader = new JPEGImageReader();
        reader.setSource(getSupportedFixture());
        return reader;
    }

    /* canSeek() */

    @Test
    public void testCanSeek() {
        assertFalse(instance.canSeek());
    }

    @Test
    public void testGetApplicationPreferredIIOImplementations() {
        String[] impls = ((JPEGImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.jpeg.JPEGImageReader", impls[0]);
    }

    @Test
    @Override
    public void testGetCompression() throws Exception {
        assertEquals(Compression.JPEG, instance.getCompression(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(JPEGImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((JPEGImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((JPEGImageReader) instance).
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
        config.setProperty(JPEGImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((JPEGImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* read() */

    @Test
    public void testReadWithYCCKImage() throws Exception {
        instance = new JPEGImageReader();
        instance.setSource(TestUtil.getImage("jpg-ycck.jpg"));

        BufferedImage result = instance.read(0);
        assertEquals(64, result.getWidth());
        assertEquals(56, result.getHeight());
    }

}
