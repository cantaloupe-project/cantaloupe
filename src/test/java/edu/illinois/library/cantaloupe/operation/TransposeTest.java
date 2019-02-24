package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class TransposeTest extends BaseTest {

    private Transpose instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.instance = Transpose.HORIZONTAL;
    }

    @Test
    public void getEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                Transpose.VERTICAL.getResultingSize(fullSize, scaleConstraint));
        assertEquals(fullSize,
                Transpose.HORIZONTAL.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new CropByPixels(0, 0, 300, 200));
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void toMap() {
        Dimension size = new Dimension(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals("horizontal", map.get("axis"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        instance = Transpose.HORIZONTAL;
        assertEquals("h", instance.toString());
        instance = Transpose.VERTICAL;
        assertEquals("v", instance.toString());
    }

}
