package edu.illinois.library.cantaloupe.image;

import junit.framework.TestCase;

public class QualityTest extends TestCase {

    public void testValues() {
        assertNotNull(Quality.valueOf("BITONAL"));
        assertNotNull(Quality.valueOf("COLOR"));
        assertNotNull(Quality.valueOf("DEFAULT"));
        assertNotNull(Quality.valueOf("GRAY"));
    }

}
