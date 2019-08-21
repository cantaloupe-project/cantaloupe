package edu.illinois.library.cantaloupe.resource.api;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class HealthResourceTest extends AbstractAPIResourceTest {

    @Override
    protected String getEndpointPath() {
        return Route.HEALTH_PATH;
    }

    @Test
    public void testGETWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, false);
        try {
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

    /**
     * The processing pipeline isn't exercised until an image has been
     * successfully returned from an image endpoint.
     */
    @Test
    public void testGETWithNoPriorImageRequest() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testGETWithPriorImageRequest() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        // Request an image
        Client imageClient = null;
        try {
            URI uri = new URI("http://localhost:" + appServer.getHTTPPort() +
                    Route.IIIF_2_PATH +
                    "/jpg-rgb-64x56x8-baseline.jpg/full/max/5/default.jpg");
            imageClient = new Client().builder().uri(uri).build();
            imageClient.send();
        } finally {
            if (imageClient != null) {
                imageClient.stop();
            }
        }
        Response response = client.send();
        assertEquals(200, response.getStatus());
        assertTrue(response.getBodyAsString().contains("\"color\":\"GREEN\""));
    }

    @Test
    public void testGETWithYellowStatus() throws Exception {
        Health health = new Health();
        health.setMinColor(Health.Color.YELLOW);
        try {
            HealthChecker.setOverriddenHealth(health);

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.API_ENABLED, true);

            client.send();
            fail("Expected HTTP 500");
        } catch (ResourceException e) {
            assertEquals(500, e.getStatusCode());
        } finally {
            HealthChecker.setOverriddenHealth(null);
        }
    }

    @Test
    public void testGETWithRedStatus() throws Exception {
        Health health = new Health();
        health.setMinColor(Health.Color.RED);
        try {
            HealthChecker.setOverriddenHealth(health);

            Configuration config = Configuration.getInstance();
            config.setProperty(Key.API_ENABLED, true);

            client.send();
            fail("Expected HTTP 500");
        } catch (ResourceException e) {
            assertEquals(500, e.getStatusCode());
        } finally {
            HealthChecker.setOverriddenHealth(null);
        }
    }

    @Override // because this endpoint doesn't require auth
    @Test
    public void testGETWithNoCredentials() throws Exception {
        client.setUsername(null);
        client.setSecret(null);
        client.send();
    }

    @Override // because this endpoint doesn't require auth
    @Test
    public void testGETWithInvalidCredentials() throws Exception {
        client.setUsername("invalid");
        client.setSecret("invalid");
        client.send();
    }

    @Test
    public void testGETResponseBody() throws Exception {
        Response response = client.send();
        assertTrue(response.getBodyAsString().contains("\"color\":"));
    }

    @Test
    public void testGETResponseHeaders() throws Exception {
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(6, headers.size());

        // Cache-Control
        assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

}
