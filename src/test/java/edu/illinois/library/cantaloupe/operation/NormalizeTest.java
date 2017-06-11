package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class NormalizeTest extends BaseTest {

    private Normalize instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Normalize();
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(500, 500);
        assertEquals(fullSize, instance.getResultingSize(fullSize));
    }

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());
    }

    @Test
    public void testhasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));
        assertTrue(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() throws Exception {
        Dimension fullSize = new Dimension(500, 500);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
    }

    @Test
    public void testToString() {
        assertEquals("normalize", instance.toString());
    }

}