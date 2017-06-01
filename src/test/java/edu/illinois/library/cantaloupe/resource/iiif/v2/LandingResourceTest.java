package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Test
    public void testEndpointDisabled() {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                RestletApplication.IIIF_2_PATH);

        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testGet() throws IOException {
        ClientResource client = getClientForUriPath(RestletApplication.IIIF_2_PATH);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("Cantaloupe Image"));
    }

}
