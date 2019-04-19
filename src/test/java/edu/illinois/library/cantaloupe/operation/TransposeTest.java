package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class TransposeTest extends BaseTest {

    private Transpose instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        this.instance = Transpose.HORIZONTAL;
    }

    @Test
    void getEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                Transpose.VERTICAL.getResultingSize(fullSize, scaleConstraint));
        assertEquals(fullSize,
                Transpose.HORIZONTAL.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new CropByPixels(0, 0, 300, 200));
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    void toMap() {
        Dimension size = new Dimension(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals("horizontal", map.get("axis"));
    }

    @Test
    void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertThrows(UnsupportedOperationException.class,
                () -> map.put("test", "test"));
    }

    @Test
    void testToString() {
        instance = Transpose.HORIZONTAL;
        assertEquals("h", instance.toString());
        instance = Transpose.VERTICAL;
        assertEquals("v", instance.toString());
    }

}
