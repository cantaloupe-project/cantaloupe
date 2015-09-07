package edu.illinois.library.cantaloupe.resource;

import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    public void testUnavailableSourceFormat() throws IOException {
        ClientResource client = getClientForUriPath("/text.txt/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

}
