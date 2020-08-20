package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutputFormatTest extends BaseTest {

    @Test
    void testToFormat() {
        assertEquals(Format.get("jpg"), OutputFormat.JPG.toFormat());
    }

    @Test
    void testToString() {
        for (OutputFormat format : OutputFormat.values()) {
            assertEquals(format.toFormat().getPreferredExtension(),
                    format.toString());
        }
    }

}
