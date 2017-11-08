package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Functional test of LandingResource.
 */
public class LandingResourceTest extends ResourceTest {

    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                WebApplication.IIIF_1_PATH);

        config.setProperty("endpoint.iiif.1.enabled", true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("endpoint.iiif.1.enabled", false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testGet() throws Exception {
        ClientResource client = getClientForUriPath(
                WebApplication.IIIF_1_PATH);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.get().getText().contains("Cantaloupe Image"));
    }

    @Test
    public void testGetWithTrailingSlashRedirectsToWithout()
            throws IOException {
        ClientResource client = getClientForUriPath(
                WebApplication.IIIF_1_PATH + "/");
        client.setFollowingRedirects(false);
        Representation responseRep = client.get();

        assertEquals(Status.REDIRECTION_PERMANENT, client.getStatus());
        assertTrue(client.getLocationRef().toString().
                endsWith(WebApplication.IIIF_1_PATH));
        assertTrue(responseRep.isEmpty());
    }

}
