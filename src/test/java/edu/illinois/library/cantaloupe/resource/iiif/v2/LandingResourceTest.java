package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.configuration.Configuration;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    public void testEndpointDisabled() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath(
                WebApplication.IIIF_2_PATH);

        config.setProperty("endpoint.iiif.2.enabled", true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("endpoint.iiif.2.enabled", false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    public void testGet() throws IOException {
        ClientResource client = getClientForUriPath(
                WebApplication.IIIF_2_PATH);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("Cantaloupe Image"));
    }

}
