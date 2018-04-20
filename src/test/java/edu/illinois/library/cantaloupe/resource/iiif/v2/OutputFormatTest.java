package edu.illinois.library.cantaloupe.resource.iiif.v2;

import org.junit.Test;

import static org.junit.Assert.*;

public class OutputFormatTest {

    @Test
    public void testToString() {
        for (OutputFormat format : OutputFormat.values()) {
            assertEquals(format.toFormat().getPreferredExtension(),
                    format.toString());
        }
    }

}
