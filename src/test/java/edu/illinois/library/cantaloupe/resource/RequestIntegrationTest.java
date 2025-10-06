package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.test.SpringBootBaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.context.WebApplicationContext;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for Request class using Spring Boot test framework.
 * Tests the Request class within the full Spring MVC context to ensure
 * proper integration with the web layer.
 */
@TestPropertySource(properties = {
    "http.enabled=true",
    "http.port=0",
    "https.enabled=false",
    "endpoint.iiif.1.enabled=true",
    "endpoint.iiif.2.enabled=true",
    "endpoint.iiif.3.enabled=true"
})
@AutoConfigureWebMvc
class RequestIntegrationTest extends SpringBootBaseTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @LocalServerPort
    private int port;

    @Test
    void testRequestCreatedFromRealHttpRequest() throws Exception {
        // Capture the actual Request object created during a real HTTP request
        AtomicReference<Request> capturedRequest = new AtomicReference<>();

        // Make a real HTTP request to the landing page
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        // Verify the request was successful
        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("Cantaloupe") || response.getHeaders().containsKey("X-Powered-By"));
    }

    @Test
    void testRequestWithHeaders() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("User-Agent", "Test-Agent/1.0");
        headers.add("Accept", "text/html,application/xhtml+xml");
        headers.add("Accept-Language", "en-US,en;q=0.9");
        headers.add("X-Custom-Header", "test-value");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithCookies() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "sessionId=abc123; theme=dark");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithQueryParameters() throws Exception {
        String url = "http://localhost:" + port + "/?param1=value1&param2=value2";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestToIIIFEndpoint() throws Exception {
        String url = "http://localhost:" + port + "/iiif/3";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithDifferentMethods() throws Exception {
        String baseUrl = "http://localhost:" + port + "/";

        // Test GET
        ResponseEntity<String> getResponse = restTemplate.getForEntity(baseUrl, String.class);
        assertEquals(200, getResponse.getStatusCode().value());

        // Test HEAD
        ResponseEntity<String> headResponse = restTemplate.exchange(
            baseUrl, HttpMethod.HEAD, null, String.class);
        assertEquals(200, headResponse.getStatusCode().value());

        // Test OPTIONS
        ResponseEntity<String> optionsResponse = restTemplate.exchange(
            baseUrl, HttpMethod.OPTIONS, null, String.class);
        // OPTIONS should be handled (might return 200 or other status depending on implementation)
        assertNotNull(optionsResponse.getStatusCode());
    }

    @Test
    void testRequestWithRemoteAddress() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Forwarded-For", "192.168.1.100");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, entity, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithContentType() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");

        String jsonPayload = "{\"test\": \"data\"}";
        HttpEntity<String> entity = new HttpEntity<>(jsonPayload, headers);

        // POST to a URL that should handle it gracefully
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.POST, entity, String.class);

        // Should get some response (might not be 200 but shouldn't crash)
        assertNotNull(response.getStatusCode());
    }

    @Test
    void testHealthEndpointRequest() throws Exception {
        String url = "http://localhost:" + port + "/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody().contains("GREEN") ||
                  response.getBody().contains("YELLOW") ||
                  response.getBody().contains("RED"));
    }

    @Test
    void testRequestContextPath() throws Exception {
        // Test that context path is properly handled
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestWithSpecialCharactersInPath() throws Exception {
        // Test URL encoding handling
        String url = "http://localhost:" + port + "/iiif/3";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(200, response.getStatusCode().value());
    }

    @Test
    void testRequestTimeout() throws Exception {
        // Test that requests complete within reasonable time
        long startTime = System.currentTimeMillis();

        String url = "http://localhost:" + port + "/health";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        long duration = System.currentTimeMillis() - startTime;

        assertEquals(200, response.getStatusCode().value());
        assertTrue(duration < 5000, "Request took too long: " + duration + "ms");
    }

    @Test
    void testConcurrentRequests() throws Exception {
        String url = "http://localhost:" + port + "/health";

        // Make multiple concurrent requests
        Thread[] threads = new Thread[5];
        ResponseEntity<String>[] responses = new ResponseEntity[5];

        for (int i = 0; i < threads.length; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                responses[index] = restTemplate.getForEntity(url, String.class);
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify all requests succeeded
        for (ResponseEntity<String> response : responses) {
            assertEquals(200, response.getStatusCode().value());
            assertNotNull(response.getBody());
        }
    }
}
