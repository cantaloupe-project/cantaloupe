package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import org.junit.Test;

import static org.junit.Assert.*;

public class UnsupportedSourceFormatExceptionTest {

    @Test
    public void testConstructor4WithKnownFormat() {
        Exception e = new UnsupportedSourceFormatException(
                new MockStreamProcessor(), Format.JPG);
        assertEquals(MockStreamProcessor.class.getSimpleName() + " does not support the JPEG source format",
                e.getMessage());
    }

    @Test
    public void testConstructor4WithUnknownFormat() {
        Exception e = new UnsupportedSourceFormatException(
                new MockStreamProcessor(), Format.UNKNOWN);
        assertEquals(MockStreamProcessor.class.getSimpleName() + " only supports known source formats",
                e.getMessage());
    }

}
