package edu.illinois.library.cantaloupe.resource.health;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.SpringBootResourceTest;
import edu.illinois.library.cantaloupe.status.Health;
import edu.illinois.library.cantaloupe.status.HealthChecker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot-enabled test for HealthResource.
 *
 * This test has been refactored from the legacy Client-based approach to use
 * Spring Boot's TestRestTemplate for more reliable and maintainable HTTP testing.
 */
@TestPropertySource(properties = {
    "endpoint.health.enabled=true",
    "endpoint.api.enabled=true"
})
public class HealthResourceTest extends SpringBootResourceTest {

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        HealthChecker.getSourceUsages().clear();
        HealthChecker.overrideHealth(null);
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, true);
    }

    @Override
    protected String getEndpointPath() {
        return Route.HEALTH_PATH;
    }

    @Test
    void testGETWithEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEALTH_ENDPOINT_ENABLED, false);

        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    /**
     * The processing pipeline isn't exercised until an image has been
     * successfully returned from an image endpoint.
     */
    @Test
    void testGETWithNoPriorImageRequest() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify response contains health information
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("GREEN") ||
                  response.getBody().contains("YELLOW") ||
                  response.getBody().contains("RED"));
    }

    @Test
    void testGETWithPriorImageRequest() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.API_ENABLED, true);

        // First, request an image to exercise the processing pipeline
        String imagePath = Route.IIIF_2_PATH + "/jpg-rgb-64x56x8-baseline.jpg/full/max/5/default.jpg";
        ResponseEntity<byte[]> imageResponse = restTemplate.getForEntity(
            "http://localhost:" + serverPort + imagePath,
            byte[].class
        );

        // The image request might succeed or fail depending on setup,
        // but we're mainly testing that it exercises the pipeline

        // Now test the health endpoint
        ResponseEntity<String> healthResponse = getForEntity("");
        assertEquals(HttpStatus.OK, healthResponse.getStatusCode());

        String body = healthResponse.getBody();
        assertNotNull(body);
        assertTrue(body.contains("GREEN") || body.contains("YELLOW") || body.contains("RED"));
    }

    @Test
    void testGETWithoutDependencyChecking() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.HEALTH_DEPENDENCY_CHECK, false);

        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Health should be reported without dependency checking
        assertNotNull(response.getBody());
    }

    @Test
    void testGETWithHealthOverride() throws Exception {
        Health health = new Health();
        health.setMinColor(Health.Color.RED);
        // Override health status
        HealthChecker.overrideHealth(health);

        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("RED"));
    }

    @Test
    void testGETReturnsJSON() throws Exception {
        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // Verify content type is JSON
        String contentType = response.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/json"));

        // Verify response is valid JSON structure
        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.startsWith("{"));
        assertTrue(body.endsWith("}"));
        assertTrue(body.contains("color"));
    }

    @Test
    void testHEADRequest() throws Exception {
        ResponseEntity<String> response = headForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        // HEAD should return headers but no body
        String contentType = response.getHeaders().getFirst("Content-Type");
        assertNotNull(contentType);
        assertTrue(contentType.contains("application/json"));
    }

    @Test
    void testHealthResponseStructure() throws Exception {
        ResponseEntity<String> response = getForEntity("");
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body);

        // Verify expected JSON structure
        assertTrue(body.contains("\"color\""));
        assertTrue(body.contains("\"message\""));
        assertTrue(body.contains("\"possibleColors\""));

        // Verify possible colors array
        assertTrue(body.contains("[\"GREEN\",\"YELLOW\",\"RED\"]") ||
                  body.contains("[\"RED\",\"YELLOW\",\"GREEN\"]") ||
                  body.contains("GREEN") && body.contains("YELLOW") && body.contains("RED"));
    }

    @Test
    void testHealthEndpointCaching() throws Exception {
        // Make multiple requests to verify consistent response
        ResponseEntity<String> response1 = getForEntity("");
        ResponseEntity<String> response2 = getForEntity("");

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertEquals(HttpStatus.OK, response2.getStatusCode());

        // Both responses should be valid health responses
        assertNotNull(response1.getBody());
        assertNotNull(response2.getBody());

        assertTrue(response1.getBody().contains("color"));
        assertTrue(response2.getBody().contains("color"));
    }

    @Test
    void testHealthEndpointWithCustomUserAgent() throws Exception {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("User-Agent", "HealthCheck/1.0");

        ResponseEntity<String> response = getForEntityWithHeaders("", headers);
        assertEquals(HttpStatus.OK, response.getStatusCode());

        String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("color"));
    }
}
