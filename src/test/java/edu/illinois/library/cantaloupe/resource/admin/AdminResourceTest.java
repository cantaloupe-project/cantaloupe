package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Functional test of AdminResource.
 */
public class AdminResourceTest extends ResourceTest {

    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.ADMIN_SECRET, SECRET);

        client = newClient("", USERNAME, SECRET,
                RestletApplication.ADMIN_REALM);
    }

    @Override
    protected String getEndpointPath() {
        return RestletApplication.ADMIN_PATH;
    }

    @Test
    public void testCacheHeaders() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "false");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");

        Response response = client.send();
        assertEquals("no-cache", response.getHeaders().get("Cache-Control"));
    }

    @Test
    public void testDoGet() throws Exception {
        // no credentials
        try {
            client.setUsername(null);
            client.setSecret(null);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }

        // invalid credentials
        try {
            client.setUsername("invalid");
            client.setSecret("invalid");
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(401, e.getStatusCode());
        }

        // valid credentials
        client.setUsername(USERNAME);
        client.setSecret(SECRET);
        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertTrue(response.getBodyAsString().contains("Cantaloupe Image Server"));
    }

    @Test
    public void testEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        // enabled
        config.setProperty(Key.ADMIN_ENABLED, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());

        // disabled
        config.setProperty(Key.ADMIN_ENABLED, false);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
