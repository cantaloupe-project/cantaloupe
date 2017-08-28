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
        instance = new Info(100, 80, Format.JPG);
        instance.getImages().get(0).setOrientation(Orientation.ROTATE_270);
    }

    /************************ Info tests ****************************/

    /* fromJSON(File) */

    @Test
    public void fromJSONWithFile() throws Exception {
        Path tempFile = Files.createTempFile("test", "json");

        // Serialize the instance to JSON and write it to a file.
        String json = instance.toJSON();
        Files.write(tempFile, json.getBytes("UTF-8"));

        // Read the file back into a string.
        byte[] fileBytes = Files.readAllBytes(tempFile);
        String fileString = new String(fileBytes);

        assertEquals(fileString, json);
    }

    /* fromJSON(InputStream) */

    @Test
    public void fromJSONWithInputStream() throws Exception {
        String json = instance.toJSON();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        Info info = Info.fromJSON(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    /* fromJSON(String) */

    @Test
    public void fromJSONWithString() throws Exception {
        String json = instance.toJSON();
        Info info = Info.fromJSON(json);
        assertEquals(info.toString(), instance.toString());
    }

    /* Info() */

    @Test
    public void Info() {
        instance = new Info();
        assertEquals(0, instance.getImages().size());
    }

    /* Info(Dimension) */

    @Test
    public void InfoWithDimension() {
        Dimension size = new Dimension(500, 200);
        instance = new Info(size);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
    }

    /* Info(Dimension, Format) */

    @Test
    public void InfoWithDimensionAndFormat() {
        Dimension size = new Dimension(500, 200);
        Format format = Format.JPG;
        instance = new Info(size, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
        assertEquals(format, instance.getSourceFormat());
    }

    /* Info(int, int) */

    @Test
    public void InfoWithWidthAndHeight() {
        int width = 500;
        int height = 200;
        instance = new Info(width, height);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
    }

    /* Info(int, int, Format) */

    @Test
    public void InfoWithWidthAndHeightAndFormat() {
        int width = 500;
        int height = 200;
        Format format = Format.JPG;
        instance = new Info(width, height, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(format, instance.getSourceFormat());
    }

    /* Info(Dimension, Dimension) */

    @Test
    public void InfoWithWidthAndHeightDimensions() {
        Dimension size = new Dimension(500, 200);
        Dimension tileSize = new Dimension(300, 100);
        instance = new Info(size, tileSize);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
        assertEquals(tileSize, instance.getImages().get(0).getTileSize());
    }

    /* Info(int, int, int, int) */

    @Test
    public void InfoWithWidthAndHeightAndTileWidthAndTileHeight() {
        int width = 500;
        int height = 200;
        int tileWidth = 200;
        int tileHeight = 100;
        instance = new Info(width, height, tileWidth, tileHeight);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(tileWidth, instance.getImages().get(0).getTileSize().width);
        assertEquals(tileHeight, instance.getImages().get(0).getTileSize().height);
    }

    /* Info(int, int, int, int, Format) */

    @Test
    public void InfoWithWidthAndHeightAndTileWidthAndTileHeightAndFormat() {
        int width = 500;
        int height = 200;
        int tileWidth = 200;
        int tileHeight = 100;
        Format format = Format.GIF;
        instance = new Info(width, height, tileWidth, tileHeight, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(tileWidth, instance.getImages().get(0).getTileSize().width);
        assertEquals(tileHeight, instance.getImages().get(0).getTileSize().height);
        assertEquals(format, instance.getSourceFormat());
    }

    /* equals() */

    @Test
    public void equals() {
        // equal
        Info info1 = new Info(100, 80, 50, 40, Format.JPG);
        Info info2 = new Info(100, 80, 50, 40, Format.JPG);
        assertTrue(info1.equals(info2));
        // not equal
        info2 = new Info(99, 80, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new Info(100, 79, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new Info(100, 80, 49, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new Info(100, 80, 50, 39, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new Info(100, 80, 50, 40, Format.TIF);
        assertFalse(info1.equals(info2));
        info2 = new Info(100, 80, Format.JPG);
        assertFalse(info1.equals(info2));
    }

    /* getImages() */

    @Test
    public void getImages() {
        assertEquals(1, instance.getImages().size());
        assertEquals(0, new Info().getImages().size());
    }

    /* getOrientation() */

    @Test
    public void getOrientation() {
        assertEquals(Orientation.ROTATE_270, instance.getOrientation());
    }

    /* getOrientationSize(int) */

    @Test
    public void getOrientationSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), instance.getOrientationSize());
    }

    /* getSize() */

    @Test
    public void getSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    /* getSize(int) */

    @Test
    public void getSizeWithIndex() {
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

    @Test
    public void getSourceFormat() {
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    /* toJSON() */

    @Test
    public void toJSON() throws Exception {
        // We are going to trust that Jackson creates correct JSON, but also
        // test that null values are not serialized.
        String json = instance.toJSON();
        Info info2 = Info.fromJSON(json);
        info2.setSourceFormat(instance.getSourceFormat());
        assertEquals(instance, info2);
    }

    /* toString() */

    @Test
    public void testToString() throws Exception {
        assertEquals(instance.toJSON(), instance.toString());
    }

    /* writeAsJSON() */

    @Test
    public void writeAsJSON() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJSON(baos);
        assertTrue(Arrays.equals(baos.toByteArray(),
                instance.toJSON().getBytes()));
    }

    /********************* Info.Image tests *************************/

    @Test
    public void imageConstructor1() {
        Info.Image image = new Info.Image();
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(new Dimension(0, 0), image.getSize());
        assertEquals(new Dimension(0, 0), image.getTileSize());
    }

    @Test
    public void imageConstructor2() {
        Dimension size = new Dimension(300, 200);
        Info.Image image = new Info.Image(size);
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(size, image.getSize());
        assertEquals(size, image.getTileSize());
    }

    @Test
    public void imageConstructor3() {
        Dimension size = new Dimension(300, 200);
        Orientation orientation = Orientation.ROTATE_90;
        Info.Image image = new Info.Image(size, orientation);
        assertEquals(orientation, image.getOrientation());
        assertEquals(size, image.getSize());
        assertEquals(size, image.getTileSize());
    }

    @Test
    public void imageConstructor4() {
        int width = 300;
        int height = 200;
        Info.Image image = new Info.Image(width, height);
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(width, image.getSize().width);
        assertEquals(height, image.getSize().height);
        assertEquals(new Dimension(width, height), image.getTileSize());
    }

    @Test
    public void imageConstructor5() {
        int width = 300;
        int height = 200;
        Orientation orientation = Orientation.ROTATE_90;
        Info.Image image = new Info.Image(width, height, orientation);
        assertEquals(orientation, image.getOrientation());
        assertEquals(width, image.getSize().width);
        assertEquals(height, image.getSize().height);
        assertEquals(new Dimension(width, height), image.getTileSize());
    }

    @Test
    public void imageGetOrientationSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationSize());
    }

    @Test
    public void imageGetOrientationTileSize() {
        Info.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationTileSize());
    }

}
