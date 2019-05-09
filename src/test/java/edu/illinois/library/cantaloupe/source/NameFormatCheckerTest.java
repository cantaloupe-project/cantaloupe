package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameFormatCheckerTest {

    @Test
    void testCheckWithKnownFormat() {
        NameFormatChecker instance = new NameFormatChecker("cats.jpg");
        assertEquals(Format.JPG, instance.check());
    }

    @Test
    void testCheckWithUnknownFormat() {
        NameFormatChecker instance = new NameFormatChecker("cats");
        assertEquals(Format.UNKNOWN, instance.check());
    }

}
