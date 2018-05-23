package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class GIFImageReaderTest extends AbstractImageReaderTest {

    @Override
    GIFImageReader newInstance() throws IOException {
        GIFImageReader reader = new GIFImageReader();
        reader.setSource(TestUtil.getImage("gif"));
        return reader;
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
        config.setProperty(GIFImageReader.IMAGEIO_PLUGIN_CONFIG_KEY, "cats");
        assertEquals("cats",
                ((GIFImageReader) instance).getUserPreferredIIOImplementation());
    }

    /* read() */

    @Test
    public void testReadWithArguments() throws Exception {
        OperationList ops = new OperationList();
        Crop crop = new Crop();
        crop.setX(10f);
        crop.setY(10f);
        crop.setWidth(40f);
        crop.setHeight(40f);
        ops.add(crop);
        Scale scale = new Scale(35, 35, Scale.Mode.ASPECT_FIT_INSIDE);
        ops.add(scale);
        Orientation orientation = Orientation.ROTATE_0;
        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        BufferedImage image = instance.read(ops, orientation, rf, hints);

        assertEquals(64, image.getWidth());
        assertEquals(56, image.getHeight());
        assertEquals(0, rf.factor);
        assertFalse(hints.contains(ReaderHint.ALREADY_CROPPED));
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
