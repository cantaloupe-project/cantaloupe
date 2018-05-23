package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.image.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

abstract class AbstractImageReaderTest extends BaseTest {

    private static final Dimension FIXTURE_SIZE = new Dimension(64, 56);

    ImageReader instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @After
    public void tearDown() {
        if (instance != null) {
            instance.dispose();
        }
    }

    abstract ImageReader newInstance() throws IOException;

    @Test
    public void testGetCompression() throws Exception {
        assertEquals(Compression.UNCOMPRESSED, instance.getCompression(0));
    }

    @Test
    public void testGetMetadata() throws Exception {
        assertNotNull(instance.getMetadata(0));
    }

    @Test
    public void testGetNumImages() throws Exception {
        assertEquals(1, instance.getNumImages());
    }

    @Test
    public void testGetNumResolutions() throws Exception {
        assertEquals(1, instance.getNumResolutions());
    }

    @Test
    public void testGetPreferredIIOImplementationsWithNoUserPreference() {
        String[] impls = ((AbstractIIOImageReader) instance).
                getPreferredIIOImplementations();
        assertArrayEquals(((AbstractIIOImageReader) instance).
                getApplicationPreferredIIOImplementations(), impls);
    }

    @Test
    public void testGetSize() throws Exception {
        assertEquals(FIXTURE_SIZE, instance.getSize(0));
    }

    @Test
    public void testGetTileSize() throws Exception {
        assertEquals(FIXTURE_SIZE, instance.getTileSize(0));
    }

    @Test
    public void testRead() throws Exception {
        BufferedImage result = instance.read();
        assertEquals(FIXTURE_SIZE.width, result.getWidth());
        assertEquals(FIXTURE_SIZE.height, result.getHeight());
    }

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

    @Test
    public void testReadSmallestUsableSubimageReturningBufferedImage() {
        // TODO: write this
    }

    @Test
    public void testReadRendered() throws Exception {
        RenderedImage result = instance.read();
        assertEquals(FIXTURE_SIZE.width, result.getWidth());
        assertEquals(FIXTURE_SIZE.height, result.getHeight());
    }

    @Test
    public void testReadRenderedWithArguments() throws Exception {
        // TODO: write this
    }

    @Test
    public void testReadSmallestUsableSubimageReturningRenderedImage() {
        // TODO: write this
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadSequence() throws Exception {
        instance.readSequence();
    }

}
