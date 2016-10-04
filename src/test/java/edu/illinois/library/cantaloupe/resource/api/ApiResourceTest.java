package edu.illinois.library.cantaloupe.resource.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.StandaloneEntry;
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

    private static final String identifier = "jpg";
    private static final String username = "admin";
    private static final String secret = "secret";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        StandaloneEntry.getWebServer().stop();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, true);
        config.setProperty(WebApplication.API_USERNAME_CONFIG_KEY, username);
        config.setProperty(WebApplication.API_SECRET_CONFIG_KEY, secret);

        StandaloneEntry.getWebServer().start();
    }

    /* doPurge() */

    @Test
    public void testDoPurgeWithEndpointDisabled() {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ApiResource.ENABLED_CONFIG_KEY, false);

        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithNoCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        try {
            client.delete();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }
    }

    @Test
    public void testDoPurgeWithInvalidCredentials() throws Exception {
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
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
        ClientResource client = getClientForUriPath(WebApplication.API_PATH + "/" + identifier);
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
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
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
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
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.put(entity, MediaType.APPLICATION_JSON);

        assertEquals("cats", ConfigurationFactory.getInstance().getString("test"));
    }

}
