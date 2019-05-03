package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SourceFormatExceptionTest {

    @Test
    void testConstructor4WithKnownFormat() {
        Exception e = new SourceFormatException(
                new MockStreamProcessor(), Format.JPG);
        assertEquals(MockStreamProcessor.class.getSimpleName() + " does not support the JPEG source format",
                e.getMessage());
    }

    @Test
    void testConstructor4WithUnknownFormat() {
        Exception e = new SourceFormatException(
                new MockStreamProcessor(), Format.UNKNOWN);
        assertEquals("Unknown source format", e.getMessage());
    }

}
