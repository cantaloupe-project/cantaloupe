package edu.illinois.library.cantaloupe.operation;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Compression;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.overlay.BasicStringOverlayServiceTest;
import edu.illinois.library.cantaloupe.operation.overlay.StringOverlay;
import edu.illinois.library.cantaloupe.operation.redaction.RedactionServiceTest;
import edu.illinois.library.cantaloupe.processor.Processor;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OperationListTest extends BaseTest {

    private OperationList instance;

    private static OperationList newOperationList() {
        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops.add(crop);
        Scale scale = new Scale();
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.JPG);
        ops.setOutputCompression(Compression.JPEG);
        ops.setOutputQuality(80);
        ops.setOutputInterlacing(true);
        return ops;
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = newOperationList();
        assertNotNull(instance.getOptions());
    }

    /* add(Operation) */

    @Test
    public void testAdd() {
        instance = new OperationList();
        assertFalse(instance.iterator().hasNext());
        instance.add(new Rotate());
        assertTrue(instance.iterator().hasNext());
    }

    @Test
    public void testAddWhileFrozen() {
        instance = new OperationList();
        instance.freeze();
        try {
            instance.add(new Rotate());
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* applyNonEndpointMutations() */

    @Test
    public void testApplyNonEndpointMutationsWithJPEGOutputFormat()
            throws Exception {
        final Configuration config = Configuration.getInstance();

        //////////////////////////// Setup ////////////////////////////////

        // redactions
        RedactionServiceTest.setUpConfiguration();
        // overlay
        BasicStringOverlayServiceTest.setUpConfiguration();
        // scale filters
        config.setProperty(Processor.DOWNSCALE_FILTER_CONFIG_KEY, "bicubic");
        config.setProperty(Processor.UPSCALE_FILTER_CONFIG_KEY, "triangle");
        // sharpening
        config.setProperty(Processor.SHARPEN_CONFIG_KEY, 0.2f);
        // metadata copies
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        // background color
        config.setProperty(Processor.BACKGROUND_COLOR_CONFIG_KEY, "white");
        // JPEG quality
        config.setProperty(Processor.JPG_QUALITY_CONFIG_KEY, 50);
        // JPEG progressive
        config.setProperty(Processor.JPG_PROGRESSIVE_CONFIG_KEY, true);

        ///////////////////////////// Test ////////////////////////////////

        final OperationList opList = new OperationList();
        opList.add(new Scale(0.5f));
        opList.add(new Rotate(45));
        opList.setOutputFormat(Format.JPG);
        final Dimension fullSize = new Dimension(2000,1000);

        opList.applyNonEndpointMutations(
                fullSize, "127.0.0.1", new URL("http://example.org/"),
                new HashMap<>(), new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Sharpen);
        assertTrue(it.next() instanceof StringOverlay);
        assertTrue(it.next() instanceof MetadataCopy);

        assertEquals(Color.fromString("#FFFFFF"),
                ((Rotate) opList.getFirst(Rotate.class)).getFillColor());
        assertEquals(50, opList.getOutputQuality());
        assertTrue(opList.isOutputInterlacing());
    }

    @Test
    public void testAddNonEndpointMutationsWithTIFFOutputFormat()
            throws IOException {
        final Configuration config = Configuration.getInstance();

        //////////////////////////// Setup ////////////////////////////////

        // redactions
        RedactionServiceTest.setUpConfiguration();
        // overlay
        BasicStringOverlayServiceTest.setUpConfiguration();
        // scale filters
        config.setProperty(Processor.DOWNSCALE_FILTER_CONFIG_KEY, "bicubic");
        config.setProperty(Processor.UPSCALE_FILTER_CONFIG_KEY, "triangle");
        // sharpening
        config.setProperty(Processor.SHARPEN_CONFIG_KEY, 0.2f);
        // metadata copies
        config.setProperty(Processor.PRESERVE_METADATA_CONFIG_KEY, true);
        // TIFF compression
        config.setProperty(Processor.TIF_COMPRESSION_CONFIG_KEY, "LZW");

        ///////////////////////////// Test ////////////////////////////////

        final OperationList opList = new OperationList();
        opList.add(new Scale(0.5f));
        opList.add(new Rotate(45));
        opList.setOutputFormat(Format.TIF);
        final Dimension fullSize = new Dimension(2000,1000);

        opList.applyNonEndpointMutations(
                fullSize, "127.0.0.1", new URL("http://example.org/"),
                new HashMap<>(), new HashMap<>());

        Iterator<Operation> it = opList.iterator();
        assertEquals(Scale.Filter.BICUBIC, ((Scale) it.next()).getFilter());
        assertTrue(it.next() instanceof Rotate);
        assertTrue(it.next() instanceof Sharpen);
        assertTrue(it.next() instanceof StringOverlay);
        assertTrue(it.next() instanceof MetadataCopy);
        assertEquals(Compression.LZW, opList.getOutputCompression());
    }

    /* clear() */

    @Test
    public void testClear() {
        int opCount = 0;
        Iterator it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(3, opCount);
        instance.clear();

        opCount = 0;
        it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(0, opCount);
    }

    @Test
    public void testClearWhileFrozen() {
        instance.freeze();
        try {
            instance.clear();
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* compareTo(OperationList) */

    @Test
    public void testCompareTo() {
        OperationList ops2 = new OperationList();
        ops2.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops2.add(crop);
        Scale scale = new Scale();
        ops2.add(scale);
        ops2.add(new Rotate(0));
        ops2.setOutputFormat(Format.JPG);
        ops2.setOutputCompression(Compression.JPEG);
        ops2.setOutputQuality(80);
        ops2.setOutputInterlacing(true);
        assertEquals(0, ops2.compareTo(this.instance));
    }

    /* equals(Object) */

    @Test
    public void testEqualsWithEqualOperationList() {
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate());
        assertTrue(ops1.equals(ops2));
    }

    @Test
    public void testEqualsWithUnequalOperationList() {
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate(1));
        assertFalse(ops1.equals(ops2));
    }

    /* getFirst(Class<Operation>) */

    @Test
    public void testGetFirst() {
        assertNull(instance.getFirst(MetadataCopy.class));
        assertNotNull(instance.getFirst(Scale.class));
    }

    /* getOptions() */

    @Test
    public void testGetOptions() {
        assertNotNull(instance.getOptions());
    }

    @Test
    public void testGetOptionsWhileFrozen() {
        instance.freeze();
        try {
            instance.getOptions().put("test", "test");
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* getResultingSize(Dimension) */

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(300, 200);
        instance = new OperationList();
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        Rotate rotate = new Rotate();
        instance.add(crop);
        instance.add(scale);
        instance.add(rotate);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        instance = new OperationList();
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        scale = new Scale(0.5f);
        instance.add(crop);
        instance.add(scale);
        assertEquals(new Dimension(75, 50), instance.getResultingSize(fullSize));
    }

    /* isNoOp(Format) */

    @Test
    public void testIsNoOpWithFormat() {
        // same format
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.gif"));
        instance.setOutputFormat(Format.GIF);
        assertTrue(instance.isNoOp(Format.GIF));

        // different formats
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.jpg"));
        instance.setOutputFormat(Format.GIF);
        assertFalse(instance.isNoOp(Format.JPG));
    }

    @Test
    public void testIsNoOpWithPdfSourceAndPdfOutputAndOverlay() {
        // same format
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.pdf"));
        instance.setOutputFormat(Format.PDF);
        assertTrue(instance.isNoOp(Format.PDF));
    }

    /* iterator() */

    @Test
    public void testIterator() {
        int count = 0;
        Iterator it = instance.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void testIteratorCannotRemoveWhileFrozen() {
        instance.freeze();
        Iterator it = instance.iterator();
        it.next();
        try {
            it.remove();
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* setIdentifier() */

    @Test
    public void testSetIdentifierWhileFrozen() {
        instance.freeze();
        try {
            instance.setIdentifier(new Identifier("alpaca"));
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* setOutputFormat() */

    @Test
    public void testSetOutputFormatWhileFrozen() {
        instance.freeze();
        try {
            instance.setOutputFormat(Format.GIF);
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    /* toFilename() */

    @Test
    public void testToFilename() {
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setX(5f);
        crop.setY(6f);
        crop.setWidth(20f);
        crop.setHeight(22f);
        instance.add(crop);
        Scale scale = new Scale(0.4f);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.setOutputFormat(Format.JPG);
        instance.getOptions().put("animal", "cat");

        String expected = "50c63748527e634134449ae20b199cc0_f694166f0f0aa4f0a88d5d7a7315a15f.jpg";
        assertEquals(expected, instance.toFilename());

        // Assert that changing an operation changes the filename
        crop.setX(10f);
        assertNotEquals(expected, instance.toFilename());

        // Assert that changing an option changes the filename
        crop.setX(5f);
        assertEquals(expected, instance.toFilename());
        instance.getOptions().put("animal", "dog");
        assertNotEquals(expected, instance.toFilename());
    }

    /* toMap() */

    @Test
    public void testToMap() {
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.jpg"));
        // crop
        Crop crop = new Crop();
        crop.setX(2);
        crop.setY(4);
        crop.setWidth(50);
        crop.setHeight(50);
        instance.add(crop);
        // no-op scale
        Scale scale = new Scale();
        instance.add(scale);
        instance.add(new Rotate(0));
        // transpose
        instance.add(Transpose.HORIZONTAL);
        // output
        instance.setOutputFormat(Format.JPG);
        instance.setOutputCompression(Compression.JPEG);
        instance.setOutputInterlacing(true);
        instance.setOutputQuality(80);

        final Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(2, ((List) map.get("operations")).size());
        assertEquals(0, ((Map) map.get("options")).size());
        assertEquals("jpg", ((Map) map.get("output_format")).get("extension"));
        assertEquals(80, map.get("output_quality"));
        assertTrue((boolean) map.get("output_interlacing"));
        assertEquals("JPEG", map.get("output_compression"));
    }

    /* toString() */

    @Test
    public void testToString() {
        instance = new OperationList();
        instance.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setX(5f);
        crop.setY(6f);
        crop.setWidth(20f);
        crop.setHeight(22f);
        instance.add(crop);
        Scale scale = new Scale(0.4f);
        instance.add(scale);
        instance.add(new Rotate(15));
        instance.add(ColorTransform.BITONAL);
        instance.setOutputFormat(Format.JPG);
        instance.setOutputInterlacing(true);
        instance.setOutputQuality(80);
        instance.setOutputCompression(Compression.JPEG);
        instance.getOptions().put("animal", "cat");

        String expected = "identifier.jpg_crop:5,6,20,22_scale:40%_rotate:15_null_colortransform:bitonal_animal:cat_interlace_quality:80_compression:JPEG.jpg";
        assertEquals(expected, instance.toString());
    }


    /* validate() */

    @Test
    public void testValidateWithValidInstance() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList();
        Crop crop = new Crop(0, 0, 100, 100);
        ops.add(crop);
        try {
            ops.validate(fullSize);
        } catch (ValidationException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testValidateWithOutOfBoundsCrop() {
        Dimension fullSize = new Dimension(1000, 1000);
        OperationList ops = new OperationList();
        Crop crop = new Crop(1001, 1001, 100, 100);
        ops.add(crop);
        try {
            ops.validate(fullSize);
            fail("Expected exception");
        } catch (ValidationException e) {
            // pass
        }
    }

}
