package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * This class is divided into two sections: one for Info and one for
 * Info.Image.
 */
public class InfoTest extends BaseTest {

    private Info instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withFormat(Format.JPG)
                .withNumResolutions(3)
                .withOrientation(Orientation.ROTATE_270)
                .build();
    }

    /************************ Info tests ****************************/

    /* fromJSON(Path) */

    @Test
    public void testFromJSONWithPath() throws Exception {
        Path tempFile = Files.createTempFile("test", "json");

        // Serialize the instance to JSON and write it to a file.
        String json = instance.toJSON();
        Files.write(tempFile, json.getBytes("UTF-8"));

        Info info = Info.fromJSON(tempFile);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON(InputStream) */

    @Test
    public void testFromJSONWithInputStream() throws Exception {
        String json = instance.toJSON();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        Info info = Info.fromJSON(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON(String) */

    @Test
    public void testFromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(info.toString(), instance.toString());
    }

    /* Info() */

    @Test
    public void testConstructor() {
        instance = new Info();
        assertEquals(1, instance.getImages().size());
    }

    /* equals() */

    @Test
    public void testEqualsWithEqualInstances() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_270)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertTrue(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentWidths() {
        Info info2 = Info.builder()
                .withSize(99, 80)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_270)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentHeights() {
        Info info2 = Info.builder()
                .withSize(100, 79)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_270)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentTileWidths() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(49, 40)
                .withOrientation(Orientation.ROTATE_270)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentTileHeights() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 39)
                .withOrientation(Orientation.ROTATE_270)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentOrientations() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_180)
                .withNumResolutions(3)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentNumResolutions() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_90)
                .withNumResolutions(2)
                .withFormat(Format.JPG)
                .build();
        assertFalse(instance.equals(info2));
    }

    @Test
    public void testEqualsWithDifferentFormats() {
        Info info2 = Info.builder()
                .withSize(100, 80)
                .withTileSize(50, 40)
                .withOrientation(Orientation.ROTATE_90)
                .withNumResolutions(3)
                .withFormat(Format.GIF)
                .build();
        assertFalse(instance.equals(info2));
    }

    /* getImages() */

    @Test
    public void testGetImages() {
        assertEquals(1, instance.getImages().size());
    }

    /* getNumResolutions() */

    @Test
    public void testGetNumResolutions() {
        assertEquals(3, instance.getNumResolutions());
    }

    /* getOrientation() */

    @Test
    public void testGetOrientation() {
        assertEquals(Orientation.ROTATE_270, instance.getOrientation());
    }

    /* getOrientationSize(int) */

    @Test
    public void testGetOrientationSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), instance.getOrientationSize());
    }

    /* getSize() */

    @Test
    public void testGetSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    /* getSize(int) */

    @Test
    public void testGetSizeWithIndex() {
        Info.Image image = new Info.Image();
        image.width = 50;
        image.height = 40;
        instance.getImages().add(image);

        image = new Info.Image();
        image.width = 25;
        image.height = 20;
        instance.getImages().add(image);

        assertEquals(new Dimension(25, 20), instance.getSize(2));
    }

    /* getSourceFormat() */

    @Test
    public void testGetSourceFormat() {
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    /* toJSON() */

    @Test
    public void testToJSON() throws Exception {
        String json = instance.toJSON();
        Info info2 = Info.fromJSON(json);
        assertEquals(instance, info2);
    }

    @Test
    public void testToJSONOmitsNullValues() throws Exception {
        String json = instance.toJSON();
        assertFalse(json.contains("null"));
    }

    /* toString() */

    @Test
    public void testToString() throws Exception {
        assertEquals(instance.toJSON(), instance.toString());
    }

    /* writeAsJSON() */

    @Test
    public void testWriteAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);
        assertTrue(Arrays.equals(baos.toByteArray(),
                instance.toJSON().getBytes()));
    }

    /********************* Info.Image tests *************************/

    @Test
    public void testImageConstructor() {
        Info.Image image = new Info.Image();
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(new Dimension(0, 0), image.getSize());
        assertEquals(new Dimension(0, 0), image.getTileSize());
    }

    @Test
    public void testImageGetOrientationSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationSize());
    }

    @Test
    public void testImageGetOrientationTileSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(40, 50), image.getOrientationTileSize());
    }

}
