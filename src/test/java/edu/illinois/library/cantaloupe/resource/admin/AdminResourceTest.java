package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    }

    @Test
    public void testCacheHeaders() {
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

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("no-cache", null);

        ClientResource client = getClientForUriPath(RestletApplication.ADMIN_PATH,
                USERNAME, SECRET);
        client.get();
        List<CacheDirective> actualDirectives = client.getResponse().getCacheDirectives();
        for (CacheDirective d : actualDirectives) {
            if (d.getName() != null) {
                assertTrue(expectedDirectives.keySet().contains(d.getName()));
                if (d.getValue() != null) {
                    assertTrue(expectedDirectives.get(d.getName()).equals(d.getValue()));
                } else {
                    assertNull(expectedDirectives.get(d.getName()));
                }
            }
        }
    }

    @Test
    public void testDoGet() throws Exception {
        // no credentials
        ClientResource client = getClientForUriPath(RestletApplication.ADMIN_PATH);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }

        // invalid credentials
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "invalid", "invalid"));
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }

        // valid credentials
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
        client.get(MediaType.TEXT_HTML);
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.getResponse().getEntityAsText().
                contains("Cantaloupe Image Server"));
    }

    @Test
    public void testEnabled() {
        Configuration config = Configuration.getInstance();
        // enabled
        config.setProperty(Key.ADMIN_ENABLED, true);

        ClientResource client = getClientForUriPath(
                RestletApplication.ADMIN_PATH, USERNAME, SECRET);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        // disabled
        config.setProperty(Key.ADMIN_ENABLED, false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

}
