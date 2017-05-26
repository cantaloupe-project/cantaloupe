package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;

/**
 * Functional test of CacheResource.
 */
public class CacheResourceTest extends APIResourceTest {

    private static final String IDENTIFIER = "jpg";

    /* doPurge() */

    @Test
    public void testDoPurgeWithEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);
        ClientResource client = getClientForUriPath(
                WebApplication.CACHE_PATH + "/" + IDENTIFIER, USERNAME, SECRET);
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithNoCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.CACHE_PATH + "/" + IDENTIFIER);
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithInvalidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(
                WebApplication.CACHE_PATH + "/" + IDENTIFIER, "invalid", "invalid");
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithValidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(
                WebApplication.CACHE_PATH + "/" + IDENTIFIER, USERNAME, SECRET);
        client.delete();
        assertEquals(Status.SUCCESS_NO_CONTENT, client.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

}
