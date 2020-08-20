package edu.illinois.library.cantaloupe.source;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NameFormatCheckerTest extends BaseTest {

    @Test
    void testCheckWithKnownFormat() {
        NameFormatChecker instance = new NameFormatChecker("cats.jpg");
        assertEquals(Format.get("jpg"), instance.check());
    }

    @Test
    void testCheckWithUnknownFormat() {
        NameFormatChecker instance = new NameFormatChecker("cats");
        assertEquals(Format.UNKNOWN, instance.check());
    }

}
