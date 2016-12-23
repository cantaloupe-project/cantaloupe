package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class TransposeTest extends BaseTest {

    private Transpose transpose;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        this.transpose = Transpose.HORIZONTAL;
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Transpose.VERTICAL.getResultingSize(fullSize));
        assertEquals(fullSize, Transpose.HORIZONTAL.getResultingSize(fullSize));
    }

    @Test
    public void testHasEffect() {
        assertTrue(transpose.hasEffect());
    }

    @Test
    public void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));
        assertTrue(transpose.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Map<String,Object> map = transpose.toMap(new Dimension(0, 0));
        assertEquals(transpose.getClass().getSimpleName(), map.get("class"));
        assertEquals("horizontal", map.get("axis"));
    }

    @Test
    public void testToString() {
        transpose = Transpose.HORIZONTAL;
        assertEquals("h", transpose.toString());
        transpose = Transpose.VERTICAL;
        assertEquals("v", transpose.toString());
    }

}
