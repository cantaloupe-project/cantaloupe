package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class TransposeTest {

    private Transpose transpose;

    @Before
    public void setUp() {
        this.transpose = Transpose.HORIZONTAL;
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(200, 200);
        assertEquals(fullSize, Transpose.VERTICAL.getResultingSize(fullSize));
        assertEquals(fullSize, Transpose.HORIZONTAL.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertFalse(transpose.isNoOp());
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
