package edu.illinois.library.cantaloupe.resource.iiif.v2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class OutputFormatTest {

    @Test
    void testToString() {
        for (OutputFormat format : OutputFormat.values()) {
            assertEquals(format.toFormat().getPreferredExtension(),
                    format.toString());
        }
    }

}
