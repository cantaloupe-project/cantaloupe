package edu.illinois.library.cantaloupe.processor.imageio;

import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.operation.ReductionFactor;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class JPEG2000ImageReaderTest extends BaseTest {

    private JPEG2000ImageReader instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new JPEG2000ImageReader(TestUtil.getImage("jp2"));
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        instance.dispose();
    }

    @Test
    public void testGetCompression() {
        assertEquals(Compression.JPEG2000, instance.getCompression(0));
    }

    @Test
    public void testGetIIOReader() {
        assertNotNull(instance.getIIOReader());
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
    public void testGetSize() throws Exception {
        assertEquals(new Dimension(100, 88), instance.getSize());
    }

    @Test
    public void testGetSizeWithIndex() throws Exception {
        assertEquals(new Dimension(100, 88), instance.getSize(0));
    }

    @Test
    public void testGetTileSize() throws Exception {
        assertEquals(new Dimension(100, 88), instance.getTileSize(0));
    }

    @Test
    public void testPreferredIIOImplementations() {
        assertEquals(0, instance.preferredIIOImplementations().length);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testRead() {
        instance.read();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadWithArguments() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
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
        Set<ImageReader.Hint> hints = new HashSet<>();

        instance.read(ops, orientation, rf, hints);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadRendered() {
        instance.read();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadRenderedWithArguments() {
        OperationList ops = new OperationList(new Identifier("cats"), Format.JPG);
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
        Set<ImageReader.Hint> hints = new HashSet<>();

        instance.readRendered(ops, orientation, rf, hints);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadSequence() {
        instance.readSequence();
    }

}
