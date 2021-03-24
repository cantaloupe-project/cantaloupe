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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.*;

public class HealthResourceTest extends AbstractAPIResourceTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        HealthChecker.getSourceUsages().clear();
        HealthChecker.overrideHealth(null);
    }

    @Override
    protected String getEndpointPath() {
        return Route.HEALTH_PATH;
    }

    @Test
    void testGETWithEndpointDisabled() throws Exception {
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
    void testGETWithNoPriorImageRequest() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        Response response = client.send();
        assertEquals(200, response.getStatus());
    }

    @Test
    void testGETWithPriorImageRequest() throws Exception {
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
    void testGETWithYellowStatus() throws Exception {
        var config = Configuration.getInstance();
        config.setProperty(Key.HEALTH_DEPENDENCY_CHECK, true);
        config.setProperty(Key.API_ENABLED, true);

        Health health = new Health();
        health.setMinColor(Health.Color.YELLOW);
        try {
            HealthChecker.overrideHealth(health);

            client.send();
            fail("Expected HTTP 500");
        } catch (ResourceException e) {
            assertEquals(500, e.getStatusCode());
        }
    }

    @Test
    void testGETWithRedStatus() throws Exception {
        var config = Configuration.getInstance();
        config.setProperty(Key.HEALTH_DEPENDENCY_CHECK, true);
        config.setProperty(Key.API_ENABLED, true);

        Health health = new Health();
        health.setMinColor(Health.Color.RED);
        try {
            HealthChecker.overrideHealth(health);

            client.send();
            fail("Expected HTTP 500");
        } catch (ResourceException e) {
            assertEquals(500, e.getStatusCode());
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
    void testGETResponseBody() throws Exception {
        Response response = client.send();
        assertTrue(response.getBodyAsString().contains("\"color\":"));
    }

    @Test
    void testGETResponseHeaders() throws Exception {
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(6, headers.size());

        // Cache-Control
        assertEquals("no-cache", headers.getFirstValue("Cache-Control"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertTrue("application/json;charset=UTF-8".equalsIgnoreCase(
                headers.getFirstValue("Content-Type")));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

}
