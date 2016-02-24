package edu.illinois.library.cantaloupe.resource.iiif.v1;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ImageInfoTest {

    private ImageInfo instance;

    @Before
    public void setUp() {
        instance = new ImageInfo();
    }

    /**
     * Tests the functionality of toJson(), not the JSON structure, which will
     * be tested instead in ImageInfoFactoryTest.
     *
     * @throws Exception
     */
    @Test
    public void testToJson() throws Exception {
        assertTrue(instance.toJson().contains("\"@context\":"));
    }

}
