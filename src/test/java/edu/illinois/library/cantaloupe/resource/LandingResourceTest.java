package edu.illinois.library.cantaloupe.resource;

import org.junit.Test;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    private String getURI() {
        return "http://localhost:" + PORT;
    }

    @Test
    public void rootURIStatus() throws Exception {
        assertStatus(200, getURI());
    }

    @Test
    public void rootURIRepresentation() throws Exception {
        assertRepresentationContains("<body", getURI());
    }

}
