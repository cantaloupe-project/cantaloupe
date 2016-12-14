package edu.illinois.library.cantaloupe.operation.redaction;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.OperationList;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Map;

import static org.junit.Assert.*;

public class RedactionTest {

    private Redaction instance;

    @Before
    public void setUp() {
        instance = new Redaction(new Rectangle(50, 60, 200, 100));
    }

    @Test
    public void testGetResultingRegion() {
        Dimension sourceSize = new Dimension(500, 500);

        // redaction within source image bounds
        Crop crop = new Crop(0, 0, 300, 300);
        Rectangle resultingRegion = instance.getResultingRegion(sourceSize, crop);
        assertEquals(new Rectangle(50, 60, 200, 100), resultingRegion);

        // redaction partially within source image bounds
        crop = new Crop(0, 0, 100, 100);
        resultingRegion = instance.getResultingRegion(sourceSize, crop);
        assertEquals(new Rectangle(50, 60, 200, 100), resultingRegion);

        // redaction outside source image bounds
        crop = new Crop(300, 300, 100, 100);
        resultingRegion = instance.getResultingRegion(sourceSize, crop);
        assertEquals(new Rectangle(0, 0, 0, 0), resultingRegion);
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(500, 500);
        assertEquals(fullSize, instance.getResultingSize(fullSize));
    }

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());

        // zero width
        instance = new Redaction(new Rectangle(50, 60, 0, 100));
        assertFalse(instance.hasEffect());

        // zero height
        instance = new Redaction(new Rectangle(50, 60, 50, 0));
        assertFalse(instance.hasEffect());

        // null region
        instance = new Redaction(null);
        assertFalse(instance.hasEffect());
    }

    @Test
    public void testIsNoOpWithArguments() {
        final Dimension fullSize = new Dimension(600, 400);
        final OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 400, 300));

        // in bounds
        assertTrue(instance.hasEffect(fullSize, opList));

        // out of bounds
        instance = new Redaction(new Rectangle(420, 305, 20, 20));
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void testToMap() {
        Dimension fullSize = new Dimension(500, 500);

        Map<String,Object> map = instance.toMap(fullSize);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(50, map.get("x"));
        assertEquals(60, map.get("y"));
        assertEquals(200, map.get("width"));
        assertEquals(100, map.get("height"));
    }

    @Test
    public void testToString() {
        assertEquals("50,60/200x100", instance.toString());
    }

}
