package edu.illinois.library.cantaloupe.processor.codec.gif;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.operation.ScaleByPixels;
import edu.illinois.library.cantaloupe.processor.codec.AbstractIIOImageReader;
import edu.illinois.library.cantaloupe.processor.codec.AbstractImageReaderTest;
import edu.illinois.library.cantaloupe.processor.codec.BufferedImageSequence;
import edu.illinois.library.cantaloupe.processor.codec.ReaderHint;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class GIFImageReaderTest extends AbstractImageReaderTest {

    @Override
    protected Path getSupportedFixture() {
        return TestUtil.getImage("gif");
    }

    @Override
    protected Path getUnsupportedFixture() {
        return TestUtil.getImage("png");
    }

    @Override
    protected GIFImageReader newInstance() throws IOException {
        GIFImageReader reader = new GIFImageReader();
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
        String[] impls = ((GIFImageReader) instance).
                getApplicationPreferredIIOImplementations();
        assertEquals(1, impls.length);
        assertEquals("com.sun.imageio.plugins.gif.GIFImageReader", impls[0]);
    }

    @Test
    @Override
    public void testGetCompression() throws IOException {
        assertEquals(Compression.LZW, instance.getCompression(0));
    }

    /* getPreferredIIOImplementations() */

    @Test
    public void testGetPreferredIIOImplementationsWithUserPreference() {
        Configuration config = Configuration.getInstance();
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");

        String userImpl = ((GIFImageReader) instance).
                getUserPreferredIIOImplementation();
        String[] appImpls = ((GIFImageReader) instance).
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
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((GIFImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* read() */

    @Test
    public void testReadWithArguments() throws Exception {
        Crop crop             = new CropByPixels(10, 10, 40, 40);
        Scale scale           = new ScaleByPixels(35, 35, ScaleByPixels.Mode.ASPECT_FIT_INSIDE);
        ScaleConstraint sc    = new ScaleConstraint(1, 1);
        ReductionFactor rf    = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        BufferedImage image = instance.read(0, crop, scale, sc, rf, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
        assertTrue(hints.contains(ReaderHint.ALREADY_CROPPED));
    }

    /* readSequence() */

    @Test
    public void testReadSequenceWithAnimatedImage() throws Exception {
        instance = new GIFImageReader();
        instance.setSource(TestUtil.getImage("gif-animated-looping.gif"));
        BufferedImageSequence seq = instance.readSequence();
        assertEquals(2, seq.length());
    }

}
