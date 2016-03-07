package edu.illinois.library.cantaloupe.resource.admin;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.apache.commons.configuration.Configuration;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
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

    @Test
    public void testBasicAuthentication() throws Exception {
        final String username = "admin";
        final String secret = "secret";
        Application.getWebServer().stop();
        Configuration config = Application.getConfiguration();
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, "secret");
        Application.getWebServer().start();

        // no credentials
        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH);
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
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    @Test
    public void testCacheHeaders() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.enabled", "true");
        config.setProperty("cache.client.max_age", "1234");
        config.setProperty("cache.client.shared_max_age", "4567");
        config.setProperty("cache.client.public", "false");
        config.setProperty("cache.client.private", "false");
        config.setProperty("cache.client.no_cache", "true");
        config.setProperty("cache.client.no_store", "false");
        config.setProperty("cache.client.must_revalidate", "false");
        config.setProperty("cache.client.proxy_revalidate", "false");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("no-cache", null);

        ClientResource client = getClientForUriPath("");
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

}
