package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.operation.CropByPixels;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public abstract class AbstractImageReaderTest extends BaseTest {

    private static final double DELTA = 0.00000001;
    private static final Dimension FIXTURE_SIZE = new Dimension(64, 56);

    protected ImageReader instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = newInstance();
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        if (instance != null) {
            instance.dispose();
        }
    }

    abstract protected ImageReader newInstance() throws IOException;

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
    void testGetPreferredIIOImplementationsWithNoUserPreference() {
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
        assertEquals(FIXTURE_SIZE.width(), result.getWidth(), DELTA);
        assertEquals(FIXTURE_SIZE.height(), result.getHeight(), DELTA);
    }

    @Test
    public void testReadWithArguments() throws Exception {
        OperationList ops = new OperationList(
                new CropByPixels(10, 10, 40, 40),
                new Scale(35, 35, Scale.Mode.ASPECT_FIT_INSIDE));

        ReductionFactor rf = new ReductionFactor();
        Set<ReaderHint> hints = new HashSet<>();

        BufferedImage image = instance.read(ops, rf, hints);

        assertEquals(40, image.getWidth());
        assertEquals(40, image.getHeight());
        assertEquals(0, rf.factor);
        assertTrue(hints.contains(ReaderHint.ALREADY_CROPPED));
    }

    @Test
    void testReadSmallestUsableSubimageReturningBufferedImage() {
        // TODO: write this
    }

    @Test
    public void testReadRendered() throws Exception {
        RenderedImage result = instance.read();
        assertEquals(FIXTURE_SIZE.width(), result.getWidth(), DELTA);
        assertEquals(FIXTURE_SIZE.height(), result.getHeight(), DELTA);
    }

    @Test
    public void testReadRenderedWithArguments() throws Exception {
        // TODO: write this
    }

    @Test
    void testReadSmallestUsableSubimageReturningRenderedImage() {
        // TODO: write this
    }

    @Test
    public void testReadSequence() {
        assertThrows(UnsupportedOperationException.class,
                () -> instance.readSequence());
    }

}
