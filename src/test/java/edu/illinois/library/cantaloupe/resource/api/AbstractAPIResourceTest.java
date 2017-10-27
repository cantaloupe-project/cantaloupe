package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import static org.junit.Assert.*;

abstract class AbstractAPIResourceTest extends ResourceTest {

    static final String USERNAME = "admin";
    static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);
        config.setProperty(Key.API_USERNAME, USERNAME);
        config.setProperty(Key.API_SECRET, SECRET);
    }

    abstract String getURIPath();

    @Test
    public void testEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);
        ClientResource client = getClientForUriPath(
                getURIPath(), USERNAME, SECRET);
        try {
            client.options();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testNoCredentials() throws Exception {
        ClientResource client = getClientForUriPath(getURIPath());
        try {
            client.options();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testInvalidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(
                getURIPath(), "invalid", "invalid");
        try {
            client.options();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

}
