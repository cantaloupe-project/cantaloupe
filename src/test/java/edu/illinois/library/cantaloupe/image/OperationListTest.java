package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.StringUtils;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

public class OperationListTest extends CantaloupeTestCase {

    private OperationList ops;

    public void setUp() {
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops.add(scale);
        ops.add(new Rotate(0));
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.JPG);

        assertNotNull(ops.getOptions());
    }

    public void testAdd() {
        ops = new OperationList();
        assertFalse(ops.iterator().hasNext());
        ops.add(new Rotate());
        assertTrue(ops.iterator().hasNext());
    }

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
        ops2.add(Filter.NONE);
        ops2.setOutputFormat(OutputFormat.JPG);
        assertEquals(0, ops2.compareTo(this.ops));
    }

    public void testEqualsWithEqualOperationList() {
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate());
        assertTrue(ops1.equals(ops2));
    }

    public void testEqualsWithUnequalOperationList() {
        OperationList ops1 = TestUtil.newOperationList();
        OperationList ops2 = TestUtil.newOperationList();
        ops2.add(new Rotate(1));
        assertFalse(ops1.equals(ops2));
    }

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

    public void testIsNoOp1() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotate rotate = new Rotate(0);
        Filter filter = Filter.NONE;
        OutputFormat format = OutputFormat.JPG;
        ops = new OperationList();
        ops.setIdentifier(new Identifier("identifier"));
        ops.add(crop);
        ops.add(scale);
        ops.add(rotate);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertFalse(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.JPG);
        assertFalse(ops.isNoOp()); // false because the identifier has no discernible source format
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

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
        ops.add(Filter.NONE);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

    public void testToString() {
        List<String> parts = new ArrayList<>();
        parts.add(ops.getIdentifier().toString());
        for (Operation op : ops) {
            if (!op.isNoOp()) {
                parts.add(op.toString());
            }
        }
        String expected = StringUtils.join(parts, "_") + "." +
                ops.getOutputFormat().getExtension();
        assertEquals(expected, ops.toString());
    }

}
