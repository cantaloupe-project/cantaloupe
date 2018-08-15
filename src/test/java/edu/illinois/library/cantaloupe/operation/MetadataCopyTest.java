package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class MetadataCopyTest extends BaseTest {

    private MetadataCopy instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new MetadataCopy();
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    public void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(500, 500);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
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
        assertEquals("mdcopy", instance.toString());
    }

}