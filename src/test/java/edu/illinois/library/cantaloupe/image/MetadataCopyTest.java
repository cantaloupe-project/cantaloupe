package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class MetadataCopyTest {

    private MetadataCopy instance;

    @Before
    public void setUp() throws Exception {
        instance = new MetadataCopy();
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(500, 500);
        assertEquals(fullSize, instance.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertFalse(instance.isNoOp());
    }

    @Test
    public void testToMap() throws Exception {
        Dimension fullSize = new Dimension(500, 500);
        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
    }

    @Test
    public void testToString() {
        assertEquals("mdcopy", instance.toString());
    }

}