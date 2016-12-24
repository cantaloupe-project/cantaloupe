package edu.illinois.library.cantaloupe.resource;

import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

import static org.junit.Assert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Test
    public void testRootUri() throws Exception {
        ClientResource client = getClientForUriPath("");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("<body"));
    }

}
