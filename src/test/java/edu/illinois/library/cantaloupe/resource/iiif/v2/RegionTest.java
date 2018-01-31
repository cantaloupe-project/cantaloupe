package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RegionTest extends BaseTest {

    private static final float DELTA = 0.0000001f;

    private Region instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new Region();
        instance.setType(Region.Type.PERCENT);
        instance.setX(20f);
        instance.setY(20f);
        instance.setWidth(20f);
        instance.setHeight(20f);
    }

    /* fromUri(String) */

    /**
     * Tests fromUri(String) with a value of "full".
     */
    @Test
    public void testFromUriFull() {
        Region r = Region.fromUri("full");
        assertEquals(Region.Type.FULL, r.getType());
    }

    /**
     * Tests fromUri(String) with a value of "square".
     */
    @Test
    public void testFromUriSquare() {
        Region r = Region.fromUri("square");
        assertEquals(Region.Type.SQUARE, r.getType());
    }

    /**
     * Tests fromUri(String) with absolute pixel values.
     */
    @Test
    public void testFromUriAbsolute() {
        Region r = Region.fromUri("0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PIXELS, r.getType());
    }

    /**
     * Tests fromUri(String) with percentage values.
     */
    @Test
    public void testFromUriPercentage() {
        Region r = Region.fromUri("pct:0,0,50,40");
        assertEquals(0f, r.getX(), DELTA);
        assertEquals(0f, r.getY(), DELTA);
        assertEquals(50f, r.getWidth(), DELTA);
        assertEquals(40f, r.getHeight(), DELTA);
        assertEquals(Region.Type.PERCENT, r.getType());
    }

    @Test
    public void testFromUriWithNegativeX() {
        try {
            Region.fromUri("pct:-2,3,50,50");
        } catch (IllegalClientArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testFromUriWithNegativeY() {
        try {
            Region.fromUri("pct:2,-3,50,50");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testFromUriWithNegativeWidth() {
        try {
            Region.fromUri("2,3,-50,50");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testFromUriWithNegativeHeight() {
        try {
            Region.fromUri("2,3,50,-50");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* equals */

    @Test
    public void testEqualsWithEqualRegion1() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertTrue(instance.equals(region2));
    }

    @Test
    public void testEqualsWithEqualRegion2() {
        instance.setType(Region.Type.SQUARE);

        Region region2 = new Region();
        region2.setType(Region.Type.SQUARE);
        assertTrue(instance.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion1() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(50f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(instance.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion2() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(50f);
        region2.setWidth(20f);
        region2.setHeight(20f);
        assertFalse(instance.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion3() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(50f);
        region2.setHeight(20f);
        assertFalse(instance.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion4() {
        Region region2 = new Region();
        region2.setType(Region.Type.PERCENT);
        region2.setX(20f);
        region2.setY(20f);
        region2.setWidth(20f);
        region2.setHeight(50f);
        assertFalse(instance.equals(region2));
    }

    @Test
    public void testEqualsWithUnequalRegion5() {
        Region region2 = new Region();
        region2.setType(Region.Type.SQUARE);
        instance.setX(20f);
        instance.setY(20f);
        instance.setWidth(20f);
        instance.setHeight(20f);
        assertFalse(instance.equals(region2));
    }

    @Test
    public void testEqualsWithEqualCrop1() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertTrue(instance.equals(crop));
    }

    @Test
    public void testEqualsWithEqualCrop2() {
        instance.setType(Region.Type.SQUARE);
        Crop crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        assertTrue(instance.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop1() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.5f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(instance.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop2() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.5f);
        crop.setWidth(0.2f);
        crop.setHeight(0.2f);
        assertFalse(instance.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop3() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.5f);
        crop.setHeight(0.2f);
        assertFalse(instance.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop4() {
        Crop crop = new Crop();
        crop.setUnit(Crop.Unit.PERCENT);
        crop.setX(0.2f);
        crop.setY(0.2f);
        crop.setWidth(0.2f);
        crop.setHeight(0.5f);
        assertFalse(instance.equals(crop));
    }

    @Test
    public void testEqualsWithUnequalCrop5() {
        Crop crop = new Crop();
        crop.setShape(Crop.Shape.SQUARE);
        assertFalse(instance.equals(crop));
    }

    /* height */

    @Test
    public void testSetHeight() {
        float height = 50f;
        this.instance.setHeight(height);
        assertEquals(height, this.instance.getHeight(), DELTA);
    }

    @Test
    public void testSetNegativeHeight() {
        try {
            this.instance.setHeight(-1f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroHeight() {
        try {
            this.instance.setHeight(0f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Height must be a positive integer", e.getMessage());
        }
    }

    /* width */

    @Test
    public void testSetWidth() {
        float width = 50f;
        this.instance.setWidth(width);
        assertEquals(width, this.instance.getWidth(), DELTA);
    }

    @Test
    public void testSetNegativeWidth() {
        try {
            this.instance.setWidth(-1f);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    @Test
    public void testSetZeroWidth() {
        try {
            this.instance.setWidth(0f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Width must be a positive integer", e.getMessage());
        }
    }

    /* x */

    @Test
    public void testSetX() {
        Float x = 50.0f;
        this.instance.setX(x);
        assertEquals(x, this.instance.getX());
    }

    @Test
    public void testSetNegativeX() {
        try {
            this.instance.setX(-1f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("X must be a positive float", e.getMessage());
        }
    }

    /* y */

    @Test
    public void testSetY() {
        float y = 50.0f;
        this.instance.setY(y);
        assertEquals(y, this.instance.getY(), DELTA);
    }

    @Test
    public void testSetNegativeY() {
        try {
            this.instance.setY(-1f);
            fail("Expected exception");
        } catch (IllegalClientArgumentException e) {
            assertEquals("Y must be a positive float", e.getMessage());
        }
    }

    @Test
    public void testToCrop() {
        instance = new Region();
        instance.setX(30f);
        instance.setY(40f);
        instance.setWidth(50f);
        instance.setHeight(50f);
        instance.setType(Region.Type.PERCENT);
        assertTrue(instance.equals(instance.toCrop()));

        instance = new Region();
        instance.setType(Region.Type.SQUARE);
        assertTrue(instance.equals(instance.toCrop()));
    }

    @Test
    public void testToString() {
        Region r = Region.fromUri("full");
        assertEquals("full", r.toString());

        r = Region.fromUri("square");
        assertEquals("square", r.toString());

        r = Region.fromUri("0,0,50,40");
        assertEquals("0,0,50,40", r.toString());

        r = Region.fromUri("pct:0,0,50,40");
        assertEquals("pct:0,0,50,40", r.toString());
    }

}
