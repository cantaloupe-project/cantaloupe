package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
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

    private static final String username = "admin";
    private static final String secret = "secret";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        StandaloneEntry.getWebServer().stop();

        Configuration config = ConfigurationFactory.getInstance();
        resetConfiguration();
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, secret);

        StandaloneEntry.getWebServer().start();
    }

    @Test
    public void testCacheHeaders() {
        Configuration config = ConfigurationFactory.getInstance();
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

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
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
    public void testDoGetAsHtml() throws Exception {
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
        client.get(MediaType.TEXT_HTML);
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        System.out.println(client.getResponse().getEntityAsText());
        assertTrue(client.getResponse().getEntityAsText().
                contains("Cantaloupe Image Server"));
    }

    @Test
    public void testDoGetAsJson() {
        ConfigurationFactory.getInstance().setProperty("test", "cats");

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));

        client.get(MediaType.APPLICATION_JSON);
        assertTrue(client.getResponse().getEntityAsText().
                contains("\"test\":\"cats\""));
    }

    @Test
    public void testDoPost() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.post(entity, MediaType.APPLICATION_JSON);

        assertEquals("cats", ConfigurationFactory.getInstance().getString("test"));
    }

    @Test
    public void testDoPostSavesFile() {
        // TODO: write this
    }

    @Test
    public void testEnabled() {
        Configuration config = ConfigurationFactory.getInstance();
        // enabled
        config.setProperty(AdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, true);

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        // disabled
        config.setProperty(AdminResource.CONTROL_PANEL_ENABLED_CONFIG_KEY, false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

}
