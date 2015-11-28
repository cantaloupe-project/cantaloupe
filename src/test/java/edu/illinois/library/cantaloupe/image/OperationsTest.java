package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class OperationsTest extends CantaloupeTestCase {

    private Operations ops;

    public void setUp() {
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops.add(crop);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);

        assertNotNull(ops.getOptions());
    }

    public void testAdd() {
        ops = new Operations();
        assertFalse(ops.iterator().hasNext());
        ops.add(new Rotation());
        assertTrue(ops.iterator().hasNext());
    }

    public void testCompareTo() {
        Operations ops2 = new Operations();
        ops2.setIdentifier(new Identifier("identifier.jpg"));
        Crop crop = new Crop();
        crop.setFull(true);
        ops2.add(crop);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops2.add(scale);
        ops2.add(new Rotation(0));
        ops2.add(Filter.DEFAULT);
        ops2.setOutputFormat(OutputFormat.JPG);
        assertEquals(0, ops2.compareTo(this.ops));
    }

    public void testIsNoOp1() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        Rotation rotation = new Rotation(0);
        Filter filter = Filter.DEFAULT;
        OutputFormat format = OutputFormat.JPG;
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier"));
        ops.add(crop);
        ops.add(scale);
        ops.add(rotation);
        ops.add(filter);
        ops.setOutputFormat(format);
        assertFalse(ops.isNoOp());
    }

    public void testIsNoOp2() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.JPG);
        assertFalse(ops.isNoOp()); // false because the identifier has no discernible source format
    }

    public void testIsNoOp3() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
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
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

    public void testIsNoOp5() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

    public void testIsNoOp6() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.ASPECT_FIT_INSIDE);
        scale.setPercent(0.5f);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

    public void testIsNoOp7() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(2));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertFalse(ops.isNoOp());
    }

    public void testIsNoOp8() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);;
        ops.add(new Rotation());
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

    public void testIsNoOp9() {
        Crop crop = new Crop();
        crop.setFull(true);
        Scale scale = new Scale();
        scale.setMode(Scale.Mode.FULL);
        ops = new Operations();
        ops.setIdentifier(new Identifier("identifier.gif"));
        ops.add(crop);
        ops.add(scale);
        ops.add(new Rotation(0));
        ops.add(Filter.DEFAULT);
        ops.setOutputFormat(OutputFormat.GIF);
        assertTrue(ops.isNoOp());
    }

    public void testToString() {
        List<String> parts = new ArrayList<>();
        parts.add(ops.getIdentifier().toString());
        for (Operation op : ops) {
            parts.add(op.toString());
        }
        String expected = StringUtils.join(parts, "_") + "." +
                ops.getOutputFormat().getExtension();
        assertEquals(expected, ops.toString());
    }

}
