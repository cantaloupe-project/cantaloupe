package edu.illinois.library.cantaloupe.resource.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
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

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(WebApplication.ADMIN_SECRET_CONFIG_KEY, SECRET);
    }

    @Test
    public void testCacheHeaders() {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, "1234");
        config.setProperty(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, "4567");
        config.setProperty(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, "false");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("no-cache", null);

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH,
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
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
        client.get(MediaType.TEXT_HTML);
        assertEquals(Status.SUCCESS_OK, client.getStatus());
        assertTrue(client.getResponse().getEntityAsText().
                contains("Cantaloupe Image Server"));
    }

    @Test
    public void testDoGetAsJson() {
        ConfigurationFactory.getInstance().setProperty("test", "cats");

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH,
                USERNAME, SECRET);

        client.get(MediaType.APPLICATION_JSON);
        assertTrue(client.getResponse().getEntityAsText().
                contains("\"test\":\"cats\""));
    }

    @Test
    public void testDoPost() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entityStr = new ObjectMapper().writer().writeValueAsString(entityMap);
        Representation rep = new StringRepresentation(entityStr,
                MediaType.APPLICATION_JSON);

        ClientResource client = getClientForUriPath(
                WebApplication.ADMIN_PATH, USERNAME, SECRET);
        client.post(rep);

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

        ClientResource client = getClientForUriPath(WebApplication.ADMIN_PATH,
                USERNAME, SECRET);
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
