package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
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
        assertEquals(fullSize, Transpose.VERTICAL.getResultingSize(fullSize));
        assertEquals(fullSize, Transpose.HORIZONTAL.getResultingSize(fullSize));
    }

    @Test
    public void hasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList(new Identifier("cats"),
                Format.JPG, new Crop(0, 0, 300, 200));
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void toMap() {
        Map<String,Object> map = instance.toMap(new Dimension(0, 0));
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals("horizontal", map.get("axis"));
    }

    @Test
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        try {
            map.put("test", "test");
            fail("Expected exception");
        } catch (UnsupportedOperationException e) {
            // pass
        }
    }

    @Test
    public void testToString() {
        instance = Transpose.HORIZONTAL;
        assertEquals("h", instance.toString());
        instance = Transpose.VERTICAL;
        assertEquals("v", instance.toString());
    }

}
