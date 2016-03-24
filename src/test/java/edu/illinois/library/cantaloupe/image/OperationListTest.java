package edu.illinois.library.cantaloupe.image;

import static org.junit.Assert.*;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.watermark.WatermarkService;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OperationListTest {

    private OperationList ops;

    private static OperationList newOperationList() {
        OperationList ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.JPG);
        return ops;
    }

    @Before
    public void setUp() {
        Configuration config = Configuration.getInstance();
        config.clear();
        config.setProperty(WatermarkService.WATERMARK_ENABLED_CONFIG_KEY, false);

        ops = newOperationList();

        assertNotNull(ops.getOptions());
    }

    /* add(Operation) */

    @Test
    public void testAdd() {
        ops = new OperationList();
        assertFalse(ops.iterator().hasNext());
        ops.add(new Rotate());
        assertTrue(ops.iterator().hasNext());
    }

    /* clear() */

    @Test
    public void testClear() {
        int opCount = 0;
        Iterator it = ops.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(3, opCount);
        ops.clear();

        opCount = 0;
        it = ops.iterator();
        while (it.hasNext()) {
            it.next();
            opCount++;
        }
        assertEquals(0, opCount);
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
        scale.setMode(Scale.Mode.FULL);
        ops2.add(scale);
        ops2.add(new Rotate(0));
        ops2.setOutputFormat(Format.JPG);
        assertEquals(0, ops2.compareTo(this.ops));
    }

    /* contains() */

    @Test
    public void testContains() {
        assertTrue(ops.contains(Scale.class));
        assertFalse(ops.contains(Filter.class));
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

    /* getResultingSize(Dimension) */

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(300, 200);
        ops = new OperationList();
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate();
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        assertEquals(fullSize, ops.getResultingSize(fullSize));

        ops = new OperationList();
        crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setWidth(0.5f);
        crop.setHeight(0.5f);
        scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        ops.add(crop);
        ops.add(scale);
        assertEquals(new Dimension(75, 50), ops.getResultingSize(fullSize));
    }

    /* isNoOp() */

    @Test
    public void testIsNoOp1() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate(0);
        Format format = Format.JPG;
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier"));
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.setOutputFormat(format);
        assertFalse(ops.isNoOp());
    }

    @Test
    public void testIsNoOp2() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.JPG);
        assertFalse(ops.isNoOp()); // false because the identifier has no discernible source format
    }

    @Test
    public void testIsNoOp3() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.GIF);
        assertTrue(ops.isNoOp());
    }

    @Test
    public void testIsNoOp4() {
        Crop crop = new Crop();
        crop.setFull(false);
        crop.setX(30f);
        crop.setY(30f);
        crop.setWidth(30f);
        crop.setHeight(30f);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.GIF);
        assertFalse(ops.isNoOp());
    }

    @Test
    public void testIsNoOp5() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.GIF);
        assertTrue(ops.isNoOp());
    }

    @Test
    public void testIsNoOp6() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.GIF);
        assertFalse(ops.isNoOp());
    }

    @Test
    public void testIsNoOp7() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(2));
        ops.setOutputFormat(Format.GIF);
        assertFalse(ops.isNoOp());
    }

    @Test
    public void testIsNoOp8() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);;
        ops.add(new Rotate());
        ops.setOutputFormat(Format.GIF);
        assertTrue(ops.isNoOp());
    }

    @Test
    public void testIsNoOp9() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.GIF);
        assertTrue(ops.isNoOp());
    }

    /* isNoOp(Format) */

    @Test
    public void testIsNoOpWithSourceFormat() {
        // same format
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.setOutputFormat(Format.GIF);
        assertTrue(ops.isNoOp(Format.GIF));

        // different formats
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        ops.setOutputFormat(Format.GIF);
        assertFalse(ops.isNoOp(Format.JPG));
    }

    @Test
    public void testIsNoOpWithPdfSourceAndPdfOutputAndWatermark() {
        // same format
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.pdf"));
        ops.setOutputFormat(Format.PDF);
        assertTrue(ops.isNoOp(Format.PDF));
    }

    /* iterator() */

    @Test
    public void testIterator() {
        int count = 0;
        Iterator it = ops.iterator();
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertEquals(3, count);
    }

    /* toMap() */

    @Test
    public void testToMap() {
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        // crop
        Crop crop = new Crop();
        crop.setX(2);
        crop.setY(4);
        crop.setWidth(50);
        crop.setHeight(50);
        ops.add(crop);
        // no-op scale
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.setOutputFormat(Format.JPG);
        // transpose
        ops.add(Transpose.HORIZONTAL);

        final Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = ops.toMap(fullSize);
        assertEquals("identifier.jpg", map.get("identifier"));
        assertEquals(2, ((List) map.get("operations")).size());
        assertEquals(0, ((Map) map.get("options")).size());
        assertEquals("jpg", ((Map) map.get("output_format")).get("extension"));
    }

    /* toString() */

    @Test
    public void testToString() {
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setX(5f);
        crop.setY(6f);
        crop.setWidth(20f);
        crop.setHeight(22f);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setPercent(0.4f);
        ops.add(scale);
        ops.add(new Rotate(15));
        ops.add(Filter.BITONAL);
        ops.setOutputFormat(Format.JPG);
        ops.getOptions().put("animal", "cat");

        List<String> parts = new ArrayList<>();
        parts.add(ops.getIdentifier().toString());
        for (Operation op : ops) {
            if (!op.isNoOp()) {
                parts.add(op.getClass().getSimpleName().toLowerCase() + ":" +
                        op.toString());
            }
        }
        for (String key : ops.getOptions().keySet()) {
            parts.add(key + ":" + ops.getOptions().get(key));
        }
        String expected = StringUtils.join(parts, "_") + "." +
                ops.getOutputFormat().getPreferredExtension();
        assertEquals(expected, ops.toString());
    }

}
