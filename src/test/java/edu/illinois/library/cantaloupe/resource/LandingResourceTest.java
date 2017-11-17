package edu.illinois.library.cantaloupe.resource;

import org.junit.Test;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Override
    protected String getEndpointPath() {
        return "";
    }

    @Test
    public void testRootURIStatus() throws Exception {
        assertStatus(200, getHTTPURI(""));
    }

    @Test
    public void testRootURIRepresentation() throws Exception {
        assertRepresentationContains("<body", getHTTPURI(""));
    }

}
