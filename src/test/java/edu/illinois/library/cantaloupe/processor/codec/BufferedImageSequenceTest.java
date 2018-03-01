package edu.illinois.library.cantaloupe.processor.codec;

import org.junit.Before;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.util.Iterator;

import static org.junit.Assert.*;

public class BufferedImageSequenceTest {

    private BufferedImageSequence instance;

    @Before
    public void setUp() throws Exception {
        instance = new BufferedImageSequence();
    }

    private BufferedImage mockImage() {
        return new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    }

    /* add() */

    @Test
    public void testAdd() {
        assertEquals(0, instance.length());
        instance.add(mockImage());
        assertEquals(1, instance.length());
    }

    /* get() */

    @Test
    public void testGetWithExistingIndex() {
        BufferedImage image = mockImage();
        instance.add(image);
        assertSame(image, instance.get(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testGetWithNonexistingIndex() {
        instance.get(0);
    }

    /* iterator() */

    @Test
    public void testIterator() {
        assertFalse(instance.iterator().hasNext());

        BufferedImage image = mockImage();
        instance.add(image);
        Iterator<BufferedImage> it = instance.iterator();
        assertTrue(it.hasNext());
        assertSame(image, it.next());
    }

    /* length() */

    @Test
    public void testLength() {
        assertEquals(0, instance.length());
        instance.add(mockImage());
        assertEquals(1, instance.length());
    }

    /* set() */

    @Test
    public void testSet() {
        BufferedImage image1 = mockImage();
        BufferedImage image2 = mockImage();
        instance.add(image1);
        assertSame(image1, instance.get(0));

        instance.set(0, image2);
        assertSame(image2, instance.get(0));
    }

}