package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Quality;

public class QualityTest extends CantaloupeTestCase {

    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
    }

}
