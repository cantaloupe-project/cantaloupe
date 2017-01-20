package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * This class is divided into two sections: one for ImageInfo and one for
 * ImageInfo.Image.
 */
public class ImageInfoTest extends BaseTest {

    private ImageInfo instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ImageInfo(100, 80, Format.JPG);
        instance.getImages().get(0).setOrientation(Orientation.ROTATE_270);
    }

    /************************ ImageInfo tests ****************************/

    @Test
    public void testConstructor1() {
        instance = new ImageInfo();
        assertEquals(0, instance.getImages().size());
    }

    @Test
    public void testConstructor2() {
        Dimension size = new Dimension(500, 200);
        instance = new ImageInfo(size);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
    }

    @Test
    public void testConstructor3() {
        Dimension size = new Dimension(500, 200);
        Format format = Format.JPG;
        instance = new ImageInfo(size, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
        assertEquals(format, instance.getSourceFormat());
    }

    @Test
    public void testConstructor4() {
        int width = 500;
        int height = 200;
        instance = new ImageInfo(width, height);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
    }

    @Test
    public void testConstructor5() {
        int width = 500;
        int height = 200;
        Format format = Format.JPG;
        instance = new ImageInfo(width, height, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(format, instance.getSourceFormat());
    }

    @Test
    public void testConstructor6() {
        Dimension size = new Dimension(500, 200);
        Dimension tileSize = new Dimension(300, 100);
        instance = new ImageInfo(size, tileSize);
        assertEquals(1, instance.getImages().size());
        assertEquals(size, instance.getImages().get(0).getSize());
        assertEquals(tileSize, instance.getImages().get(0).getTileSize());
    }

    @Test
    public void testConstructor7() {
        int width = 500;
        int height = 200;
        int tileWidth = 200;
        int tileHeight = 100;
        instance = new ImageInfo(width, height, tileWidth, tileHeight);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(tileWidth, instance.getImages().get(0).getTileSize().width);
        assertEquals(tileHeight, instance.getImages().get(0).getTileSize().height);
    }

    @Test
    public void testConstructor8() {
        int width = 500;
        int height = 200;
        int tileWidth = 200;
        int tileHeight = 100;
        Format format = Format.GIF;
        instance = new ImageInfo(width, height, tileWidth, tileHeight, format);
        assertEquals(1, instance.getImages().size());
        assertEquals(width, instance.getImages().get(0).getSize().width);
        assertEquals(height, instance.getImages().get(0).getSize().height);
        assertEquals(tileWidth, instance.getImages().get(0).getTileSize().width);
        assertEquals(tileHeight, instance.getImages().get(0).getTileSize().height);
        assertEquals(format, instance.getSourceFormat());
    }

    @Test
    public void testFromJsonWithFile() throws Exception {
        // TODO: write this
    }

    @Test
    public void testFromJsonWithInputStream() throws Exception {
        String json = instance.toJson();
        InputStream inputStream = new ByteArrayInputStream(json.getBytes());

        ImageInfo info = ImageInfo.fromJson(inputStream);
        assertEquals(info.toString(), instance.toString());
    }

    @Test
    public void testFromJsonWithString() throws Exception {
        String json = instance.toJson();
        ImageInfo info = ImageInfo.fromJson(json);
        assertEquals(info.toString(), instance.toString());
    }

    @Test
    public void testEquals() {
        // equal
        ImageInfo info1 = new ImageInfo(100, 80, 50, 40, Format.JPG);
        ImageInfo info2 = new ImageInfo(100, 80, 50, 40, Format.JPG);
        assertTrue(info1.equals(info2));
        // not equal
        info2 = new ImageInfo(99, 80, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 79, 50, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 49, 40, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 50, 39, Format.JPG);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, 50, 40, Format.TIF);
        assertFalse(info1.equals(info2));
        info2 = new ImageInfo(100, 80, Format.JPG);
        assertFalse(info1.equals(info2));
    }

    @Test
    public void testGetImages() {
        assertEquals(1, instance.getImages().size());
        assertEquals(0, new ImageInfo().getImages().size());
    }

    @Test
    public void testGetOrientation() {
        assertEquals(Orientation.ROTATE_270, instance.getOrientation());
    }

    @Test
    public void testGetOrientationSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), instance.getOrientationSize());
    }

    @Test
    public void testGetSize() {
        assertEquals(new Dimension(100, 80), instance.getSize());
    }

    @Test
    public void testGetSizeWithIndex() {
        ImageInfo.Image image = new ImageInfo.Image();
        image.width = 50;
        image.height = 40;
        instance.getImages().add(image);

        image = new ImageInfo.Image();
        image.width = 25;
        image.height = 20;
        instance.getImages().add(image);

        assertEquals(new Dimension(25, 20), instance.getSize(2));
    }

    @Test
    public void testGetSourceFormat() {
        assertEquals(Format.JPG, instance.getSourceFormat());

        instance.setSourceFormat(null);
        assertEquals(Format.UNKNOWN, instance.getSourceFormat());
    }

    @Test
    public void testToJson() {
        // tested in testFromJson()
    }

    @Test
    public void testToString() throws Exception {
        assertEquals(instance.toJson(), instance.toString());
    }

    @Test
    public void testWriteAsJson() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        instance.writeAsJson(baos);
        assertTrue(Arrays.equals(baos.toByteArray(),
                instance.toJson().getBytes()));
    }

    /********************* ImageInfo.Image tests *************************/

    @Test
    public void testImageConstructor1() {
        ImageInfo.Image image = new ImageInfo.Image();
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(new Dimension(0, 0), image.getSize());
        assertEquals(new Dimension(0, 0), image.getTileSize());
    }

    @Test
    public void testImageConstructor2() {
        Dimension size = new Dimension(300, 200);
        ImageInfo.Image image = new ImageInfo.Image(size);
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(size, image.getSize());
        assertEquals(size, image.getTileSize());
    }

    @Test
    public void testImageConstructor3() {
        Dimension size = new Dimension(300, 200);
        Orientation orientation = Orientation.ROTATE_90;
        ImageInfo.Image image = new ImageInfo.Image(size, orientation);
        assertEquals(orientation, image.getOrientation());
        assertEquals(size, image.getSize());
        assertEquals(size, image.getTileSize());
    }

    @Test
    public void testImageConstructor4() {
        int width = 300;
        int height = 200;
        ImageInfo.Image image = new ImageInfo.Image(width, height);
        assertEquals(Orientation.ROTATE_0, image.getOrientation());
        assertEquals(width, image.getSize().width);
        assertEquals(height, image.getSize().height);
        assertEquals(new Dimension(width, height), image.getTileSize());
    }

    @Test
    public void testImageConstructor5() {
        int width = 300;
        int height = 200;
        Orientation orientation = Orientation.ROTATE_90;
        ImageInfo.Image image = new ImageInfo.Image(width, height, orientation);
        assertEquals(orientation, image.getOrientation());
        assertEquals(width, image.getSize().width);
        assertEquals(height, image.getSize().height);
        assertEquals(new Dimension(width, height), image.getTileSize());
    }

    @Test
    public void testImageGetOrientationSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationSize());
    }

    @Test
    public void testImageGetOrientationTileSize() {
        ImageInfo.Image image = instance.getImages().get(0);
        image.setOrientation(Orientation.ROTATE_90);
        assertEquals(new Dimension(80, 100), image.getOrientationTileSize());
    }

}
