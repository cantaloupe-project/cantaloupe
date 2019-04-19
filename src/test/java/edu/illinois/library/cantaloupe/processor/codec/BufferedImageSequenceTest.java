package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

public class BufferedImageSequenceTest extends BaseTest {

    private BufferedImageSequence instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new BufferedImageSequence();
    }

    private BufferedImage mockImage() {
        return new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
    }

    /* add() */

    @Test
    void testAdd() {
        assertEquals(0, instance.length());
        instance.add(mockImage());
        assertEquals(1, instance.length());
    }

    /* get() */

    @Test
    void testGetWithExistingIndex() {
        BufferedImage image = mockImage();
        instance.add(image);
        assertSame(image, instance.get(0));
    }

    @Test
    void testGetWithNonexistingIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> instance.get(0));
    }

    /* iterator() */

    @Test
    void testIterator() {
        assertFalse(instance.iterator().hasNext());

        BufferedImage image = mockImage();
        instance.add(image);
        Iterator<BufferedImage> it = instance.iterator();
        assertTrue(it.hasNext());
        assertSame(image, it.next());
    }

    /* length() */

    @Test
    void testLength() {
        assertEquals(0, instance.length());
        instance.add(mockImage());
        assertEquals(1, instance.length());
    }

    /* set() */

    @Test
    void testSet() {
        BufferedImage image1 = mockImage();
        BufferedImage image2 = mockImage();
        instance.add(image1);
        assertSame(image1, instance.get(0));

        instance.set(0, image2);
        assertSame(image2, instance.get(0));
    }

}