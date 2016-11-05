package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.MediaType;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of ApiResource.
 */
public class ApiResourceTest extends ResourceTest {

    private static final String IDENTIFIER = "jpg";
    private static final String USERNAME = "admin";
    private static final String SECRET = "secret";

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, true);
        config.setProperty(WebApplication.API_USERNAME_CONFIG_KEY, USERNAME);
        config.setProperty(WebApplication.API_SECRET_CONFIG_KEY, SECRET);

        webServer.start();
    }

    /* doPurge() */

    @Test
    public void testDoPurgeWithEndpointDisabled() {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, false);

        ClientResource client = getClientForUriPath(WebApplication.CACHE_PATH + "/" + IDENTIFIER);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
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
        ClientResource client = getClientForUriPath(WebApplication.CACHE_PATH + "/" + IDENTIFIER);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "invalid", "invalid"));
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithValidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.CACHE_PATH + "/" + IDENTIFIER);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
        client.delete();
        assertEquals(Status.SUCCESS_NO_CONTENT, client.getStatus());

        // TODO: assert that relevant cache files have been deleted
    }

    /* getConfiguration() */

    @Test
    public void testGetConfiguration() throws Exception {
        System.setProperty("cats", "yes");

        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/configuration");
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
        client.get();

        assertTrue(client.getResponseEntity().getText().startsWith("{"));
    }

    /* putConfiguration() */

    @Test
    public void testPutConfiguration() throws Exception {
        Map<String,Object> entityMap = new HashMap<>();
        entityMap.put("test", "cats");
        String entity = new ObjectMapper().writer().writeValueAsString(entityMap);

        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/configuration");
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, USERNAME, SECRET));
        client.put(entity, MediaType.APPLICATION_JSON);

        assertEquals("cats", ConfigurationFactory.getInstance().getString("test"));
    }

}
